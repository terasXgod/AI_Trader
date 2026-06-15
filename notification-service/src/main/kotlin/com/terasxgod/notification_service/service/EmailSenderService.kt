package com.terasxgod.notification_service.service

import com.terasxgod.common.events.EmailNotificationEvent

interface EmailSenderService {
    fun send(event: EmailNotificationEvent)
}

