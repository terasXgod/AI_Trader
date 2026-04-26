package com.terasxgod.notification_service.config

import com.terasxgod.common.kafka.KafkaTopics
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
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
    @Value("\${spring.kafka.consumer.group-id:${KafkaTopics.NOTIFICATION_SERVICE_GROUP}}")
    private val consumerGroupId: String,
    @Value("\${spring.kafka.consumer.auto-offset-reset:earliest}")
    private val autoOffsetReset: String,
    @Value("\${spring.kafka.listener.auto-startup:true}")
    private val listenerAutoStartup: Boolean,
    @Value("\${notification.kafka.retry.max-attempts:3}")
    private val maxAttempts: Long,
    @Value("\${notification.kafka.retry.backoff-ms:1000}")
    private val backOffMs: Long
) {

    @Bean
    fun notificationConsumerFactory(): ConsumerFactory<String, String> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to consumerGroupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
        )
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean("kafkaListenerContainerFactory")
    fun kafkaListenerContainerFactory(
        notificationConsumerFactory: ConsumerFactory<String, String>,
        kafkaErrorHandler: DefaultErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setConsumerFactory(notificationConsumerFactory)
        factory.setCommonErrorHandler(kafkaErrorHandler)
        factory.setAutoStartup(listenerAutoStartup)
        return factory
    }

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



