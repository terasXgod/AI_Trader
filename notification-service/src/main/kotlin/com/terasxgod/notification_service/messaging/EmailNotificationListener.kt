package com.terasxgod.notification_service.messaging

import com.terasxgod.common.kafka.KafkaTopics
import com.terasxgod.notification_service.service.NotificationEventProcessor
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class EmailNotificationListener(
    private val notificationEventProcessor: NotificationEventProcessor
) {
    @KafkaListener(
        topics = [KafkaTopics.EMAIL_NOTIFICATIONS_V1]
    )
    fun consume(rawMessage: String) {
        notificationEventProcessor.processRawMessage(rawMessage)
    }
}

