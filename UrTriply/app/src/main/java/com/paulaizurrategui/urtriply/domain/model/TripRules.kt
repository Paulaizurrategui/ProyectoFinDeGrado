package com.paulaizurrategui.urtriply.domain.model

import java.util.concurrent.TimeUnit

fun countTripNights(fechaInicioMillis: Long?, fechaFinMillis: Long?): Int {
    if (fechaInicioMillis == null || fechaFinMillis == null) return 1
    val diffMillis = fechaFinMillis - fechaInicioMillis
    if (diffMillis <= 0L) return 1
    val nights = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
    return nights.coerceAtLeast(1)
}
