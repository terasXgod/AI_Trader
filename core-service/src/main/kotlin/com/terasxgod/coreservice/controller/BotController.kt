package com.terasxgod.coreservice.controller

import com.terasxgod.coreservice.api.BotApi
import com.terasxgod.coreservice.dto.BotSettings
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class BotController: BotApi {
    override fun botSettingsGet(): ResponseEntity<BotSettings> {
        TODO("Need to send request to trading service")
        return super.botSettingsGet()
    }

    override fun botSettingsPatch(): ResponseEntity<Unit> {
        TODO("Need to send request to trading service")
        return super.botSettingsPatch()
    }
}