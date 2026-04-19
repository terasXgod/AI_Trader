package com.terasxgod.auth_service.service

import com.terasxgod.auth_service.entity.RefreshToken
import com.terasxgod.auth_service.entity.TokenType
import com.terasxgod.auth_service.entity.User
import com.terasxgod.auth_service.repository.TokenRepository
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RefreshTokenService(
    private val tokenRepository: TokenRepository
) {

    @Value("\${jwt.refresh-expiration}")
    private val refreshExpiration: Long = 0

    fun isTokenValid(token: String): Boolean {
        return tokenRepository.findByToken(token)
            .map { !it.revoked && !it.expired && it.expireAt.isAfter(LocalDateTime.now()) }
            .orElse(false)
    }

    @Transactional
    fun saveUserRefreshToken(user: User, token: String) {
        val refreshToken = RefreshToken(
            token = token,
            tokenType = TokenType.BEARER,
            revoked = false,
            expired = false,
            user = user,
            createdAt = LocalDateTime.now(),
            expireAt = LocalDateTime.now().plusSeconds(refreshExpiration / 1000)
        )
        tokenRepository.save(refreshToken)
    }

    @Transactional
    fun revokeAllUserTokens(user: User) {
        tokenRepository.revokeAllByUser(user)
    }
}