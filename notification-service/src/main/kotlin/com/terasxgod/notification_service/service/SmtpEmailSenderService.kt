package com.terasxgod.notification_service.service

import com.terasxgod.common.events.EmailNotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "notification.mail", name = ["enabled"], havingValue = "true")
class SmtpEmailSenderService(
    private val javaMailSender: JavaMailSender
) : EmailSenderService {
    private val log = LoggerFactory.getLogger(SmtpEmailSenderService::class.java)

    override fun send(event: EmailNotificationEvent) {
        val message = SimpleMailMessage()
        message.setTo(event.recipient.email)
        message.subject = event.template.subject
        message.text = buildBody(event)

        javaMailSender.send(message)
        log.info("Email sent via SMTP eventId={} recipient={}", event.eventId, event.recipient.email)
    }

    private fun buildBody(event: EmailNotificationEvent): String {
        val paramsText = if (event.template.params.isEmpty()) {
            "(no template params)"
        } else {
            event.template.params.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        }

        return """
            Event type: ${event.eventType}
            Template: ${event.template.templateId}

            Params:
            $paramsText
        """.trimIndent()
    }
}

