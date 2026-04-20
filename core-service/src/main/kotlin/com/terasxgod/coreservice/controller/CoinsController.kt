package com.terasxgod.coreservice.controller

import com.terasxgod.coreservice.api.CoinsApi
import com.terasxgod.coreservice.dto.Asset
import com.terasxgod.coreservice.service.CoinsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CoinsController (
    val coinsService: CoinsService
): CoinsApi {
    override fun coinsGet(sortBy: String?): ResponseEntity<List<Asset>> {
        val coins = coinsService.getCoins()
        // нужно добавить логику, которая из этих монет будет подтягивать цены для этих активов из парсера
        // пока будет затычка с рандомными ценами
        val assets: List<Asset> = coins.stream()
            .map { coin ->
                Asset(
                    id = coin.id?.toInt() ?: 0,
                    name = coin.name,
                    symbol = coin.symbol,
                    currentPrice = java.math.BigDecimal
                        .valueOf(java.util.concurrent.ThreadLocalRandom.current().nextDouble(0.01, 100000.0))
                        .setScale(2, java.math.RoundingMode.HALF_UP),
                    iconUrl = coin.iconUrl
                )
            }
            .toList()
        return ResponseEntity.ok(assets)
    }
}