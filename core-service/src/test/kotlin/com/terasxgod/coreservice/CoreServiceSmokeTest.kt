package com.terasxgod.coreservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoreServiceSmokeTest {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun coinsEndpointReturns200() {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/coins"))
            .GET()
            .build()

        val response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(HttpStatus.OK.value(), response.statusCode())
    }
}



