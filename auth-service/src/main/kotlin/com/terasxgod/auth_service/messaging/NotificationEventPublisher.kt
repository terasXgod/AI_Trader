package com.terasxgod.auth_service.messaging

import com.terasxgod.common.events.EmailEventType
import com.terasxgod.common.events.EmailNotificationEvent
import com.terasxgod.common.events.EmailRecipient
import com.terasxgod.common.events.EmailTemplate
import com.terasxgod.common.events.EventMetadata
import com.terasxgod.common.kafka.KafkaTopics
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class NotificationEventPublisher(
	private val kafkaTemplate: KafkaTemplate<String, String>
) {
	private val log = LoggerFactory.getLogger(NotificationEventPublisher::class.java)
	private val objectMapper = jacksonObjectMapper().findAndRegisterModules()

	fun publishWelcomeEmail(email: String, name: String? = null, correlationId: String? = null) {
		publish(
			buildEvent(
				eventType = EmailEventType.WELCOME_EMAIL,
				email = email,
				name = name,
				subject = "Добро пожаловать!",
				templateId = "welcome_v1",
				correlationId = correlationId
			)
		)
	}

	fun publishResetPasswordEmail(email: String, name: String? = null, correlationId: String? = null) {
		publish(
			buildEvent(
				eventType = EmailEventType.RESET_PASSWORD_EMAIL,
				email = email,
				name = name,
				subject = "Сброс пароля",
				templateId = "reset_password_v1",
				correlationId = correlationId
			)
		)
	}

	private fun buildEvent(
		eventType: EmailEventType,
		email: String,
		name: String?,
		subject: String,
		templateId: String,
		correlationId: String?
	): EmailNotificationEvent {
		val safeName = name ?: email.substringBefore("@")
		return EmailNotificationEvent(
			eventId = UUID.randomUUID().toString(),
			eventVersion = 1,
			eventType = eventType,
			occurredAt = Instant.now().toString(),
			recipient = EmailRecipient(email = email, name = safeName),
			template = EmailTemplate(
				templateId = templateId,
				subject = subject,
				params = mapOf("name" to safeName)
			),
			metadata = EventMetadata(
				sourceService = "auth-service",
				correlationId = correlationId
			)
		)
	}

	private fun publish(event: EmailNotificationEvent) {
		val payload = try {
			objectMapper.writeValueAsString(event)
		} catch (ex: JsonProcessingException) {
			log.error(
				"Failed to serialize email eventId={} type={} email={}",
				event.eventId,
				event.eventType,
				event.recipient.email,
				ex
			)
			return
		}

		kafkaTemplate.send(KafkaTopics.EMAIL_NOTIFICATIONS_V1, event.recipient.email, payload)
			.whenComplete { result, ex ->
				if (ex != null) {
					log.error(
						"Failed to publish email eventId={} type={} email={}",
						event.eventId,
						event.eventType,
						event.recipient.email,
						ex
					)
				} else {
					log.info(
						"Published email eventId={} type={} topic={} partition={} offset={}",
						event.eventId,
						event.eventType,
						KafkaTopics.EMAIL_NOTIFICATIONS_V1,
						result.recordMetadata.partition(),
						result.recordMetadata.offset()
					)
				}
			}
	}
}