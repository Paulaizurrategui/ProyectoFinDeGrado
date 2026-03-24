# Hito 1 — Inicio del proyecto (1ª entrega)

## 1. Definición del proyecto

### 1.1. Nombre del proyecto
**UrTriply**

### 1.2. Eslogan
**“Tu próxima aventura europea empieza aquí, sin salirte del presupuesto.”**

### 1.3. Descripción general
**UrTriply** es una aplicación Android (Kotlin + Jetpack Compose) orientada a la planificación de viajes desde un origen fijo (**Aeropuerto de Madrid – MAD**) hacia **capitales europeas**, generando una propuesta de viaje ajustada a:
- **presupuesto total**
- **número de viajeros**
- **rango de fechas aproximado**
- **preferencias del usuario** (cultura, ocio nocturno, naturaleza, gastronomía)

La app devuelve una **propuesta completa** con:
- distribución del presupuesto por categorías (transporte, alojamiento, comidas, actividades)
- recomendaciones de actividades reales según preferencias
- un itinerario por días (día 1, día 2, …)
- diferenciación entre planes gratuitos y de pago
- enlaces para compra/reserva (a través de proveedores externos)

Además, UrTriply incorpora una dimensión social: los usuarios registrados pueden **publicar sus viajes**, **comentar**, **dar like**, **guardar en favoritos** y **reportar contenido**, creando una comunidad de ejemplos y recomendaciones.

### 1.4. Objetivo principal (1 frase)
**Planificar viajes a capitales europeas desde Madrid basándose en el presupuesto total, número de viajeros y fechas aproximadas, ofreciendo propuestas realistas y ajustadas al dinero disponible.**

### 1.5. Público objetivo
- **Público general** (estudiantes, parejas, familias, mochileros, etc.)
- Personas con un perfil digital **básico**, pero que buscan funcionalidades completas (“que tenga chicha”) sin complicaciones.

### 1.6. Problemas que resuelve
UrTriply busca solucionar principalmente:
- “No sé cuánto cuesta viajar a X con mi presupuesto”
- “Quiero ideas y planes ajustados a mi dinero”
- “Quiero ejemplos reales de otros viajeros y poder inspirarme”
- “Quiero una planificación rápida sin tener que abrir mil webs (vuelos, actividades, etc.)”

### 1.7. Propuesta de valor / diferenciación
UrTriply se diferencia de otras soluciones por combinar:
- **Generación automática** de propuesta ajustada a presupuesto y nº de viajeros.
- **Búsqueda de mejor precio dentro de un rango de fechas** (el usuario no fija días exactos: define un rango y UrTriply optimiza).
- **Propuesta con estructura clara**: categorías de gasto + itinerario diario + lista de actividades.
- **Contenido “real” basado en APIs** (vuelos/alojamiento/actividades), y si no hay disponibilidad se usa un **fallback estimado** con aviso.
- **Comunidad integrada** (publicación, comentarios, likes, favoritos y reportes).
- **Experiencia simple (pocos pasos)** sin perder potencia: se prioriza una navegación clara y acciones directas.

### 1.8. Plataforma, tecnología e idioma
- **Plataforma**: Android
- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Backend/servicios**: Firebase (Auth + base de datos para comunidad y viajes guardados)
- **Idiomas de la app**: Español e Inglés (detección automática del idioma del sistema)
- **Compatibilidad**: Android **9 (API 28)** o superior
- **Orientación**: solo vertical (portrait)

### 1.9. Restricciones principales
- **Origen fijo**: Aeropuerto de Madrid (MAD)
- **Destinos**: capitales europeas (lista inicial definida en el MVP)
- **Moneda**: la app trabaja en **EUR**, convirtiendo otros precios (ej. GBP) mediante API de conversión.

---

## 2. Alcance inicial (MVP)

### 2.1. Lista inicial de destinos (10 capitales europeas)
Lista propuesta (muy conocidas y visitadas), incluyendo las imprescindibles indicadas:
1. París (Francia)
2. Londres (Reino Unido)
3. Roma (Italia)
4. Ámsterdam (Países Bajos)
5. Atenas (Grecia)
6. Lisboa (Portugal)
7. Berlín (Alemania)
8. Praga (República Checa)
9. Viena (Austria)
10. Dublín (Irlanda)

> Nota: esta lista es ampliable en hitos posteriores.

### 2.2. Entrada de datos del usuario (planificación)
Formulario base para generar propuesta:
- Presupuesto total (**EUR**)
- Número de viajeros
- **Rango de fechas aproximado** (ej. “del 10 al 15 de mayo”)
- Preferencias (multi-selección):
  - Cultura
  - Ocio nocturno
  - Naturaleza
  - Gastronomía

Decisiones de diseño:
- El presupuesto es **total** (para todo el viaje), no por persona.
- No se define “estilo de viaje” (económico/estándar/cómodo) en el MVP: se infiere por presupuesto.

### 2.3. Salida de la propuesta generada
La app generará:
- **Resumen del presupuesto** por categorías:
  - Transporte (vuelos desde MAD)
  - Alojamiento (precio por habitación)
  - Comidas
  - Actividades
- **Itinerario por días** (día 1, día 2… según días que permita el presupuesto)
- **Lista de actividades sugeridas** filtradas por preferencias
- Distinción:
  - actividades gratuitas
  - actividades de pago
- Enlaces a proveedores externos:
  - enlace a compra de vuelos
  - enlace a reserva o información del alojamiento
  - enlace a actividades (si aplica)

### 2.4. Datos reales mediante APIs (y fallback)
Objetivo: precios “como si el usuario los consultase en el proveedor”.

- **Vuelos**: búsqueda dentro del rango de fechas y selección del **precio más bajo**.
  - Salida: precio + enlace para compra.
- **Alojamiento**: consulta por destino y rango de fechas (precio por habitación).
- **Actividades**: actividades reales con precio y enlace, filtradas por preferencias.
- **Conversión de moneda**: API para convertir a EUR cuando sea necesario.

**Estrategia ante fallos (requisito clave):**
- Si una API falla o no devuelve datos suficientes:
  - se usa un **fallback estimado**
  - se informa al usuario con un aviso (transparencia)

### 2.5. Comunidad (funcionalidades del MVP)
Para usuarios registrados:
- Publicar viaje generado (sí)
- Publicar viaje manual (sí)
- Ver feed/lista de viajes de otros (sí)
- Comentar publicaciones (sí)
- Dar like (sí)
- Guardar en favoritos (sí)
- Reportar publicaciones/comentarios (sí)

Los viajes publicados solo serán visibles para **usuarios registrados**.

### 2.6. Modo sin cuenta
- Al abrir la app, se mostrará una pantalla inicial con:
  - **Entrar/Registrarse**
  - **Continuar sin cuenta**
- Sin cuenta, el usuario puede:
  - generar propuestas
- Sin cuenta, el usuario **no puede**:
  - guardar borradores
  - publicar
  - comentar
  - ver el feed de comunidad
  - dar like / favoritos
En cuanto intente realizar una acción social o de guardado, se pedirá login/registro.

### 2.7. Moderación (Admin simple)
Se definirá un usuario “admin” (por ejemplo, identificado por email) con permisos para:
- borrar publicaciones
- borrar comentarios
- bloquear usuarios
- gestionar/ocultar contenido reportado

---

## 3. Requisitos funcionales (RF)

> Nota: se redactan como “debe permitir / debe hacer”.

### 3.1. Autenticación y acceso
- **RF-01**: El sistema debe permitir registro de usuarios mediante email y contraseña.
- **RF-02**: El sistema debe permitir inicio de sesión con email y contraseña.
- **RF-03**: El sistema debe permitir recuperación de contraseña por correo.
- **RF-04**: El sistema debe permitir cerrar sesión.
- **RF-05**: El sistema debe permitir continuar sin cuenta para generar propuestas.
- **RF-06**: El sistema debe solicitar login/registro al intentar publicar, comentar, dar like, guardar favoritos o ver el feed.

### 3.2. Planificación de viaje
- **RF-07**: El sistema debe permitir seleccionar un destino (capital europea del catálogo).
- **RF-08**: El sistema debe permitir introducir presupuesto total en EUR.
- **RF-09**: El sistema debe permitir introducir número de viajeros.
- **RF-10**: El sistema debe permitir introducir un rango de fechas aproximado.
- **RF-11**: El sistema debe permitir seleccionar múltiples preferencias (cultura/ocio nocturno/naturaleza/gastronomía).
- **RF-12**: El sistema debe generar una propuesta de viaje con:
  - presupuesto por categorías
  - itinerario por días
  - lista de actividades sugeridas
  - separación de actividades gratis/de pago
- **RF-13**: El sistema debe estimar el número de días recomendados en función del presupuesto y costes obtenidos.
- **RF-14**: El sistema debe obtener precios reales de vuelos dentro del rango y mostrar el mejor precio encontrado.
- **RF-15**: El sistema debe mostrar un enlace externo para compra/reserva (WebView).
- **RF-16**: El sistema debe obtener datos reales de alojamiento en las fechas indicadas.
- **RF-17**: El sistema debe obtener actividades reales con precio y enlace, filtradas por preferencias.
- **RF-18**: Si una API falla, el sistema debe usar datos estimados de respaldo e informar al usuario.

### 3.3. Guardado y publicación
- **RF-19**: El sistema debe permitir a usuarios registrados guardar propuestas como **borrador**.
- **RF-20**: El sistema debe permitir editar un viaje generado antes de publicarlo.
- **RF-21**: El sistema debe permitir publicar un viaje (generado o manual).
- **RF-22**: El sistema debe permitir ver el detalle de un viaje publicado.

### 3.4. Comunidad
- **RF-23**: El sistema debe permitir ver un feed de publicaciones (solo usuarios registrados).
- **RF-24**: El sistema debe permitir comentar publicaciones.
- **RF-25**: El sistema debe permitir dar like a publicaciones.
- **RF-26**: El sistema debe permitir guardar publicaciones en favoritos.
- **RF-27**: El sistema debe permitir reportar publicaciones y comentarios.
- **RF-28**: El sistema debe permitir que un admin gestione reportes y oculte contenido.

### 3.5. Perfil y usuario
- **RF-29**: El sistema debe permitir a los usuarios tener un perfil con nombre visible y foto.
- **RF-30**: El sistema debe restringir el acceso a la comunidad a mayores de **13 años** (confirmación en registro o aviso).

---

## 4. Requisitos no funcionales (RNF)

- **RNF-01 (Rendimiento)**: La generación de una propuesta debe completarse idealmente en **< 10 segundos**.
- **RNF-02 (UX)**: La app debe mostrar un indicador de carga/progreso durante llamadas a APIs.
- **RNF-03 (Fiabilidad)**: Si una fuente externa falla, la app debe usar fallback estimado (cuando sea posible) y avisar.
- **RNF-04 (Accesibilidad)**: La app debe soportar modo oscuro, tamaños de letra y contraste adecuados.
- **RNF-05 (Seguridad)**: Autenticación mediante Firebase Auth. Acceso a datos restringido por reglas.
- **RNF-06 (Idiomas)**: La app debe detectar el idioma del sistema (ES/EN) y usar español como idioma por defecto si no se detecta.
- **RNF-07 (Compatibilidad)**: Android 9 (API 28) o superior. Orientación vertical.

---

## 5. Consideraciones legales y de uso de APIs
- UrTriply **no vende** billetes ni realiza reservas directamente.
- UrTriply muestra precios orientativos obtenidos mediante APIs y **redirige** al proveedor para completar la compra/reserva (en **WebView**).
- Se indicará que los precios pueden variar y dependen de disponibilidad del proveedor.

---

## 6. Esquema E/R (modelo conceptual)

### 6.1. Entidades principales
- **Usuario**
  - uid (PK)
  - email
  - nombre
  - fotoUrl
  - idioma
  - fechaRegistro
  - esAdmin (boolean) *(o admin por lista de emails en configuración)*
  - bloqueado (boolean)
- **Viaje**
  - idViaje (PK)
  - autorUid (FK → Usuario.uid)
  - destino
  - fechaInicioRango
  - fechaFinRango
  - numViajeros
  - presupuestoTotalEUR
  - propuestaGenerada (texto/JSON)
  - estado (BORRADOR / PUBLICADO)
  - fechaCreacion
  - fechaPublicacion (nullable)
- **Actividad**
  - idActividad (PK)
  - viajeId (FK → Viaje.idViaje)
  - nombre
  - tipo (cultura/naturaleza/gastro/ocio)
  - precioEUR
  - esGratis (boolean)
  - enlace
- **Comentario**
  - idComentario (PK)
  - viajeId (FK → Viaje.idViaje)
  - autorUid (FK → Usuario.uid)
  - texto
  - fecha
- **Like**
  - idLike (PK)
  - viajeId (FK → Viaje.idViaje)
  - uid (FK → Usuario.uid)
  - fecha
- **Favorito**
  - idFavorito (PK)
  - viajeId (FK → Viaje.idViaje)
  - uid (FK → Usuario.uid)
  - fecha
- **Reporte**
  - idReporte (PK)
  - tipoObjetivo (VIAJE / COMENTARIO)
  - objetivoId (idViaje o idComentario)
  - uidReporta (FK → Usuario.uid)
  - motivo
  - fecha
  - estado (ABIERTO / RESUELTO / DESCARTADO)
- **Bloqueo**
  - idBloqueo (PK)
  - adminUid (FK → Usuario.uid)
  - usuarioBloqueadoUid (FK → Usuario.uid)
  - motivo
  - fecha

### 6.2. Relaciones y cardinalidades (resumen)
- Usuario 1 — N Viaje (un usuario crea muchos viajes; un viaje tiene un autor)
- Viaje 1 — N Actividad
- Viaje 1 — N Comentario
- Usuario 1 — N Comentario
- Usuario N — M Viaje (Likes) *(resuelto con entidad Like)*
- Usuario N — M Viaje (Favoritos) *(resuelto con entidad Favorito)*
- Usuario 1 — N Reporte
- Admin (Usuario) 1 — N Bloqueo

---

## 7. Modelo relacional (tablas)

> Nota: aunque Firebase suele ser NoSQL, se entrega un modelo relacional equivalente para el hito.

- **USUARIO**(uid PK, email, nombre, fotoUrl, idioma, fechaRegistro, esAdmin, bloqueado)
- **VIAJE**(idViaje PK, autorUid FK→USUARIO.uid, destino, fechaInicioRango, fechaFinRango, numViajeros, presupuestoTotalEUR, propuestaGenerada, estado, fechaCreacion, fechaPublicacion)
- **ACTIVIDAD**(idActividad PK, viajeId FK→VIAJE.idViaje, nombre, tipo, precioEUR, esGratis, enlace)
- **COMENTARIO**(idComentario PK, viajeId FK→VIAJE.idViaje, autorUid FK→USUARIO.uid, texto, fecha)
- **LIKE**(idLike PK, viajeId FK→VIAJE.idViaje, uid FK→USUARIO.uid, fecha, UNIQUE(viajeId, uid))
- **FAVORITO**(idFavorito PK, viajeId FK→VIAJE.idViaje, uid FK→USUARIO.uid, fecha, UNIQUE(viajeId, uid))
- **REPORTE**(idReporte PK, tipoObjetivo, objetivoId, uidReporta FK→USUARIO.uid, motivo, fecha, estado)
- **BLOQUEO**(idBloqueo PK, adminUid FK→USUARIO.uid, usuarioBloqueadoUid FK→USUARIO.uid, motivo, fecha)

---

## 8. Mockups iniciales (bocetos funcionales)

> Los mockups describen la estructura y componentes principales (boceto de baja fidelidad).

### 8.1. Pantalla 0 — Bienvenida / Acceso
- Logo + eslogan
- Botones:
  - “Iniciar sesión”
  - “Crear cuenta”
  - “Continuar sin cuenta”
- Aviso: “Para publicar o ver la comunidad necesitarás iniciar sesión.”

### 8.2. Pantalla 1 — Login
- Email
- Contraseña
- “¿Olvidaste la contraseña?”
- Botón “Iniciar sesión”
- Enlace “Crear cuenta”
- AlertDialog para avisos/errores (en ES/EN según idioma)

### 8.3. Pantalla 2 — Registro
- Email
- Contraseña
- Repetir contraseña
- Checkbox/confirmación +13 (o aviso)
- Botón “Registrarse”

### 8.4. Pantalla 3 — Home (usuario registrado)
- Resumen: “Bienvenida”, email/nombre
- Botón principal: “Planificar viaje”
- Accesos:
  - “Mis borradores”
  - “Comunidad”
  - “Perfil”
- Botón “Cerrar sesión”

### 8.5. Pantalla 4 — Formulario “Planificar viaje”
- Selector destino (lista)
- Presupuesto total (EUR)
- Nº viajeros
- Rango de fechas (date picker rango)
- Preferencias (chips multi-selección)
- Botón “Generar propuesta”
- Loading/progreso

### 8.6. Pantalla 5 — Resultado de propuesta
- Resumen de presupuesto por categorías (gráfico o cards)
- Duración recomendada (nº días)
- Itinerario por días (expandible)
- Actividades recomendadas (con precios y enlaces)
- Aviso si se usó fallback estimado
- Botones:
  - “Guardar borrador” (requiere login)
  - “Publicar” (requiere login)
  - “Compartir” (opcional)

### 8.7. Pantalla 6 — Comunidad (feed)
- Lista de publicaciones (tarjetas):
  - destino, días, presupuesto, autor, fecha
  - likes, comentarios, botón favorito
- Filtros básicos (destino, presupuesto)
- Acceso al detalle

### 8.8. Pantalla 7 — Detalle publicación + comentarios
- Información del viaje publicado
- Itinerario/actividades
- Comentarios (lista)
- Añadir comentario
- Botones: like, favorito, reportar

### 8.9. Pantalla 8 — Perfil
- Foto + nombre editable
- Email visible
- Mis viajes publicados
- Mis favoritos
- Ajustes idioma (ES/EN)

### 8.10. Pantalla 9 — Admin (solo admin)
- Lista de reportes
- Acciones:
  - ocultar/borrar post
  - borrar comentario
  - bloquear usuario

---

## 9. Criterio de “MVP logrado”
Se considera el MVP completado cuando:
1. Un usuario puede **registrarse/iniciar sesión**.
2. Puede **generar** una propuesta de viaje para una capital europea usando presupuesto, viajeros y rango de fechas.
3. Puede **guardar en borradores**, editar y publicar el viaje.
4. Otro usuario registrado puede **ver el feed**, entrar al detalle, **comentar**, dar **like**, guardar en **favoritos** y **reportar**.
5. El admin puede gestionar reportes: **ocultar/borrar** y **bloquear**.

---
