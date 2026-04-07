package com.paulaizurrategui.urtriply.ui.navigation

object Routes {
    const val WELCOME = "welcome" // Pantalla 0: Bienvenida/Acceso (login, registro o continuar sin cuenta)
    const val LOGIN = "login" // Pantalla de iniciar sesión (Firebase Auth)
    const val REGISTER = "register" // Pantalla de registro (Firebase Auth)

    const val MAIN = "main/{mode}" // Contenedor principal; {mode} define permisos: "auth" (logueado) o "guest" (invitado)
    // Nota: para navegar a MAIN se usa "main/auth" o "main/guest" (rellenando {mode})
}

object MainTabs {
    const val HOME = "tab_home" // Tab/pantalla de inicio dentro del MainShell
    const val PLAN = "tab_plan" // Tab/pantalla de "Planificar viaje" (formulario + generación)
    const val COMMUNITY = "tab_community" // Tab/pantalla comunidad (solo auth; en guest debe pedir login)
    const val PROFILE = "tab_profile" // Tab/pantalla perfil (solo auth; en guest debe pedir login)
}