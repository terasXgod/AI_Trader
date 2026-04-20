package com.terasxgod.coreservice.controller

import com.terasxgod.coreservice.api.AdminToolApi
import com.terasxgod.coreservice.dto.CreateCoin202Response
import com.terasxgod.coreservice.dto.NewAsset
import com.terasxgod.coreservice.dto.UpdateCoin202Response
import com.terasxgod.coreservice.entity.Coin
import com.terasxgod.coreservice.service.CoinsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminToolController(
    val coinsService: CoinsService
): AdminToolApi {
    override fun createCoin(newAsset: NewAsset): ResponseEntity<CreateCoin202Response> {
        val coin: Coin = coinsService.createCoin(newAsset)
        //реализовать правильно чтобы отправлялся запрос в парсинг сервис, пока что затычка
        return ResponseEntity.accepted().body(CreateCoin202Response("${coin.id}", CreateCoin202Response.Status.PENDING))
    }

    override fun deleteCoin(id: Int): ResponseEntity<Unit> {
        if (id < 0){
            return ResponseEntity.notFound().build()
        }
        try {
            val response: Boolean = coinsService.deleteCoin(id)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.notFound().build()
        } catch (e: Exception) {
            return ResponseEntity.internalServerError().build()
        }
        return ResponseEntity.ok().build()
    }

    override fun updateCoin(
        id: Int,
        newAsset: NewAsset
    ): ResponseEntity<UpdateCoin202Response> {
        val coin: Coin = coinsService.updateCoin(id, newAsset)
        //реализовать правильно чтобы отправлялся запрос в парсинг сервис, пока что затычка
        return ResponseEntity.accepted().body(UpdateCoin202Response("${coin.id}", UpdateCoin202Response.Status.PENDING))
    }

}