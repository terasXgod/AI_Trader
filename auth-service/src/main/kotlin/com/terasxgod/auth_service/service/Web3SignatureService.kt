package com.terasxgod.auth_service.service

import org.springframework.stereotype.Service
import org.web3j.crypto.Hash
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.util.Arrays

/**
 * Сервис для верификации Web3 сигнатур от кошельков
 */
@Service
class Web3SignatureService {

    fun verifySignature(walletAddress: String, nonce: String, signature: String): Boolean {
        return try {
            val message = "web3-auth:$nonce"
            val messageHash = hashMessage(message)
            val recoveredAddress = recoverAddressFromSignature(messageHash, signature)
            
            recoveredAddress.equals(walletAddress, ignoreCase = true)
        } catch (e: Exception) {
            println("Error verifying signature: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun hashMessage(message: String): ByteArray {
        val prefix = "Ethereum Signed Message:\n"
        val prefixedMessage = prefix + message.length + message
        return Hash.sha3(prefixedMessage.toByteArray())
    }

    private fun recoverAddressFromSignature(messageHash: ByteArray, signature: String): String {
        try {
            val cleanSignature = if (signature.startsWith("0x")) {
                signature.substring(2)
            } else {
                signature
            }
            
            val signatureBytes = Numeric.hexStringToByteArray(cleanSignature)
            
            if (signatureBytes.size != 65) {
                throw IllegalArgumentException("Invalid signature length: ${signatureBytes.size}")
            }
            
            val r = Arrays.copyOfRange(signatureBytes, 0, 32)
            val s = Arrays.copyOfRange(signatureBytes, 32, 64)
            val vByte = signatureBytes[64]
            
            val signatureData = Sign.SignatureData(vByte, r, s)
            val publicKeyBigInteger = Sign.signedMessageHashToKey(messageHash, signatureData)
            
            // Конвертируем BigInteger в hex string (публичный ключ)
            val publicKeyHex = publicKeyBigInteger.toString(16)
            // Убедимся, что это 128 символов (256 бит = 64 байта hex)
            val paddedPublicKey = publicKeyHex.padStart(128, '0')
            
            // Хешируем публичный ключ
            val publicKeyBytes = Numeric.hexStringToByteArray(paddedPublicKey)
            val publicKeyHash = Hash.sha3(publicKeyBytes)
            
            // Берем последние 20 байт для адреса
            val addressBytes = ByteArray(20)
            System.arraycopy(publicKeyHash, publicKeyHash.size - 20, addressBytes, 0, 20)
            
            // Конвертируем в hex
            val addressHex = Numeric.toHexStringNoPrefix(addressBytes)
            return "0x$addressHex"
        } catch (e: Exception) {
            throw RuntimeException("Failed to recover address from signature: ${e.message}", e)
        }
    }
}








