package com.paulaizurrategui.urtriply.ui.navigation

// rutas del nav principal (fuera de los tabs)
object Routes {
    const val WELCOME = "welcome"   // pantalla 0: bienvenida
    const val LOGIN = "login"       // pantalla login
    const val REGISTER = "register" // pantalla registro

    // contenedor principal (recibe modo: auth o guest)
    const val MAIN = "main/{mode}"  // navegar con "main/auth" o "main/guest"
    
    // admin moderation panel (solo admins)
    const val ADMIN = "admin_moderate"
}

// tabs dentro del mainshell (bottom nav)
object MainTabs {
    const val HOME = "tab_home"           // inicio
    const val PLAN = "tab_plan"           // planificar
    const val COMMUNITY = "tab_community" // comunidad
    const val PROFILE = "tab_profile"     // perfil
}