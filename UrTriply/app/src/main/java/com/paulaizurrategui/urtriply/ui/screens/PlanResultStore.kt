package com.paulaizurrategui.urtriply.ui.screens

import com.paulaizurrategui.urtriply.domain.model.Hotel
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.paulaizurrategui.urtriply.domain.model.FlightOffer


/**
 * lo uso para poder pasar datos desde la Pantalla 4 (PlanTabScreen)
 * a la Pantalla 5 (PlanResultScreen)
 */
object PlanResultStore {
    var lastResult: PlanResult? = null
}

/**
 * Modelo de datos que representa la propuesta generada (Pantalla 5).
 *
 * ahoraa ees esstimado (no APIs reales)
 */
data class PlanResult(
    val destino: String,                           // Destino elegido
    val presupuestoTotal: Double,                  // Presupuesto total introducido
    val viajeros: Int,                             // Nº viajeros introducido
    val fechaInicioMillis: Long?,                  // Fecha inicio (en ms)
    val fechaFinMillis: Long?,                     // Fecha fin (en ms)
    val diasRecomendados: Int,                     // Estimación de duración recomendada
    val presupuestoCategorias: Map<String, Double>,// Distribución por categorías
    val itinerario: List<String>,                  // Lista de días: "Día 1: ..."
    val actividadesGratis: List<String>,           // Actividades gratis sugeridas
    val actividadesPago: List<String>,             // Actividades de pago sugeridas
    val usedFallback: Boolean,                      // true si se usó estimación/fallback (sin APIs reales)

    val destinoDisplayName: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val hoteles: List<Hotel> = emptyList(),
    val hotelMesSeleccionado: Hotel? = null,
    val apiHotelesOk: Boolean = false,
    val actividadesReales: List<SuggestedActivity> = emptyList(),
    val apiActividadesOk: Boolean = false
    ,
    val vuelos: List<FlightOffer> = emptyList(),
    val apiVuelosOk: Boolean = false
)