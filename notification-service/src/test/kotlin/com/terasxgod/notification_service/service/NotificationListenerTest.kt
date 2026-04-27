package com.terasxgod.notification_service.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.terasxgod.common.events.EmailEventType
import com.terasxgod.common.events.EmailNotificationEvent
import com.terasxgod.common.events.EmailRecipient
import com.terasxgod.common.events.EmailTemplate
import com.terasxgod.common.events.EventMetadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertFailsWith

class NotificationListenerTest {
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val emailService = mockk<EmailSenderService>()
    private val processor = NotificationEventProcessor(emailService)

    @Test
    fun `should parse valid payload and call email service`() {
        val event = EmailNotificationEvent(
            eventId = "evt-1",
            eventType = EmailEventType.WELCOME_EMAIL,
            occurredAt = "2026-04-26T10:00:00Z",
            recipient = EmailRecipient(email = "test@mail.com", name = "Test User"),
            template = EmailTemplate(
                templateId = "welcome",
                subject = "Hello",
                params = mapOf("firstName" to "Test")
            ),
            metadata = EventMetadata(
                sourceService = "core-service",
                correlationId = "corr-1",
                tenantId = "tenant-1"
            )
        )

        val payload = objectMapper.writeValueAsString(event)
        every { emailService.send(any()) } returns Unit

        processor.processRawMessage(payload)

        verify(exactly = 1) { emailService.send(event) }
    }

    @Test
    fun `should throw IllegalArgumentException for invalid payload`() {
        val badPayload = "{ invalid-json }"

        assertFailsWith<IllegalArgumentException> {
            processor.processRawMessage(badPayload)
        }

        verify(exactly = 0) { emailService.send(any()) }
    }
}