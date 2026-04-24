package com.terasxgod.notification_service.service

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NotificationEventProcessorTest {

    private val emailSenderService = mockk<EmailSenderService>(relaxed = true)
    private val processor = NotificationEventProcessor(emailSenderService)

    @Test
    fun `processRawMessage calls sender for valid payload`() {
        val payload = """
            {
              "eventId": "evt-1",
              "eventVersion": 1,
              "eventType": "WELCOME_EMAIL",
              "occurredAt": "2026-04-23T18:00:00Z",
              "recipient": {
                "email": "user@example.com",
                "name": "User"
              },
              "template": {
                "templateId": "welcome_v1",
                "subject": "Welcome",
                "params": {
                  "name": "User"
                }
              },
              "metadata": {
                "sourceService": "auth-service",
                "correlationId": "corr-1",
                "tenantId": null
              }
            }
        """.trimIndent()

        processor.processRawMessage(payload)

        verify(exactly = 1) { emailSenderService.send(any()) }
    }

    @Test
    fun `processRawMessage skips invalid payload`() {
        assertThrows<IllegalArgumentException> {
            processor.processRawMessage("not-json")
        }

        verify(exactly = 0) { emailSenderService.send(any()) }
    }
}


