package com.paulaizurrategui.urtriply.ui.validation

object EmailValidator {

    // Lista blanca de dominios aceptados para registro/login social.
    // Si necesitas permitir otros dominios (empresa, edu) añadirlos aquí.
    private val allowedDomains = setOf(
        "gmail.com",
        "hotmail.com",
        "outlook.com",
        "live.com",
        "yahoo.com",
        "icloud.com"
    )

    // Comprueba si un email está permitido por la app.
    // Validaciones realizadas (rápidas y defensivas):
    // 1) trim + lowercase para normalizar
    // 2) debe contener exactamente un '@' y la parte local no puede estar vacía
    // 3) el dominio no puede estar vacío y debe contener al menos un '.'
    // 4) el dominio debe estar en la lista `allowedDomains`
    fun isAllowed(email: String): Boolean {
        // Normalizo espacios y mayúsculas
        val trimmed = email.trim().lowercase()

        // Separación local@dominio
        val parts = trimmed.split("@")
        if (parts.size != 2) return false // no hay exactamente una '@'
        if (parts[0].isBlank()) return false // parte local vacía

        val domain = parts[1]
        if (domain.isBlank() || !domain.contains(".")) return false // dominio inválido

        // Finalmente compruebo si el dominio está en la lista permitida
        return domain in allowedDomains
    }
}