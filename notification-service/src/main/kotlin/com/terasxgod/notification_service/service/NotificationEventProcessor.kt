package com.terasxgod.notification_service.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.terasxgod.common.events.EmailNotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NotificationEventProcessor(
    private val emailSenderService: LoggingEmailSenderService
) {
    private val log = LoggerFactory.getLogger(NotificationEventProcessor::class.java)
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()

    fun processRawMessage(payload: String) {
        val event = try {
            objectMapper.readValue<EmailNotificationEvent>(payload)
        } catch (ex: Exception) {
            log.error("Invalid email notification payload: {}", payload, ex)
            throw IllegalArgumentException("Invalid email notification payload", ex)
        }

        emailSenderService.send(event)
    }
}


