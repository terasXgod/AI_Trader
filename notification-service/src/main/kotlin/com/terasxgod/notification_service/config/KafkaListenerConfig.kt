package com.terasxgod.notification_service.config

import com.terasxgod.common.kafka.KafkaTopics
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaListenerConfig(
    @Value("\${spring.kafka.bootstrap-servers}")
    private val bootstrapServers: String,
    @Value("\${notification.kafka.retry.max-attempts:3}")
    private val maxAttempts: Long,
    @Value("\${notification.kafka.retry.backoff-ms:1000}")
    private val backOffMs: Long
) {

    @Bean
    fun notificationDltProducerFactory(): ProducerFactory<String, String> {
        val props = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true
        )
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun notificationDltKafkaTemplate(
        notificationDltProducerFactory: ProducerFactory<String, String>
    ): KafkaTemplate<String, String> {
        return KafkaTemplate(notificationDltProducerFactory)
    }

    @Bean
    fun kafkaErrorHandler(
        notificationDltKafkaTemplate: KafkaTemplate<String, String>
    ): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(notificationDltKafkaTemplate) { record, _ ->
            TopicPartition(KafkaTopics.EMAIL_NOTIFICATIONS_DLT_V1, record.partition())
        }

        val retries = if (maxAttempts > 0) maxAttempts - 1 else 0
        return DefaultErrorHandler(recoverer, FixedBackOff(backOffMs, retries))
    }
}



