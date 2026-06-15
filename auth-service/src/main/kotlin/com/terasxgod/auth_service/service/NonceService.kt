package com.terasxgod.auth_service.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Сервис для управления nonce (номер одного использования)
 * 
 * Nonce используется в Web3 авторизации:
 * 1. Пользователь запрашивает nonce для своего адреса
 * 2. Подписывает его своим приватным ключом через кошельки
 * 3. Отправляет подпись на сервер
 * 4. Сервер верифицирует подпись с помощью nonce
 * 
 * Nonce хранится в Redis с временем жизни для безопасности
 */
@Service
class NonceService(
    private val redisTemplate: StringRedisTemplate,
    private val web3SignatureService: Web3SignatureService
) {
    private val NONCE_EXPIRATION_MINUTES = 5L  // Nonce живет 5 минут
    private val NONCE_KEY_PREFIX = "web3:nonce:"

    /**
     * Генерирует новый nonce для адреса кошелька
     * 
     * @param walletAddress адрес кошелька
     * @return сгенерированный nonce
     */
    fun generateNonce(walletAddress: String): String {
        val normalizedAddress = normalizeAddress(walletAddress)
        val nonce = generateRandomNonce()
        
        // Сохраняем nonce в Redis с временем жизни
        redisTemplate.opsForValue().set(
            getKeyForAddress(normalizedAddress),
            nonce,
            NONCE_EXPIRATION_MINUTES,
            TimeUnit.MINUTES
        )

        return nonce
    }

    /**
     * Получает nonce из Redis и верифицирует сигнатуру
     * 
     * @param walletAddress адрес кошелька
     * @param signature подпись от кошелька
     * @return true если сигнатура валидна, иначе false
     */
    fun getAndVerifyNonce(walletAddress: String, signature: String): Boolean {
        val normalizedAddress = normalizeAddress(walletAddress)
        val key = getKeyForAddress(normalizedAddress)

        // Получаем nonce из Redis
        val storedNonce = redisTemplate.opsForValue().get(key)
            ?: return false  // Nonce не найден или истек

        // Верифицируем сигнатуру
        val isValidSignature = web3SignatureService.verifySignature(
            normalizedAddress,
            storedNonce,
            signature
        )

        if (!isValidSignature) {
            return false
        }

        // Если сигнатура валидна, удаляем nonce (одноразовое использование)
        redisTemplate.delete(key)

        return true
    }

    /**
     * Нормализует адрес кошелька (приводит к нижнему регистру)
     */
    private fun normalizeAddress(address: String): String {
        // Адреса Ethereum должны быть в нижнем регистре для сравнения
        val normalized = address.lowercase()
        
        // Проверяем, что это валидный адрес (40 символов hex или с префиксом 0x)
        if (normalized.startsWith("0x")) {
            if (normalized.length != 42) {
                throw IllegalArgumentException("Invalid Ethereum address length")
            }
        } else {
            if (normalized.length != 40) {
                throw IllegalArgumentException("Invalid Ethereum address length")
            }
        }
        
        return normalized
    }

    /**
     * Генерирует криптографически стойкий случайный nonce
     */
    private fun generateRandomNonce(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)  // 256 бит для большей безопасности
        random.nextBytes(bytes)
        
        // Преобразуем байты в hex строку
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Возвращает ключ Redis для адреса
     */
    private fun getKeyForAddress(address: String): String {
        return "$NONCE_KEY_PREFIX$address"
    }
}