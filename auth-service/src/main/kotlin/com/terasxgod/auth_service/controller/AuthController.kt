package com.terasxgod.auth_service.controller

import com.terasxgod.auth_service.service.AuthService
import com.terasxgod.auth_service.service.NonceService
import com.yourproject.auth.api.AuthApi
import com.yourproject.auth.dto.AuthForgotPasswordPost200Response
import com.yourproject.auth.dto.AuthForgotPasswordPostRequest
import com.yourproject.auth.dto.AuthLogoutPost200Response
import com.yourproject.auth.dto.AuthLogoutPostRequest
import com.yourproject.auth.dto.AuthRefreshPostRequest
import com.yourproject.auth.dto.AuthResetPasswordPost200Response
import com.yourproject.auth.dto.AuthResetPasswordPostRequest
import com.yourproject.auth.dto.AuthWeb3BindPost200Response
import com.yourproject.auth.dto.AuthWeb3NonceGet200Response
import com.yourproject.auth.dto.JwtAuthResponse
import com.yourproject.auth.dto.UserAuth
import com.yourproject.auth.dto.Web3AuthRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService,
    private val nonceService: NonceService
) : AuthApi {
    override fun authForgotPasswordPost(authForgotPasswordPostRequest: AuthForgotPasswordPostRequest): ResponseEntity<AuthForgotPasswordPost200Response> {
        return ResponseEntity(authService.forgotPassword(authForgotPasswordPostRequest), HttpStatus.OK)
    }

    override fun authLoginPasswordPost(userAuth: UserAuth): ResponseEntity<JwtAuthResponse> {
        val response = authService.login(userAuth)
        return ResponseEntity(response, HttpStatus.OK)
    }

    override fun authLogoutPost(authLogoutPostRequest: AuthLogoutPostRequest): ResponseEntity<AuthLogoutPost200Response> {
        return ResponseEntity(authService.logout(authLogoutPostRequest), HttpStatus.OK)
    }

    override fun authRefreshPost(authRefreshPostRequest: AuthRefreshPostRequest): ResponseEntity<JwtAuthResponse> {
        return ResponseEntity(authService.refresh(authRefreshPostRequest), HttpStatus.OK)
    }

    override fun authRegisterPost(userAuth: UserAuth): ResponseEntity<JwtAuthResponse> {
        return ResponseEntity(authService.register(userAuth), HttpStatus.CREATED)
    }

    override fun authResetPasswordPost(authResetPasswordPostRequest: AuthResetPasswordPostRequest): ResponseEntity<AuthResetPasswordPost200Response> {
        //челу приходит на почту ссылка, по которой он переходит и восстанавливает пароль без добавления в request дополнительного поля, пусть этим занимается фронтенд
        return super.authResetPasswordPost(authResetPasswordPostRequest)
    }


    //web3
    override fun authWeb3BindPost(web3AuthRequest: Web3AuthRequest): ResponseEntity<AuthWeb3BindPost200Response> {
        return try {
            authService.web3BindWallet(web3AuthRequest)
            val response = AuthWeb3BindPost200Response("Wallet successfully linked")
            ResponseEntity(response, HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(HttpStatus.FORBIDDEN)
        }
    }

    override fun authWeb3LoginPost(web3AuthRequest: Web3AuthRequest): ResponseEntity<JwtAuthResponse> {
        return try {
            val response = authService.web3Login(web3AuthRequest)
            ResponseEntity(response, HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    override fun authWeb3NonceGet(walletAddress: String?): ResponseEntity<AuthWeb3NonceGet200Response> {
        return try {
            val nonce: String = nonceService.generateNonce(
                walletAddress ?: throw IllegalArgumentException("Wallet address is required")
            )
            val response = AuthWeb3NonceGet200Response(nonce = nonce)
            ResponseEntity(response, HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }
}