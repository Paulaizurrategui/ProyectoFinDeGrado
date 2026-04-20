### Descripción
**UrTriply** es una app Android (Kotlin + Jetpack Compose) para planificar viajes desde el **Aeropuerto de Madrid (MAD)** a capitales europeas, generando una propuesta ajustada al **presupuesto total**, **número de viajeros**, **rango de fechas** y **preferencias** (cultura, ocio nocturno, naturaleza, gastronomía).

Incluye **modo invitado** para generar propuestas sin registrarse y una **comunidad** (solo para usuarios registrados) donde se pueden publicar viajes, comentar, dar like, guardar favoritos y reportar contenido.

### Funcionalidades principales (MVP)
- Autenticación con **Firebase Auth** (registro, login, recuperación de contraseña, logout)
- **Pantalla de bienvenida**: iniciar sesión / crear cuenta / continuar sin cuenta
- Planificador de viaje (en desarrollo):
  - destino (capital europea)
  - presupuesto total (EUR)
  - nº de viajeros
  - **rango de fechas**
  - preferencias (multi-selección)
- Generación de propuesta:
  - presupuesto por categorías (vuelos, alojamiento, comidas, actividades)
  - itinerario por días
  - actividades recomendadas (gratis / de pago)
  - enlaces a proveedores en **WebView**
- Comunidad (en desarrollo, requiere login):
  - publicar (viaje generado o manual)
  - comentarios, likes, favoritos, reportes
  - moderación admin (borrar/bloquear/gestionar reportes)

### Arquitectura / Tecnologías
- **Kotlin**
- **Jetpack Compose (Material 3)**
- **Firebase Authentication**
- (Previsto) **Firestore** para publicaciones, comentarios, favoritos y borradores
- Soporte multi-idioma: **ES/EN**
- Android mínimo: **API 28 (Android 9)**

### Cómo ejecutar el proyecto
1. Clona el repositorio.
2. Abre en **Android Studio**.
3. Configura Firebase:
   - Añade `google-services.json` en `app/`
   - Activa Email/Password en Firebase Authentication
4. Ejecuta la app en un emulador o dispositivo (Android 9+).

### Acceso y permisos (modo invitado)
- Sin cuenta se puede **generar** propuesta.
- Para **guardar borradores**, **publicar**, **comentar** o **ver la comunidad** se requiere iniciar sesión.

### Destinos iniciales (MVP)
París, Londres, Roma, Ámsterdam, Atenas, Lisboa, Berlín, Praga, Viena y Dublín.

### Aviso legal
UrTriply no vende billetes ni realiza reservas. Los precios se obtienen mediante APIs externas y se redirige al proveedor para finalizar la compra/reserva (WebView). Los precios pueden variar según disponibilidad.
