package com.terasxgod.notification_service.service

import com.terasxgod.common.events.EmailNotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "notification.mail", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class LoggingEmailSenderService : EmailSenderService {
    private val log = LoggerFactory.getLogger(LoggingEmailSenderService::class.java)

    override fun send(event: EmailNotificationEvent) {
        // MVP: эмулируем отправку письма и фиксируем ключевые поля в логах.
        log.info(
            "Email send simulated eventId={} type={} recipient={} templateId={} correlationId={}",
            event.eventId,
            event.eventType,
            event.recipient.email,
            event.template.templateId,
            event.metadata.correlationId
        )
    }
}


