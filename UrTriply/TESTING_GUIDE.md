# Guía de Verificación - Modificaciones UrTriply

## Resumen de Cambios Implementados

### 1. **Localización en Inglés (values-en/)**
**Archivos:** `app/src/main/res/values-en/strings.xml`

**Cómo verificar:**
1. En el emulador/dispositivo: Settings → Language & input → Language
2. Cambiar a English
3. Reiniciar la app
4. Todas las pantallas (Login, Register, Home, tabs, etc.) deben estar en inglés

**Archivos clave creados:**
- `app/src/main/res/values-en/strings.xml` (100+ strings traducidas)

---

### 2. **Verificación de Edad +13 años (RF-31)**
**Archivos:** 
- `RegisterScreen.kt` (checkbox visible)
- `AuthViewModel.kt` (persistencia en Firestore)
- `CommunityTabScreen.kt` (bloqueo de acceso)

**Cómo verificar:**
1. Ir a **Register**
2. Llenar formulario: email, password, repeat password
3. Ver checkbox "Tengo más de 13 años / I'm over 13"
4. **Sin marcar:** clic en "Registrarse" → error "Debes confirmar que tienes más de 13 años"
5. **Marcado:** click "Registrarse" → debería funcionar
6. Ir a **Community tab**: 
   - Si es menor (no confirmó +13) → mensaje "Acceso Restringido"
   - Si es mayor → muestra feed normal
7. En Firestore, ir a `/users/{uid}` → campo `isOver13Confirmed: true/false`

**Archivos clave modificados:**
- `RegisterScreen.kt` (línea ~235: bloque de edad)
- `AuthViewModel.kt` (línea ~28: parámetro `isOver13`, línea ~60: guardar en Firestore)
- `CommunityTabScreen.kt` (línea ~38: check antes de mostrar feed)

---

### 3. **Bloqueo de Usuarios**
**Archivos:**
- `ReportRepository.kt` (blockUser, unblockUser)
- `CommunityViewModel.kt` (filtrado en feed)
- `AdminViewModel.kt` (acciones admin)

**Cómo verificar:**
1. **Como Admin:**
   - Ir a **Profile** → "Panel Admin" (visible solo si `esAdmin: true` en Firestore)
   - Clic en "Panel Admin"
   - Ver lista de reportes
   - Clic en botón "Bloquear" en un reporte → bloqueará usuario
   
2. **Resultado en feed:**
   - Usuario bloqueado NO ve posts de quien lo bloqueó
   - Quien bloqueó NO ve posts del usuario bloqueado
   
3. **En Firestore:**
   - `/users/{adminUid}/blocks/{userBlockedUid}` → documento existe
   - `/users/{userBlockedUid}` → array `blockedByUserIds` contiene adminUid

**Archivos clave modificados:**
- `ReportRepository.kt` (línea ~169: blockUser con collections anidadas)
- `CommunityViewModel.kt` (línea ~166: loadBlockedUsers, línea ~210: filtrado en feed)
- `AdminViewModel.kt` (línea ~172: blockUser, línea ~197: unblockUser)

---

### 4. **Localización en Inglés + Dark Mode**
**Archivos:**
- `values-en/strings.xml` (nuevo archivo)
- `ThemeViewModel.kt` (state para dark theme)
- `MainActivity.kt` (usa ThemeViewModel)
- `ProfileTabScreen.kt` (toggle switch para cambiar tema)

**Cómo verificar Dark Mode:**
1. Ir a **Profile tab**
2. Ver card "Modo Oscuro / Dark Mode" o "Modo Claro / Light Mode"
3. Toggle switch ON → app pasa a tema oscuro
4. Toggle switch OFF → app vuelve a tema claro
5. Cambiar de pantalla y volver a Profile → tema se mantiene (persistido en Firestore)
6. En Firestore: `/users/{uid}` → campo `isDarkTheme: true/false`

**Archivos clave:**
- `ThemeViewModel.kt` (nuevo)
- `MainActivity.kt` (línea ~25: usa ThemeViewModel)
- `ProfileTabScreen.kt` (línea ~70: toggle dark mode visible)

---

### 5. **Comentarios en Publicaciones**
**Archivos:**
- `CommentViewModel.kt` (CRUD de comentarios)
- `CommentRepository.kt` (Firestore ops)
- `CommentSection.kt` (UI componente)
- `PlanResultScreen.kt` (integración)

**Cómo verificar:**
1. **Como usuario autenticado:**
   - Ir a **Community** → seleccionar un post publicado
   - Ver sección de comentarios abajo
   - Input campo "Escribe un comentario..."
   - Escribir algo → clic botón de envío
   - Comentario debe aparecer al instante con tu nombre/avatar
   - Otros usuarios verán el comentario
   
2. **Eliminar comentario:**
   - En tu propio comentario → botón X
   - Confirmar eliminación → comentario desaparece
   
3. **En Firestore:**
   - `/trips/{tripId}/comments/{commentId}` → documento con text, author, timestamp
   - Cada comentario tiene likesCount visible

**Archivos clave:**
- `CommentViewModel.kt` (línea ~80: loadCommentsForTrip, addComment, deleteComment)
- `CommentRepository.kt` (línea ~20: addComment, getCommentsForTrip)
- `CommentSection.kt` (UI del componente, línea ~30: CommentCard)
- `PlanResultScreen.kt` (línea ~315: integración CommentSection)

---

### 6. **Panel Admin y Reportes (RF-29)**
**Archivos:**
- `AdminModerateScreen.kt` (UI del panel)
- `AdminViewModel.kt` (state management)
- `ReportRepository.kt` (Firestore operations)
- `ProfileTabScreen.kt` (acceso al panel)

**Cómo verificar:**
1. **Acceso al panel:**
   - Ir a **Profile tab**
   - Ver tarjeta "Panel Admin" (solo visible si `esAdmin: true`)
   - Clic → abre `AdminModerateScreen`
   
2. **En el panel:**
   - Ver lista de reportes abiertos
   - Cada reporte muestra: tipo, ID del contenido reportado, razón, botones de acción
   - Botones: "Eliminar", "Resolver", "Bloquear usuario"
   
3. **Acciones:**
   - Clic "Eliminar" → trianguloabre diálogo, confirmación borra el post/comentario de Firestore
   - Clic "Resolver" → marca reporte como RESOLVED
   - Clic "Bloquear" → bloquea usuario, ve arriba en punto 3
   
4. **En Firestore:**
   - `/reports/{reportId}` → status: OPEN | RESOLVED | DISMISSED
   - Si resuelto → fields resolvedBy (uid admin) y resolution (texto)

**Archivos clave:**
- `AdminModerateScreen.kt` (nueva, UI completa)
- `AdminViewModel.kt` (línea ~27: init + checkAdminStatus)
- `ReportRepository.kt` (línea ~75: resolveReport, dismissReport)

---

### 7. **Likes y Favoritos Persistidos**
**Archivos:**
- `CommunityViewModel.kt` (toggleLike, toggleFavorite con Firestore)

**Cómo verificar:**
1. **Dar Like:**
   - En Community feed → post → clic corazón vacío
   - Heart llena + contador sube
   - En Firestore: `/trips/{postId}/likes/{uid}` → documento existe
   
2. **Favorito:**
   - En Community feed → post → clic star/bookmark
   - Star rellena + contador sube
   - En Firestore: `/trips/{postId}/favorites/{uid}` → documento existe
   
3. **Persistencia:**
   - Cerrar app y reabrirla
   - Ir a Community → mismo post
   - Like/favorito sigue marcado (se obtiene de Firestore)

**Archivos clave modificados:**
- `CommunityViewModel.kt` (línea ~95: toggleLike con DB write, línea ~140: toggleFavorite)

---

### 8. **Feed de Comunidad**
**Archivos:**
- `CommunityViewModel.kt` (observeFollowingAndLoadFeed)
- `CommunityTabScreen.kt` (CommunityScreen composable)

**Cómo verificar:**
1. **Como usuario autenticado:**
   - Ir a **Community tab**
   - Ver posts de usuarios a quienes sigues
   - Posts con: destino, presupuesto, autor, imagen, likes, comentarios
   
2. **Optimizaciones:**
   - Feed carga con `.limit(50)` (máx 50 posts iniciales)
   - Los posts de usuarios bloqueados NO aparecen

**Archivos clave:**
- `CommunityViewModel.kt` (línea ~217: .limit(50) optimization)

---

### 9. **Admin Navigation desde Profile**
**Archivos:**
- `ProfileTabScreen.kt` (tarjeta "Panel Admin" visible si esAdmin)
- `MainShellScreen.kt` (paso parámetro onNavigateToAdmin)

**Cómo verificar:**
1. Ir a **Profile**
2. Si eres admin → ver tarjeta verde "Panel Admin - Gestionar reportes y usuarios"
3. Si no eres admin → no aparece la tarjeta
4. Clic en la tarjeta → navega a AdminModerateScreen

---

## Resumen Rápido de Todos los Tests

```
✓ Cambiar idioma a English → strings traducidos
✓ Register sin marcar +13 → error
✓ Register marcando +13 → funciona + isOver13Confirmed guardado
✓ Sin +13 → Community muestra "Acceso Restringido"
✓ Con +13 → Community muestra feed
✓ Community → corazón y star persisten en Firestore
✓ Profile → toggle dark mode → isDarkTheme guardado
✓ Comentar en publicación → comentario visible + Firestore
✓ Eliminar comentario propio → desaparece
✓ Profile (admin) → aparece "Panel Admin"
✓ Panel Admin → ver reportes + botones (eliminar, resolver, bloquear)
✓ Bloquear usuario → posts bloqueados desaparecen del feed
```

---

## Para Comprobar en Firestore Console

Ve a https://console.firebase.google.com/ y navega a tu proyecto:

```
/users/{uid}
  ├─ uid: string
  ├─ email: string
  ├─ displayName: string
  ├─ isOver13Confirmed: boolean ← TEST ESTO
  ├─ isDarkTheme: boolean ← TEST ESTO
  ├─ esAdmin: boolean
  └─ blockedByUserIds: array ← TEST ESTO

/trips/{tripId}
  ├─ status: "PUBLISHED"
  ├─ likes/{uid}: { timestamp } ← TEST ESTO
  ├─ favorites/{uid}: { timestamp } ← TEST ESTO
  ├─ comments/{commentId} ← TEST ESTO
  │  ├─ text, authorName, authorUid, timestamp
  │  └─ likesCount
  └─ ...

/reports/{reportId} ← TEST ESTO
  ├─ status: "OPEN" | "RESOLVED"
  ├─ reporterUid
  └─ ...
```

---

## Próximos Pasos

Después de verificar todo, puedes decirme:
- Qué quieres cambiar
- Qué no te gusta
- Qué quieres que sea diferente
- Y aplico los cambios
