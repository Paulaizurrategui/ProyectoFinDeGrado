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

// Nota personal: uso este singleton como un "puente" simple entre pantallas.
// Es una solución rápida para pasar el resultado generado sin persistirlo.
// En producción evaluaría usar un ViewModel compartido o navegación con argumentos.

data class ItineraryActivityLink(
    val name: String = "",
    val bookingUrl: String? = null
)

// Actividad dentro de un día del itinerario: nombre y posible enlace de reserva.
// Lo dejé mínimo porque sólo necesitamos mostrar nombre y abrir link si existe.

data class ItineraryDay(
    val dayLabel: String = "",
    val summary: String = "",
    val activities: List<ItineraryActivityLink> = emptyList()
)

// Representa un día del itinerario con etiqueta (Día 1, Día 2...), resumen
// y una lista de `ItineraryActivityLink` con las actividades y posible bookingUrl.

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
    val itineraryByDay: List<ItineraryDay> = emptyList(),
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
    val apiVuelosOk: Boolean = false,
    val tripId: String? = null  // Firestore document ID (set when published)
)


// - Este data class concentra TODO lo que la pantalla de resultados necesita.
// - `usedFallback` me ayuda a mostrar banners indicando que algunos datos
//   no vinieron de APIs externas sino se estimaron localmente.
// - Los flags `apiHotelesOk`, `apiActividadesOk`, `apiVuelosOk` permiten
//   condicionar la UI (mostrar secciones u ofrecer enlaces reales).
// - `tripId` se rellena al publicar y es clave para habilitar comentarios.