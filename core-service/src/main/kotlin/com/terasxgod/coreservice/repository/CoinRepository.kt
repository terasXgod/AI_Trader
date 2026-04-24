package com.terasxgod.coreservice.repository

import com.terasxgod.coreservice.entity.Coin
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CoinRepository: JpaRepository<Coin, Long> {

}