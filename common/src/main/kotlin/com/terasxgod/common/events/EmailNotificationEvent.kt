package com.terasxgod.common.events

enum class EmailEventType {
    WELCOME_EMAIL,
    RESET_PASSWORD_EMAIL,
    SECURITY_ALERT_EMAIL
}

data class EmailRecipient(
    val email: String
)

data class EmailTemplate(
    val templateId: String,
    val subject: String,
    val params: Map<String, String> = emptyMap(),
)

data class EventMetadata(
    val sourceService: String,
    val correlationId: String? = null,
    val tenantId: String? = null,
)

data class EmailNotificationEvent(
    val eventId: String,
    val eventVersion: Int = 1,
    val eventType: EmailEventType,
    val occurredAt: String,
    val recipient: EmailRecipient,
    val template: EmailTemplate,
    val metadata: EventMetadata,
)