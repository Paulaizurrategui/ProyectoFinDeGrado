package com.paulaizurrategui.urtriply.data.trips

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.ui.screens.PlanResult

class TripsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val trips = db.collection("trips")

    fun saveTripFromPlan(
        plan: PlanResult,
        authorUid: String,
        authorEmail: String?,
        status: TripStatus,
        onSuccess: (tripId: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
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
            usedFallback = plan.usedFallback,
            status = status.name,
            createdAt = Timestamp.now(),
            publishedAt = if (status == TripStatus.PUBLISHED) Timestamp.now() else null
        )

        trips.add(doc)
            .addOnSuccessListener { ref -> onSuccess(ref.id) }
            .addOnFailureListener { e -> onError(e) }
        trips.add(doc)
            .addOnSuccessListener { ref ->
                android.util.Log.d("TripsRepository", "Saved trip id=${ref.id} status=${doc.status}")
                onSuccess(ref.id)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("TripsRepository", "Save failed", e)
                onError(e)
            }
    }
}