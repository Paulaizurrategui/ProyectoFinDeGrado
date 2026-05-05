package com.paulaizurrategui.urtriply.data.trips

import com.google.firebase.Timestamp
import com.paulaizurrategui.urtriply.domain.model.Hotel
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.paulaizurrategui.urtriply.ui.screens.PlanResult

// estados del viaje en firestore
enum class TripStatus { DRAFT, PUBLISHED }

// modelo "tal cual" se guarda en la coleccion trips
data class TripDoc(
    // autor (uid y email)
    val authorUid: String = "",
    val authorEmail: String? = null,

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
    val actividadesGratis: List<String> = emptyList(),
    val actividadesPago: List<String> = emptyList(),
    val hoteles: List<Hotel> = emptyList(),
    val actividadesReales: List<SuggestedActivity> = emptyList(),
    val usedFallback: Boolean = true, // true si hemos tirado de estimaciones

    // estado + timestamps
    val status: String = TripStatus.DRAFT.name,
    val createdAt: Timestamp? = null,
    val publishedAt: Timestamp? = null
)

// helper: convierte un planresult en un tripdoc
// (esto me sirve para guardar borrador o publicar segun el status)
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
    hoteles = plan.hoteles,
    actividadesReales = plan.actividadesReales,
    usedFallback = plan.usedFallback,
    status = status.name
    // createdat/publishedat se setean donde se guarda (repositorio/vm)
)