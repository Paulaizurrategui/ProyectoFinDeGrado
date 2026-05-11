# Admin Panel Setup Guide

## 🔧 Cómo acceder al Panel Admin

Para acceder al Panel Admin en la app, tu usuario debe tener `esAdmin: true` en Firestore.

### Opción 1: Configurar Admin en Firestore Console (RECOMENDADO)

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Abre tu proyecto "UrTriply"
3. Ve a **Firestore Database**
4. Navega a la colección `users` → selecciona tu documento (identificado por tu UID)
5. Busca el campo `esAdmin` 
   - Si no existe, haz clic en **Add field**
   - Nombre: `esAdmin`
   - Tipo: `boolean`
   - Valor: `true`
6. Guarda los cambios
7. Cierra la app completamente y reabre (o recarga la pantalla de perfil)
8. Ahora deberías ver la tarjeta "Panel Admin" en tu perfil

### Opción 2: Configurar Admin mediante código (Testing)

Si quieres hacerlo por código, puedes:

1. Abre el archivo `MainActivity.kt`
2. Agrega esto después del login:

```kotlin
// Temporal: Set admin for testing
FirebaseFirestore.getInstance()
    .collection("users")
    .document(uid)
    .update(mapOf("esAdmin" to true))
    .addOnSuccessListener {
        Log.d("Admin", "User set as admin")
    }
```

## 📍 Ubicación del Panel Admin en la App

- **Ruta**: Profile Tab → Tarjeta "Panel Admin" (solo visible si `esAdmin: true`)
- **Funciones**:
  - Ver lista de reportes pendientes
  - Resolver reportes
  - Descartar reportes
  - Bloquear usuarios
  - Eliminar viajes reportados
  - Eliminar comentarios reportados

## 🔐 Estructura en Firestore

```
/users/{uid}
├── email: string
├── displayName: string
├── isDarkTheme: boolean
├── isOver13Confirmed: boolean
├── esAdmin: boolean ← AGREGAR AQUÍ
├── blockedByUserIds: array
├── following: array
└── ...

/reports/{reportId}
├── id: string
├── targetType: "TRIP" | "COMMENT"
├── targetId: string
├── reporterUid: string
├── reason: string
├── status: "OPEN" | "RESOLVED" | "DISMISSED"
└── createdAt: timestamp
```

## ✅ Verificar que es Admin

Una vez configurado, verifica:

1. Abre el Profile tab
2. Deberías ver una tarjeta verde que dice "Panel Admin"
3. Haz clic para entrar
4. Deberías ver la lista de reportes (si hay alguno)

## 🐛 Troubleshooting

- **No veo la tarjeta Admin después de cambiar esAdmin a true**:
  - Cierra y reabre la app
  - Verifica que estés en la pestaña correcta (Profile)
  - Recarga la pantalla (pull-to-refresh o navega afuera y vuelve)

- **La tarjeta Admin no es clickable**:
  - Verifica que `isAdmin.value` está siendo actualizado
  - Revisa los logs: `adb logcat | grep Admin`

- **No se carga el panel**:
  - Verifica tu conexión a Firestore
  - Comprueba que tienes las reglas de Firebase correctas
