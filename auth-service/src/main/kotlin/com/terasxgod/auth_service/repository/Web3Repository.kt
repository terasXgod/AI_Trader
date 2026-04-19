package com.terasxgod.auth_service.repository

import com.terasxgod.auth_service.entity.Web3
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface Web3Repository : JpaRepository<Web3, Long> {
    /**
     * Поиск Web3 кошелька по адресу
     * @return Web3 запись или null если не найдена
     */
    fun findByWalletAddress(walletAddress: String): Web3?
}