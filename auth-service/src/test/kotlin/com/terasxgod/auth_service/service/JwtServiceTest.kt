package com.terasxgod.auth_service.service

import io.mockk.every
import io.mockk.mockk
import com.terasxgod.auth_service.repository.TokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UserDetails
import java.util.Date

open class JwtServiceTest {

    private lateinit var jwtService: JwtService
    private lateinit var userDetails: UserDetails

    private val testSecret = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    private val testExpiration = 3600000L // 1 час
    private val testRefreshExpiration = 86400000L // 24 часа

    @BeforeEach
    fun setUp() {
        val tokenRepository = mockk<TokenRepository>(relaxed = true)
        jwtService = JwtService(
            refreshTokenService = RefreshTokenService(tokenRepository),
            jwtSecret = testSecret,
            expiration = testExpiration,
            refreshExpiration = testRefreshExpiration
        )

        // Создаём мок UserDetails
        userDetails = mockk {
            every { username } returns "testuser"
            every { password } returns "password"
            every { authorities } returns emptyList()
            every { isAccountNonExpired } returns true
            every { isAccountNonLocked } returns true
            every { isCredentialsNonExpired } returns true
            every { isEnabled } returns true
        }
    }

    @Test
    @DisplayName("must generate valid token")
    fun `should generate valid token`() {
        val token = jwtService.generateAccessToken(userDetails.username)

        // When
        val username = jwtService.extractUsername(token)

        // Then
        assertEquals("testuser", username)
    }

    @Test
    @DisplayName("must extract expiration date from token")
    fun `should extract expiration date from token`() {
        val before = Date()
        val token = jwtService.generateAccessToken(userDetails.username)

        // When
        val expiration = jwtService.extractExpiration(token)

        // Then
        assertNotNull(expiration)
        assertTrue(expiration.after(before))
        assertTrue(expiration.time >= before.time + testExpiration - 1000)
    }
}