package com.terasxgod.auth_service.config

import com.terasxgod.auth_service.security.CustomUserDetailsService
import com.terasxgod.auth_service.service.JwtService
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val customUserDetailsService: CustomUserDetailsService
): OncePerRequestFilter() {
    private val publicAuthPrefixes = listOf("/auth/", "/v1/auth/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI ?: ""
        if (publicAuthPrefixes.any { path.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader("Authorization")
        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)
        val username = try {
            jwtService.extractUsername(token)
        } catch (_: JwtException) {
            filterChain.doFilter(request, response)
            return
        } catch (_: IllegalArgumentException) {
            filterChain.doFilter(request, response)
            return
        }

        if (username.isNotBlank() && SecurityContextHolder.getContext().authentication == null) {
            val userDetails = try {
                customUserDetailsService.loadUserByUsername(username)
            } catch (_: UsernameNotFoundException) {
                SecurityContextHolder.clearContext()
                filterChain.doFilter(request, response)
                return
            }
            if (jwtService.isAccessTokenValid(token, userDetails)) {
                val authToken = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }
}