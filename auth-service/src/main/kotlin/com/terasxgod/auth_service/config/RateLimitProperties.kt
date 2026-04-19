package com.terasxgod.auth_service.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties (
    val login: Rule = Rule(),
    val web3Login: Rule = Rule(),
    val web3Nonce: Rule = Rule()
) {
    data class Rule (
        val windowSeconds: Long = 60,
        val maxRequests: Long = 10
    )
}