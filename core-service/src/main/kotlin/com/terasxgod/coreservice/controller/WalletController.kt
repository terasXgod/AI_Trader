package com.terasxgod.coreservice.controller

import com.terasxgod.coreservice.api.WalletApi
import com.terasxgod.coreservice.dto.PortfolioAsset
import com.terasxgod.coreservice.dto.WalletInfo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class WalletController: WalletApi {
    override fun walletExistingCoinsGet(): ResponseEntity<List<PortfolioAsset>> {
        return super.walletExistingCoinsGet()
    }

    override fun walletGet(): ResponseEntity<WalletInfo> {
        return super.walletGet()
    }

    override fun walletHistoryGet(
        interval: String,
        startTime: Int?,
        endTime: Int?
    ): ResponseEntity<List<WalletInfo>> {
        return super.walletHistoryGet(interval, startTime, endTime)
    }
}