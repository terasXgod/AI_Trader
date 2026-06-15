package com.terasxgod.auth_service.repository

import com.terasxgod.auth_service.entity.RefreshToken
import com.terasxgod.auth_service.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface TokenRepository: JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>

    @Modifying
    @Query("update RefreshToken t set t.revoked = true, t.expired = true where t.user = :user and t.revoked = false")
    fun revokeAllByUser(@Param("user") user: User): Int
}