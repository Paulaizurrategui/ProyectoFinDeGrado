package com.paulaizurrategui.urtriply.data.trips

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.domain.model.ActivityDoc
import com.paulaizurrategui.urtriply.domain.model.Hotel
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.paulaizurrategui.urtriply.ui.screens.PlanResult

// Repositorio para centralizar las escrituras/updates de `trips` en Firestore.
// Lo uso para aislar la lógica de persistencia y evitar que la UI sepa
// detalles del esquema de Firestore.
class TripsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Repositorio responsable de persistir `trips` y subcolecciones relacionadas.
    // Intención: aislar la lógica de escritura/actualización de Firestore para que
    // la UI/ViewModels llamen a métodos simples y no conozcan el esquema.
    // referencia a la coleccion trips
    private val trips = db.collection("trips")

    // Guarda un viaje nuevo a partir de una `PlanResult`.
    // - `status` decide si lo guardo como DRAFT o PUBLISHED
    // - llamo a `onSuccess` con el id generado por Firestore
    // Nota: no hago transforms complejos aquí, solo mapeo y escribo.
    fun saveTripFromPlan(
        plan: PlanResult,
        authorUid: String,
        authorEmail: String?,
        status: TripStatus,
        onSuccess: (tripId: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // Preparo el documento a guardar (mapeo desde PlanResult a TripDoc)
        // Solo incluyo los campos necesarios para reabrir/mostrar la propuesta.
        val baseDoc = tripDocFromPlanResult(plan, authorUid, authorEmail, status)
        val doc = baseDoc.copy(
            createdAt = Timestamp.now(),
            publishedAt = if (status == TripStatus.PUBLISHED) Timestamp.now() else null
        )

        // Escritura única: `add` crea el documento con id automático.
        // - Si tiene éxito, guardo actividades en subcolección y retorno el id.
        // - Si falla, propago el error vía `onError`.
        trips.add(doc)
            .addOnSuccessListener { ref ->
                // Log y persistencia de actividades relacionadas
                Log.d("TripsRepository", "saved trip id=${ref.id} status=${doc.status}")
                // Guardo las actividades sugeridas en la sub-colección 'activities'
                // (se hace por separado para mantener la inserción del doc padre simple)
                saveTripActivities(ref.id, plan.actividadesReales)
                // Devuelvo el id generado para que el llamador pueda referenciar el trip
                onSuccess(ref.id)
            }
            .addOnFailureListener { e ->
                Log.e("TripsRepository", "save failed", e)
                onError(e)
            }
    }

    // Publica un viaje ya existente: actualiza `status` y `publishedAt`.
    // Lo uso cuando el usuario decide publicar un borrador. No devuelve el
    // documento completo, solo indica éxito/fallo vía callbacks.
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

    private fun saveTripActivities(tripId: String, activities: List<SuggestedActivity>) {
        // Si no hay actividades, no hay sub-documentos que crear
        if (activities.isEmpty()) return

        // Sub-colección `activities` bajo el documento del viaje
        val activitiesRef = trips.document(tripId).collection("activities")
        activities.forEach { activity ->
            // Mapeo de SuggestedActivity a ActivityDoc para persistir
            val activityDoc = ActivityDoc(
                tripId = tripId,
                name = activity.name,
                category = activity.category,
                priceEUR = activity.price,
                isGratis = activity.isFree,
                enlace = activity.bookingUrl,
                isReal = activity.isReal,
                createdAt = Timestamp.now()
            )

            // Uso el id de la actividad (si lo tengo) como id del documento.
            // Si falla la escritura de una actividad, registro la advertencia pero continúo
            // con las demás actividades; la operación global no hace rollback.
            activitiesRef.document(activity.id).set(activityDoc)
                .addOnFailureListener { e ->
                    Log.w("TripsRepository", "Failed to save activity ${activity.id} for trip $tripId", e)
                }
        }
    }
}