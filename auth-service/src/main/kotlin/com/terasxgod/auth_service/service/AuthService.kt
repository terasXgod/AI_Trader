package com.terasxgod.auth_service.service

import com.terasxgod.auth_service.entity.User
import com.terasxgod.auth_service.entity.Web3
import com.terasxgod.auth_service.repository.UserRepository
import com.terasxgod.auth_service.repository.Web3Repository
import com.yourproject.auth.dto.AuthForgotPasswordPost200Response
import com.yourproject.auth.dto.AuthForgotPasswordPostRequest
import com.yourproject.auth.dto.AuthLogoutPost200Response
import com.yourproject.auth.dto.AuthLogoutPostRequest
import com.yourproject.auth.dto.AuthRefreshPostRequest
import com.yourproject.auth.dto.JwtAuthResponse
import com.yourproject.auth.dto.UserAuth
import com.yourproject.auth.dto.Web3AuthRequest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val userRepository: UserRepository,
    private val web3Repository: Web3Repository,
    private val nonceService: NonceService,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager
){
    fun forgotPassword(authRequest: AuthForgotPasswordPostRequest): AuthForgotPasswordPost200Response {
        return AuthForgotPasswordPost200Response(
            message = "Password reset email sent successfully to ${authRequest.email}."
            //нужно добавить чтобы отправлялось письмо с инструкциями по сбросу пароля, но это уже зависит от конкретной реализации почтового сервиса и не входит в базовую логику аутентификации
        )
    }

    fun register(userAuth: UserAuth): JwtAuthResponse {
        val user = userRepository.save(
            User(
                email = userAuth.email,
                passwordValue = passwordEncoder.encode(userAuth.password)
                    ?: throw IllegalStateException("Password encoding failed")
            )
        )

        val response = jwtService.getJwtAuthResponse(user)
        refreshTokenService.saveUserRefreshToken(user, response.refreshToken)
        return response
    }

    fun login(userAuth: UserAuth): JwtAuthResponse {
        val authRequest: Authentication = UsernamePasswordAuthenticationToken(userAuth.email, userAuth.password)
        val authResult: Authentication

        try {
            authResult = authenticationManager.authenticate(authRequest)
        } catch (e: BadCredentialsException) {
            throw IllegalArgumentException("Invalid username or password")
        }

        val userDetails = authResult.principal as UserDetails
        val dto = jwtService.getJwtAuthResponse(userDetails)

        val user: User = userRepository.findByEmail(userAuth.email)
            .orElseThrow { IllegalArgumentException("User not found") }

        refreshTokenService.revokeAllUserTokens(user)
        refreshTokenService.saveUserRefreshToken(user, dto.refreshToken)
        return dto
    }

    fun refresh(authRefreshPostRequest: AuthRefreshPostRequest): JwtAuthResponse {
        val refreshToken = authRefreshPostRequest.refreshToken
        val username = jwtService.extractUsername(refreshToken)
        val user = userRepository.findByEmail(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
            throw IllegalArgumentException("Invalid refresh token")
        }

        refreshTokenService.revokeAllUserTokens(user)
        val response = jwtService.getJwtAuthResponse(user)
        refreshTokenService.saveUserRefreshToken(user, response.refreshToken)
        return response
    }

    fun logout(authLogoutPostRequest: AuthLogoutPostRequest): AuthLogoutPost200Response {
        val token = authLogoutPostRequest.token
        val username = jwtService.extractUsername(token)
        val user = userRepository.findByEmail(username)
            .orElseThrow { IllegalArgumentException("User not found") }

        refreshTokenService.revokeAllUserTokens(user)
        return AuthLogoutPost200Response(message = "User logged out successfully.")
    }

    /**
     * Привязывает Web3 кошельки к существующему пользователю
     */
    fun web3BindWallet(web3AuthRequest: Web3AuthRequest): JwtAuthResponse {
        // Верифицируем nonce и сигнатуру
        val isValid = nonceService.getAndVerifyNonce(
            web3AuthRequest.walletAddress,
            web3AuthRequest.signature
        )

        if (!isValid) {
            throw IllegalArgumentException("Invalid wallet signature or nonce expired")
        }

        // Проверяем, что кошельки не привязан к другому пользователю
        val existingWallet = web3Repository.findByWalletAddress(web3AuthRequest.walletAddress)
        if (existingWallet != null) {
            throw IllegalArgumentException("This wallet is already linked to another account")
        }

        // ✅ Получаем текущего пользователя из SecurityContext
        val currentUser = getCurrentUser()
            ?: throw IllegalArgumentException("User must be authenticated to bind a wallet")

        // Создаем запись Web3 и связываем с пользователем
        val web3 = Web3(
            walletAddress = web3AuthRequest.walletAddress,
            user = currentUser
        )
        web3Repository.save(web3)

        // Генерируем JWT токены
        val response = jwtService.getJwtAuthResponse(currentUser)
        refreshTokenService.saveUserRefreshToken(currentUser, response.refreshToken)

        return response
    }

    /**
     * Авторизация через Web3 кошелек
     */
    fun web3Login(web3AuthRequest: Web3AuthRequest): JwtAuthResponse {
        // Верифицируем nonce и сигнатуру
        val isValid = nonceService.getAndVerifyNonce(
            web3AuthRequest.walletAddress,
            web3AuthRequest.signature
        )

        if (!isValid) {
            throw IllegalArgumentException("Invalid wallet signature or nonce expired")
        }

        // Получаем Web3 запись через web3Repository
        val web3 = web3Repository.findByWalletAddress(web3AuthRequest.walletAddress)
            ?: throw IllegalArgumentException("Wallet not registered. Please bind your wallet first.")

        // Получаем пользователя из Web3 записи
        val user = web3.user

        // Отзываем старые токены
        refreshTokenService.revokeAllUserTokens(user)

        // Генерируем новые токены
        val response = jwtService.getJwtAuthResponse(user)
        refreshTokenService.saveUserRefreshToken(user, response.refreshToken)

        return response
    }

    /**
     * Получает текущего аутентифицированного пользователя из SecurityContext
     * 
     * @return User если пользователь аутентифицирован, иначе null
     */
    private fun getCurrentUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        
        // Проверяем что пользователь аутентифицирован и авторизован
        if (authentication == null || !authentication.isAuthenticated) {
            return null
        }
        
        // Получаем username из authentication
        val username = when (val principal = authentication.principal) {
            is UserDetails -> principal.username
            is String -> principal
            else -> return null
        }
        
        // Ищем пользователя в БД по email (username)
        return userRepository.findByEmail(username).orElse(null)
    }
}