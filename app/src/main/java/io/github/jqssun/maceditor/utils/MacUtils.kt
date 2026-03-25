package io.github.jqssun.maceditor.utils

import kotlin.random.Random

object MacUtils {

    enum class ValidationResult {
        VALID, BAD_LENGTH, ALL_ZEROS, ODD_FIRST_OCTET
    }

    fun validate(mac: String): ValidationResult {
        if (mac.length != 17) return ValidationResult.BAD_LENGTH
        if (mac == "00:00:00:00:00:00") return ValidationResult.ALL_ZEROS
        val firstOctet = mac.substring(0, 2).toIntOrNull(16) ?: return ValidationResult.BAD_LENGTH
        if (firstOctet % 2 != 0) return ValidationResult.ODD_FIRST_OCTET
        return ValidationResult.VALID
    }

    fun generateRandom(): String {
        val octets = ByteArray(6).also { Random.nextBytes(it) }
        // ensure first octet is even (unicast) and non-zero
        var first = octets[0].toInt() and 0xFF
        if (first % 2 != 0) first++
        if (first == 0) first = 2
        octets[0] = first.toByte()
        // ensure last octet non-zero to avoid all-zeros
        if (octets[5].toInt() and 0xFF == 0) octets[5] = 1
        return octets.joinToString(":") { "%02X".format(it.toInt() and 0xFF) }
    }
}
