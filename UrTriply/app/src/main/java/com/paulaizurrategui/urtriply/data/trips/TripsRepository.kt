package com.paulaizurrategui.urtriply.data.trips

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.domain.model.Hotel
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.paulaizurrategui.urtriply.ui.screens.PlanResult

// repositorio para centralizar las escrituras/updates de trips en firestore
class TripsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // referencia a la coleccion trips
    private val trips = db.collection("trips")

    // guarda un viaje nuevo a partir de una propuesta (borrador o publicado)
    fun saveTripFromPlan(
        plan: PlanResult,
        authorUid: String,
        authorEmail: String?,
        status: TripStatus,
        onSuccess: (tripId: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // preparo el documento a guardar
        val doc = TripDoc(
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
            status = status.name,
            createdAt = Timestamp.now(), // siempre lo seteo al crear
            publishedAt = if (status == TripStatus.PUBLISHED) Timestamp.now() else null // solo si publico
        )

        // una sola escritura (add crea el doc con id automatico)
        trips.add(doc)
            .addOnSuccessListener { ref ->
                Log.d("TripsRepository", "saved trip id=${ref.id} status=${doc.status}")
                onSuccess(ref.id) // devuelvo el id por si lo necesito
            }
            .addOnFailureListener { e ->
                Log.e("TripsRepository", "save failed", e)
                onError(e)
            }
    }

    // publica un viaje ya existente (cambia estado + fecha)
    fun publishExistingTrip(
        tripId: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        trips.document(tripId)
            .update(
                mapOf(
                    "status" to TripStatus.PUBLISHED.name,
                    "publishedAt" to Timestamp.now()
                    // ojo: si quieres que en comunidad salga el nombre, aqui conviene guardar authorname tambien
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
}