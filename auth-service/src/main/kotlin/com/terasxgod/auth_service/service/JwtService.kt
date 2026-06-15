package com.terasxgod.auth_service.service

import com.terasxgod.auth_service.dto.JwtAuthResponse
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey


@Service
class JwtService (
    private val refreshTokenService: RefreshTokenService,

    @Value("\${jwt.secret}")
    private val jwtSecret: String,

    @Value("\${jwt.expiration}")
    private val expiration: Long = 0,

    @Value("\${jwt.refresh-expiration}")
    private val refreshExpiration: Long = 0
) {

    //methods to get signing key
    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(jwtSecret)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    //methods to extract info from token
    fun extractUsername(token: String): String {
        val claims: Claims = extractAllClaims(token)
        return claims.subject ?: (claims["username"] as String? ?: "")
    }

    fun extractExpiration(token: String): Date {
        val claims: Claims = extractAllClaims(token)
        return claims.expiration ?: throw IllegalStateException("Token does not contain expiration")
    }

    private fun extractAllClaims(token: String): Claims {
        val claims: Claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
        return claims
    }

    //methods to validate token
    fun isAccessTokenValid(token: String, userDetails: UserDetails): Boolean {
        val userName: String = extractUsername(token)
        return (userName == userDetails.username) && !isTokenExpired(token)
    }

    fun isRefreshTokenValid(token: String, userDetails: UserDetails): Boolean {
        return isAccessTokenValid(token, userDetails) && refreshTokenService.isTokenValid(token) //пока что там затычка
    }

    private fun isTokenExpired(token: String): Boolean {
        val exp: Date = extractExpiration(token)
        return exp.before(Date())
    }

    //methods to generate tokens
    fun getJwtAuthResponse(userDetails: UserDetails): JwtAuthResponse {
        val accessToken: String = generateAccessToken(userDetails.username)
        val refreshToken: String = generateRefreshToken(userDetails.username)
        return JwtAuthResponse(accessToken, refreshToken)
    }

    private fun generateToken(userName: String, expiration: Long): String {
        return Jwts.builder()
            .subject(userName)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(Date(System.currentTimeMillis() + expiration))
            .claim("username", userName)
            .signWith(getSigningKey())
            .compact()
    }

    fun generateAccessToken(userName: String): String {
        return generateToken(userName, expiration)
    }

    fun generateRefreshToken(userName: String): String {
        val token: String = generateToken(userName, refreshExpiration)
        return token
    }

}