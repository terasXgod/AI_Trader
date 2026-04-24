package com.terasxgod.coreservice.controller

import com.terasxgod.coreservice.api.PositionsApi
import com.terasxgod.coreservice.dto.Position
import com.terasxgod.coreservice.dto.PositionDetails
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class PositionController: PositionsApi {
    override fun positionsGet(): ResponseEntity<List<Position>> {
        return super.positionsGet()
    }

    override fun positionsIdGet(id: Int): ResponseEntity<PositionDetails> {
        return super.positionsIdGet(id)
    }
}