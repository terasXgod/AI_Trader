package com.terasxgod.auth_service.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.Email
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Email(message = "Email should be valid")
    @Column(name = "email", nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    private var passwordValue: String,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(nullable = false)
    var role: String = "USER"
) : UserDetails {

    override fun getUsername(): String = email

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_$role"))

    override fun getPassword(): String = passwordValue

    fun setEncodedPassword(encodedPassword: String) {
        passwordValue = encodedPassword
    }

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = enabled
}