package com.terasxgod.auth_service.repository

import com.terasxgod.auth_service.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository: JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
}