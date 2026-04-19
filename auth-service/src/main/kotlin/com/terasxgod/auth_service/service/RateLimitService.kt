package com.terasxgod.auth_service.service

import com.terasxgod.auth_service.config.RateLimitProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Instant

data class RateLimitResult(
    val allowed: Boolean,
    val retryAfterSeconds: Long
)

@Service
class RateLimitService(
    private val redis: StringRedisTemplate,
    private val props: RateLimitProperties
) {
    fun checkLogin(ip: String, email: String): RateLimitResult {
        val id = "${ip.lowercase()}:${email.trim().lowercase()}"
        return check("login", id, props.login.windowSeconds, props.login.maxRequests)
    }

    fun checkWeb3Login(ip: String, walletAddress: String): RateLimitResult {
        val id = "${ip.lowercase()}:${walletAddress.trim().lowercase()}"
        return check("web3-login", id, props.web3Login.windowSeconds, props.web3Login.maxRequests)
    }

    fun checkWeb3Nonce(ip: String): RateLimitResult {
        return check("web3-nonce", ip.lowercase(), props.web3Nonce.windowSeconds, props.web3Nonce.maxRequests)
    }

    private fun check(scope: String, identifier: String, windowSeconds: Long, maxRequests: Long): RateLimitResult {
        val now = Instant.now().epochSecond
        val windowStart = now - (now % windowSeconds)
        val key = "rl:$scope:$identifier:$windowStart"

        val count = redis.opsForValue().increment(key) ?: 0
        if (count == 1L) {
            redis.expire(key, java.time.Duration.ofSeconds(windowSeconds))
        }

        val allowed = count <= maxRequests
        val retryAfter = (windowStart + windowSeconds - now).coerceAtLeast(1)
        return RateLimitResult(allowed, retryAfter)
    }
}