package com.paulaizurrategui.urtriply.ui.theme

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ViewModel ligero que expone la preferencia de tema (oscuro/claro) y la
// persiste en Firestore por usuario.
// Notas de diseño:
// - No cargamos la preferencia automáticamente en init para evitar lecturas
//   de Firestore durante el arranque de la app; se debe llamar a
//   `loadThemePreference()` desde pantallas que la necesiten (p.ej. ProfileTab).
// - Las operaciones que persisten en Firestore manejan revert en caso de fallo
//   para mantener consistencia visual en la UI.
class ThemeViewModel : ViewModel() {

    // Firebase instances para obtener uid del usuario y persistir la preferencia
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Estado local que la UI observa: `true` = tema oscuro, `false` = tema claro
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    // Evitamos leer Firestore en el init para no impactar el cold start.
    // Llamar `loadThemePreference()` cuando se necesite sincronizar la preferencia.
    fun loadThemePreference() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                // Leer el campo `isDarkTheme` y actualizar el StateFlow
                val isDark = doc.getBoolean("isDarkTheme") ?: false
                _isDarkTheme.value = isDark
            }
            .addOnFailureListener {
                // Si falla la lectura, usar modo claro por defecto
                _isDarkTheme.value = false
            }
    }

    // Alterna el valor local y lo persiste en Firestore. Si la persistencia falla,
    // revertimos el cambio local para que la UI refleje el estado real.
    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue

        // Persistir en Firestore (si no hay usuario, salir)
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .update(mapOf("isDarkTheme" to newValue))
            .addOnFailureListener {
                // Revertir localmente en caso de fallo
                _isDarkTheme.value = !_isDarkTheme.value
            }
    }

    // Fija explícitamente el tema y lo persiste. Igual que `toggleTheme` pero con
    // un valor dado (útil para un switch directo en UI).
    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark

        // Persistir en Firestore (si no hay usuario, salir)
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .update(mapOf("isDarkTheme" to isDark))
            .addOnFailureListener {
                // Revertir localmente en caso de fallo
                _isDarkTheme.value = !isDark
            }
    }
}
