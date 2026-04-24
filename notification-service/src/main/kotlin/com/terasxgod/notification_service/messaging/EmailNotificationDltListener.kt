package com.terasxgod.notification_service.messaging

import com.terasxgod.common.kafka.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class EmailNotificationDltListener {
    private val log = LoggerFactory.getLogger(EmailNotificationDltListener::class.java)

    @KafkaListener(
        topics = [KafkaTopics.EMAIL_NOTIFICATIONS_DLT_V1],
        groupId = "${KafkaTopics.NOTIFICATION_SERVICE_GROUP}-dlt"
    )
    fun consumeDlt(rawMessage: String) {
        log.error("Message moved to DLT topic={} payload={}", KafkaTopics.EMAIL_NOTIFICATIONS_DLT_V1, rawMessage)
    }
}

