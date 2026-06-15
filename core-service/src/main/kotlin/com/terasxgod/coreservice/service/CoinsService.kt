package com.terasxgod.coreservice.service

import com.terasxgod.coreservice.dto.Asset
import com.terasxgod.coreservice.dto.NewAsset
import com.terasxgod.coreservice.entity.Coin
import com.terasxgod.coreservice.repository.CoinRepository
import org.springframework.stereotype.Service
import kotlin.math.absoluteValue

@Service
class CoinsService(
    private val coinRepository: CoinRepository
) {
    fun getCoins(): List<Coin> {
        return coinRepository.findAll()
    }

    fun createCoin(asset: NewAsset): Coin {
        // тут должно быть обращение в парсинг сервис, чтобы он нашёл такую монету или не нашёл
        // пока что тут будет просто затычка, которая создаёт монету с рандомным именем
        val coin = Coin(
            id = asset.id.toLong(),
            iconUrl = asset.iconUrl,
            name = "test${asset.id}",
            symbol = "LOL${asset.id}"
        )
        coinRepository.save(coin)
        return coin
    }

    fun deleteCoin(id: Int): Boolean {
        try {
            coinRepository.deleteById(id.toLong())
            return true
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            return false
        }
    }

    fun updateCoin(id: Int, asset: NewAsset): Coin {
        val coin = coinRepository.findById(id.toLong())
            .orElseThrow { Exception("Coin with id $id not found") }

        coin.id = asset.id.toLong()
        coin.iconUrl = asset.iconUrl

        return coinRepository.save(coin)

    }
}