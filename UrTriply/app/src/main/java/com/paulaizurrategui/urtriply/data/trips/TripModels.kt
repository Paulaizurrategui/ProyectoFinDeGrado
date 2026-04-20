package com.paulaizurrategui.urtriply.data.trips

import com.google.firebase.Timestamp
import com.paulaizurrategui.urtriply.ui.screens.PlanResult

enum class TripStatus { DRAFT, PUBLISHED }

data class TripDoc(
    val authorUid: String = "",
    val authorEmail: String? = null,

    val destino: String = "",
    val presupuestoTotal: Double = 0.0,
    val viajeros: Int = 1,
    val fechaInicioMillis: Long? = null,
    val fechaFinMillis: Long? = null,

    val diasRecomendados: Int = 0,
    val presupuestoCategorias: Map<String, Double> = emptyMap(),
    val itinerario: List<String> = emptyList(),
    val actividadesGratis: List<String> = emptyList(),
    val actividadesPago: List<String> = emptyList(),
    val usedFallback: Boolean = true,

    val status: String = TripStatus.DRAFT.name,
    val createdAt: Timestamp? = null,
    val publishedAt: Timestamp? = null
)

fun TripDoc.CompanionFromPlanResult(
    plan: PlanResult,
    authorUid: String,
    authorEmail: String?,
    status: TripStatus
): TripDoc = TripDoc(
    authorUid = authorUid,
    authorEmail = authorEmail,
    destino = plan.destino,
    presupuestoTotal = plan.presupuestoTotal,
    viajeros = plan.viajeros,
    fechaInicioMillis = plan.fechaInicioMillis,
    fechaFinMillis = plan.fechaFinMillis,
    diasRecomendados = plan.diasRecomendados,
    presupuestoCategorias = plan.presupuestoCategorias,
    itinerario = plan.itinerario,
    actividadesGratis = plan.actividadesGratis,
    actividadesPago = plan.actividadesPago,
    usedFallback = plan.usedFallback,
    status = status.name
)