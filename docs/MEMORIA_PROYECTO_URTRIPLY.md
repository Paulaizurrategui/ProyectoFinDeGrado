# Memoria del Proyecto: UrTriply

**Alumno:** [Paula Izurrategui López]  
**Tutor:** [Macarena Cuenca]  
**Curso:** [2 DAM]  
**Centro:** [Gregorio Fernandez]  
**Fecha:** [Fecha de entrega]  

---

## Indice

1. Fundamentacion  
2. Destinatarios  
3. Objetivos  
4. Metodologia  
5. Temporalizacion  
6. Recursos  
7. Conclusiones  
8. Bibliografia y webgrafia  

---

## 1. Fundamentacion

### 1.1. Descripcion del proyecto

UrTriply es una aplicacion Android desarrollada en Kotlin y Jetpack Compose orientada a la planificacion de viajes desde un origen fijo, el Aeropuerto de Madrid (MAD), hacia capitales europeas. La aplicacion permite al usuario introducir un presupuesto total, el numero de viajeros, un rango de fechas y unas preferencias de viaje para generar una propuesta adaptada a su situacion real.

El resultado de la generacion del viaje no se limita a mostrar una simple recomendacion de destino. UrTriply ofrece una propuesta estructurada con distribucion del presupuesto por categorias, itinerario por dias, actividades gratuitas y de pago, y acceso a informacion externa mediante enlaces de reserva o compra. Ademas, la aplicacion incorpora un enfoque progresivo: cuando una fuente externa no responde o no devuelve datos suficientes, el sistema utiliza un fallback estimado para mantener la utilidad de la experiencia y evitar que la app deje de funcionar.

La aplicacion tambien contempla una dimension social, ya que los usuarios registrados podran guardar borradores, publicar viajes, comentar publicaciones, dar likes, guardar favoritos y reportar contenido, creando una comunidad de ejemplos de viaje y recomendaciones entre usuarios.

### 1.2. Justificacion de su necesidad o interes

El proyecto surge para dar respuesta a una necesidad muy habitual: muchas personas quieren viajar, pero no siempre saben si un destino encaja con su presupuesto, cuanto puede costar realmente el alojamiento o que actividades pueden realizar segun sus intereses. Esta incertidumbre hace que planificar un viaje pueda convertirse en un proceso largo, fragmentado y poco intuitivo, obligando al usuario a consultar varias paginas y servicios distintos.

UrTriply intenta resolver ese problema centralizando la informacion mas relevante en una sola aplicacion. En lugar de obligar al usuario a comparar manualmente vuelos, alojamientos, actividades y presupuesto, la aplicacion propone una experiencia mas guiada, rapida y clara. El usuario introduce unos pocos datos basicos y la app genera una propuesta de viaje coherente con ellos.

Ademas del valor practico, el proyecto tiene interes tecnico y academico porque permite aplicar conceptos importantes del desarrollo de software moderno: arquitectura por capas, consumo de APIs reales, manejo de errores, uso de interfaces declarativas con Compose, integracion con Firebase y organizacion modular del codigo. Por tanto, no solo es un proyecto util para el usuario final, sino tambien una solucion que demuestra aprendizaje tecnico y capacidad de evolucion futura.

---

## 2. Destinatarios

### 2.1. A quien va dirigido el proyecto

UrTriply va dirigido al publico general que desea planificar viajes de forma sencilla y rapida, especialmente a personas que buscan una herramienta clara para organizar escapadas a capitales europeas desde Madrid. El perfil de usuario no requiere conocimientos tecnicos avanzados, por lo que la interfaz debe ser intuitiva y accesible.

Los destinatarios principales pueden agruparse en varios perfiles:

- Estudiantes que viajan con presupuestos ajustados.
- Parejas que buscan una escapada corta y economica.
- Familias que necesitan organizar el gasto total de un viaje.
- Mochileros o viajeros ocasionales que desean una propuesta rapida.
- Personas con experiencia digital basica que quieren evitar procesos complicados.

### 2.2. Necesidades que el proyecto pretende cubrir

El proyecto se diseña para cubrir necesidades muy concretas:

- Saber si un destino entra dentro del presupuesto disponible.
- Obtener una propuesta de viaje adaptada al numero de viajeros.
- Planificar un viaje sin tener que abrir multiples paginas web.
- Encontrar ideas de actividades segun las preferencias personales.
- Disponer de una experiencia simple, directa y rapida.
- Tener una respuesta util incluso cuando algun servicio externo falle.

### 2.3. Contexto de uso

La aplicacion se puede usar tanto en una fase de inspiracion, cuando el usuario todavia no ha decidido completamente su viaje, como en una fase de decision, cuando ya quiere comprobar si un destino concreto es viable. Tambien puede usarse en modo invitado, lo que reduce la barrera de entrada y permite probar la funcionalidad principal sin necesidad de registrarse desde el principio.

---

## 3. Objetivos

### 3.1. Objetivo general

Desarrollar una aplicacion Android que permita planificar viajes a capitales europeas desde Madrid basandose en presupuesto, numero de viajeros, rango de fechas y preferencias del usuario, generando propuestas realistas, utiles y adaptadas al dinero disponible.

### 3.2. Objetivos especificos

1. Permitir la seleccion de un destino dentro de un catalogo inicial de capitales europeas.
2. Permitir al usuario introducir un presupuesto total en euros.
3. Permitir indicar el numero de viajeros.
4. Permitir definir un rango de fechas de viaje.
5. Permitir seleccionar varias preferencias de viaje mediante multi-seleccion.
6. Generar una propuesta con distribucion del presupuesto por categorias.
7. Generar un itinerario por dias ajustado al presupuesto y a la duracion estimada.
8. Diferenciar entre actividades gratuitas y actividades de pago.
9. Obtener datos reales de localizacion mediante geocodificacion.
10. Integrar alojamiento real mediante APIs basadas en coordenadas.
11. Mostrar enlaces externos para que el usuario pueda continuar la compra o reserva.
12. Incluir una estrategia de fallback para mantener la utilidad de la app si una API falla.
13. Crear una base escalable para futuras funciones de comunidad y contenido social.
14. Desarrollar una interfaz moderna, clara y adaptada a dispositivos Android.

### 3.3. Criterio de exito del proyecto

El proyecto se considerara correctamente orientado cuando el usuario pueda abrir la aplicacion, introducir los datos basicos de un viaje y obtener una propuesta comprensible, util y visualmente clara. Se considerara aun mas completo si la aplicacion puede apoyarse en APIs reales para mostrar informacion de destino, alojamiento y actividades, manteniendo siempre una experiencia estable aunque algun servicio externo no responda.

---

## 4. Metodologia

### 4.1. Enfoque de desarrollo

El desarrollo de UrTriply se ha planteado de forma incremental y por fases. Este enfoque permite validar cada bloque funcional antes de avanzar al siguiente y reduce el riesgo de integrar demasiadas cosas a la vez. La metodologia seguida se puede resumir asi:

1. Analisis de requisitos y definicion del alcance.
2. Diseño de la arquitectura y de las pantallas principales.
3. Desarrollo de la interfaz de usuario.
4. Implementacion de la logica de generacion de propuestas.
5. Integracion de servicios externos mediante APIs.
6. Pruebas, correcciones y mejora del comportamiento.
7. Redaccion de la memoria y preparacion de la entrega final.

### 4.2. Tecnologias utilizadas

#### Lenguaje y plataforma
- Kotlin como lenguaje principal.
- Android como plataforma de destino.
- Compatibilidad orientada a Android 9 (API 28) o superior.

#### Interfaz de usuario
- Jetpack Compose como framework de UI declarativa.
- Material 3 para el sistema visual y componentes.
- Uso de pantallas y componentes reutilizables.

#### Arquitectura
- Patrón MVVM como base de organizacion.
- Separacion de capas para diferenciar UI, dominio y datos.
- Repositorios para encapsular el acceso a APIs.

#### Consumo de APIs y red
- Retrofit para las peticiones HTTP.
- OkHttp para configuracion de cliente y logs.
- Moshi para parseo de JSON.
- Nominatim / OpenStreetMap para geocodificacion.
- Overpass API para obtener informacion de alojamiento.
- Firebase Auth y Firestore como base para autenticacion y comunidad.

### 4.3. Aspectos tecnicos relevantes

Uno de los aspectos mas importantes del proyecto es el uso de datos reales combinados con fallback. Esto implica que la aplicacion no depende por completo de que una API responda siempre correctamente. Si un servicio falla, el sistema sigue ofreciendo una propuesta estimada y avisa al usuario de que la informacion no es totalmente real. Este comportamiento mejora la robustez y la experiencia de uso.

Otro aspecto tecnico destacable es la generacion de propuestas a partir de datos introducidos por el usuario. El sistema valida presupuesto, viajeros, fechas y preferencias antes de construir una propuesta. A partir de ahi se calcula la distribucion del presupuesto, el numero de dias recomendados y la lista de actividades sugeridas.

Tambien es relevante la integracion progresiva de geocodificacion y alojamiento. Primero se convierte el texto del destino en datos geograficos reales, y despues se usan esas coordenadas para buscar alojamientos cercanos. Esta progresion tecnica permite que en fases posteriores puedan conectarse otras fuentes, como vuelos, actividades y mapas.

### 4.4. Consideraciones legales y de uso de APIs

UrTriply no vende billetes ni realiza reservas directamente. La aplicacion muestra informacion orientativa y redirige al usuario a proveedores externos para completar la compra o reserva. Esto reduce la responsabilidad operativa de la aplicacion y la alinea con un modelo de agregacion de informacion.

En todo momento se debe indicar que los precios pueden variar segun disponibilidad, fechas y condiciones del proveedor. Ademas, cuando se usen servicios externos, deben respetarse sus politicas de uso, limites de peticiones y condiciones legales.

### 4.5. Planificacion y control

Para organizar el trabajo se recomienda el uso de una herramienta de gestion como Trello o Jira. En este proyecto resulta util dividir las tareas por fases: analisis, diseno, desarrollo de interfaz, integracion de APIs, pruebas y documentacion. Este metodo facilita el seguimiento del progreso y la estimacion de tiempos.

---

## 5. Temporalizacion

### 5.1. Actividades y fases del proyecto

La temporalizacion puede organizarse en fases para reflejar el trabajo real realizado y el previsto. A continuacion se propone una distribucion orientativa de horas:

| Fase | Actividades principales | Horas aproximadas |
|---|---|---:|
| Analisis inicial | Estudio de requisitos, alcance, investigacion de referencias |  h |
| Diseno funcional | Estructura de pantallas, flujo de usuario, bocetos y modelo inicial |  h |
| Configuracion tecnica | Creacion del proyecto, dependencias, arquitectura base |  h |
| Interfaz principal | Pantallas de bienvenida, login, registro, home y planificacion |  h |
| Generacion de propuestas | Lógica de formulario, validaciones y calculo de presupuesto |  h |
| Geocodificacion | Integracion de Nominatim y almacenamiento de coordenadas reales |  h |
| Alojamiento | Integracion de Overpass y tarjetas de alojamiento recomendado |  h |
| Comunidad | Estructura base de borradores, publicaciones, likes y comentarios |  h |
| Pruebas y correcciones | Ajustes de errores, validaciones y mejoras de estabilidad |  h |
| Documentacion | Redaccion de memoria, referencias y preparacion de entrega |  h |

**Total estimado:**  horas

### 5.2. Distribucion temporal orientativa

Organizado por semanas:

- Semana 1: analisis y diseno.
- Semana 2: configuracion tecnica y estructura de pantallas.
- Semana 3: formulario de planificacion y generacion local.
- Semana 4: geocodificacion real.
- Semana 5: alojamiento y mejoras de salida.
- Semana 6: comunidad y almacenamiento.
- Semana 7: pruebas y correcciones.
- Semana 8: memoria y presentacion final.

### 5.3. Seguimiento del trabajo

Para documentar la evolucion del proyecto es recomendable mantener un registro de tareas completadas, incidencias y decisiones tecnicas. Una tabla de seguimiento o un tablero Kanban ayuda a visualizar el avance y justificar el tiempo invertido en cada parte del desarrollo.

---

## 6. Recursos

### 6.1. Recursos humanos

- Un alumno desarrollador principal.
- Tutor o profesor responsable del seguimiento academico.
- Posible apoyo de documentacion tecnica y revision de herramientas de IA.

### 6.2. Recursos espaciales

- Aula o entorno de trabajo con ordenador.
- Acceso a red e Internet.
- Espacio para pruebas en emulador o dispositivo fisico Android.

### 6.3. Recursos materiales y tecnologicos

- Ordenador personal o de trabajo.
- Android Studio.
- SDK de Android.
- Dispositivo Android o emulador.
- Kotlin.
- Jetpack Compose.
- Firebase.
- Retrofit.
- OkHttp.
- Moshi.
- Nominatim API.
- Overpass API.
- Git y GitHub.
- Editor de texto o procesador de documentos para la memoria final.

### 6.4. Recursos documentales

- Documentacion oficial de Android Developers.
- Documentacion de Kotlin.
- Documentacion de Jetpack Compose y Material 3.
- Documentacion de Firebase.
- Documentacion de Retrofit, OkHttp y Moshi.
- Documentacion de Nominatim y Overpass.

---

## 7. Conclusiones

### 7.1. Conclusiones extraidas

El desarrollo de UrTriply ha permitido poner en practica competencias tecnicas muy relevantes dentro del desarrollo de software movil. Entre ellas destacan la organizacion del codigo por capas, la construccion de interfaces modernas con Compose, la validacion de formularios, el uso de APIs reales y el tratamiento de errores de forma controlada.

Uno de los aprendizajes mas importantes ha sido comprobar que una aplicacion real no debe depender de que todos los servicios externos funcionen siempre. Por ello, la inclusion de un sistema fallback ha sido clave para mantener la usabilidad incluso cuando una API no devuelve informacion completa. Esta idea aporta robustez al proyecto y lo hace mas realista.

Tambien ha sido especialmente util trabajar con datos concretos como coordenadas, destinos y alojamiento, ya que eso facilita una futura ampliacion hacia hoteles, actividades, mapas y precios mas precisos. El proyecto deja preparada una base tecnica muy solida para evolucionar sin tener que rehacer la estructura principal.

### 7.2. Lineas de trabajo futuras

Como mejoras futuras se proponen las siguientes lineas:

- Integracion de una API de vuelos para mostrar precios reales desde Madrid.
- Integracion de una API de actividades con enlaces y precios.
- Conversion de moneda automatica cuando sea necesario.
- Mapa interactivo con marcadores de destino y alojamiento.
- Sistema de comunidad completo con likes, favoritos, comentarios y reportes.
- Mejoras de accesibilidad, internacionalizacion y personalizacion.
- Filtros mas avanzados para el feed y para las propuestas de viaje.

### 7.3. Valor final del proyecto

UrTriply no solo resuelve una necesidad practica, sino que ademas demuestra una evolucion tecnica clara. Es una base realista sobre la que se puede construir una aplicacion completa de planificacion de viajes. La idea principal de convertir una necesidad cotidiana en una solucion funcional, limpia y escalable se ha alcanzado de forma satisfactoria.

---

## 8. Bibliografia y webgrafia

### 8.1. Documentacion tecnica

- Android Developers. Documentacion oficial de Android.
- Kotlin Documentation. https://kotlinlang.org/docs/home.html
- Jetpack Compose Documentation. https://developer.android.com/jetpack/compose
- Material Design 3. https://m3.material.io/
- Firebase Documentation. https://firebase.google.com/docs
- Retrofit Documentation. https://square.github.io/retrofit/
- OkHttp Documentation. https://square.github.io/okhttp/
- Moshi Documentation. https://github.com/square/moshi

### 8.2. APIs utilizadas

- Nominatim / OpenStreetMap. https://nominatim.openstreetmap.org/
- Overpass API. https://overpass-api.de/

### 8.3. Herramientas de desarrollo

- Android Studio.
- Git.
- GitHub.
- Emulador de Android.

### 8.4. Referencias complementarias

- Guías oficiales de buenas practicas para desarrollo Android.
- Material de clase y apuntes del modulo.
- Documentacion y ejemplos de integracion de APIs REST.

---

## Anexos opcionales

Anexos con:

- Capturas de pantalla de la aplicacion.
- Diagramas de flujo.
- Bocetos iniciales de interfaz.
- Modelo E/R.
- Tabla de planificacion detallada.
- Registro de incidencias y soluciones tecnicas.

---


