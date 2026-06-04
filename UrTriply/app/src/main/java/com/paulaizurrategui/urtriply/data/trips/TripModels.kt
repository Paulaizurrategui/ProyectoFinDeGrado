package com.paulaizurrategui.urtriply.data.trips

import com.google.firebase.Timestamp
import com.paulaizurrategui.urtriply.ui.screens.ItineraryDay
import com.paulaizurrategui.urtriply.ui.screens.PlanResult

// estados del viaje en firestore
enum class TripStatus { DRAFT, PUBLISHED }

// modelo "tal cual" se guarda en la coleccion trips
data class TripDoc(
    // autor (uid y email)
    val authorUid: String = "",
    val authorEmail: String? = null,
    val authorName: String = "usuario",

    // datos del formulario
    val destino: String = "",
    val presupuestoTotal: Double = 0.0,
    val viajeros: Int = 1,
    val fechaInicioMillis: Long? = null,
    val fechaFinMillis: Long? = null,

    // resultado generado (propuesta)
    val diasRecomendados: Int = 0,
    val presupuestoCategorias: Map<String, Double> = emptyMap(),
    val itinerario: List<String> = emptyList(),
    val itineraryByDay: List<ItineraryDay> = emptyList(),
    val usedFallback: Boolean = true, // true si hemos tirado de estimaciones
    val propuestaGenerada: String? = null,

    // estado + timestamps
    val status: String = TripStatus.DRAFT.name,
    val createdAt: Timestamp? = null,
    val publishedAt: Timestamp? = null
)

// helper: convierte un planresult en un tripdoc
// (esto me sirve para guardar borrador o publicar segun el status)
fun tripDocFromPlanResult(
    plan: PlanResult,
    authorUid: String,
    authorEmail: String?,
    authorName: String,
    status: TripStatus
): TripDoc = TripDoc(
    authorUid = authorUid,
    authorEmail = authorEmail,
    authorName = authorName,
    destino = plan.destino,
    presupuestoTotal = plan.presupuestoTotal,
    viajeros = plan.viajeros,
    fechaInicioMillis = plan.fechaInicioMillis,
    fechaFinMillis = plan.fechaFinMillis,
    diasRecomendados = plan.diasRecomendados,
    presupuestoCategorias = plan.presupuestoCategorias,
    itinerario = plan.itinerario,
    itineraryByDay = plan.itineraryByDay,
    usedFallback = plan.usedFallback,
    propuestaGenerada = plan.itinerario.joinToString("\n"),
    status = status.name
    // createdat/publishedat se setean donde se guarda (repositorio/vm)
)