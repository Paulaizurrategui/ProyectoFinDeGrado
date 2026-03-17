# Hito 1 — Requisitos

## Requisitos funcionales (RF)
- RF-01 Registrar usuario (email/contraseña)
- RF-02 Iniciar sesión / cerrar sesión
- RF-03 Seleccionar destino
- RF-04 Introducir presupuesto
- RF-05 Introducir días o fechas (recomendado)
- RF-06 Generar propuesta de viaje (desglose + actividades)
- RF-07 Guardar propuesta de viaje
- RF-08 Publicar viaje guardado
- RF-09 Ver feed de publicaciones
- RF-10 Ver detalle de publicación
- RF-11 Comentar publicación

## Requisitos no funcionales (RNF)
- RNF-01 Usabilidad: navegación clara, pantallas principales accesibles rápidamente
- RNF-02 Rendimiento: generación en < 2 segundos con datos locales
- RNF-03 Persistencia: viajes guardados se mantienen al cerrar la app
- RNF-04 Seguridad: autenticación gestionada por proveedor (p.ej. Firebase Auth)
- RNF-05 Mantenibilidad: arquitectura MVVM y separación por capas
- RNF-06 Offline: el planificador funciona sin conexión con datos locales