package com.terasxgod.auth_service.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, nullable = false, length = 512)
    var token: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var tokenType: TokenType,

    @Column(nullable = false)
    var revoked: Boolean,

    @Column(nullable = false)
    var expired: Boolean,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var createdAt: LocalDateTime,

    @Column(nullable = false)
    var expireAt: LocalDateTime
)
