package com.paulaizurrategui.urtriply.ui.theme

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    // Do not auto-load preferences at init to avoid Firestore read during app startup.
    // Call `loadThemePreference()` from screens that need it (e.g., ProfileTabScreen).

    fun loadThemePreference() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val isDark = doc.getBoolean("isDarkTheme") ?: false
                _isDarkTheme.value = isDark
            }
            .addOnFailureListener {
                // Usar light mode por defecto si falla
                _isDarkTheme.value = false
            }
    }

    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue

        // Persistir en Firestore
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .update(mapOf("isDarkTheme" to newValue))
            .addOnFailureListener {
                // Si falla, revertir localmente
                _isDarkTheme.value = !newValue
            }
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark

        // Persistir en Firestore
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .update(mapOf("isDarkTheme" to isDark))
            .addOnFailureListener {
                // Si falla, revertir localmente
                _isDarkTheme.value = !isDark
            }
    }
}
