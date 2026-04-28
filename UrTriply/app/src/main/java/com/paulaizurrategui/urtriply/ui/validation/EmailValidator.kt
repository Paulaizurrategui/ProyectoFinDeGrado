package com.paulaizurrategui.urtriply.ui.validation

object EmailValidator {

    // Dominios permitidos
    private val allowedDomains = setOf(
        "gmail.com",
        "hotmail.com",
        "outlook.com",
        "live.com",
        "yahoo.com",
        "icloud.com"
    )

    fun isAllowed(email: String): Boolean {
        val trimmed = email.trim().lowercase()

        val parts = trimmed.split("@")
        if (parts.size != 2) return false
        if (parts[0].isBlank()) return false

        val domain = parts[1]
        if (domain.isBlank() || !domain.contains(".")) return false

        return domain in allowedDomains
    }
}