# Diseño vigente — Family Dashboard

## Procedencia y organización

Adaptación de Planify Dashboard para Amazon Fire. Se conserva el paquete Java/Kotlin y Android `com.lito.planify.dashboard`; el nombre visible es Family Dashboard. El [documento de arquitectura](../../docs/ARCHITECTURE.md) detalla componentes, fuentes y persistencia; [SPEC.md](SPEC.md) contiene los requisitos consolidados.

## Decisiones que se conservan

| Decisión | Implementación / motivo |
|---|---|
| Datos Google sin backend Planify | REST pequeño con Retrofit y autorización Play Services; lectura únicamente |
| Modelo interno compatible | `Event`, `Task`, `Content`, `Week` conservan formato de caché; no es una migración de esquema |
| Selecciones separadas por cuenta | ID del calendario principal y IDs Google completos; nuevas fuentes visibles, preferencias conocidas conservadas |
| Repositorios con última copia válida | `DiskCache`, `CachedResource`, `ContentRepository`, `CalendarRepository`, `WeatherRepository` |
| Caducidad configurable | `CalendarTaskRefreshInterval`; TV y clima conservan sus propios intervalos |
| Inicio de consulta | Cinco días con eventos/recurrentes integrados, Tareas protagonista y tendencia TV |
| Recurrentes verdes, otros del calendario | Barra y tinte al 16%; textos oscuros y cápsula de cuenta regresiva con contraste adaptativo |
| Paleta por hora local | `OverviewColors.kt`: mañana `#91C8FA → #DEEEFF`, tarde `#354F7E → #526895`, noche `#182542 → #30385F`; transición RGB lineal y contenido casi negro/blanco |
| Contraste durante transición | Se reduce la amplitud del degradado si sus extremos no permiten 4,5:1; celdas de previsión eligen su contenido sobre blanco al 20% |
| Tareas con reglas compartidas | `TaskUi.kt` y `TaskSections.kt`; orden, filtros, vencimientos e iconos comunes a Inicio/Tareas |
| TV local e historial independiente de Google | `TvLocalUsageRepository`; conserva registros importados y campos de metadatos aunque se retire el importador |
| Zona TV de la ubicación del tiempo | `WeatherTimeZone.kt`, campos opcionales de zona/coordenadas, recálculo al cambiar ubicación y `ZonedDateTime` compartido para agregados/presentación; fallback al dispositivo, sin migrar registros |
| Apagado TV inferido | Modelo conserva certeza; Inicio resume «Apagada» y pantalla TV explica falta de respuesta |
| Ajustes índice/detalle | `SettingsScreen`, `SettingsSections`, `SettingsComponents`, `SettingsLogic`, `SettingsTimePicker` |
| Diálogos dentro de Compose | Conservan densidad propia de la app; esfera Material vertical 1,7× y formato 12 h con AM/PM |
| Misma identidad de instalación | `applicationId`, namespace, firma, versión, claves y preferencias sin cambios |
| Conservar lo tangencialmente usado | `Person`/`PersonInitials`, Geist Mono, código de quiosco, servicio de apagado, campos heredados y funciones con pruebas permanecen |

## Limpieza autorizada

Se eliminan `FlipClock`, `RollingText`, utilidades visuales sin llamadas, las rutas `user/auth`, `event`, `task` del backend Planify y el stub de login por contraseña. `Api.kt` queda dedicado a Open-Meteo. Los repositorios de contenido/calendario reciben únicamente `GoogleWorkspaceRepository`; las pruebas de caché usan Google simulado.

Se retiran cliente/modelos Drive y Sheets y `TvUsageRepository` remoto, junto con su prueba exclusiva. No se retiran `TvUsageHistory`, transiciones, intervalos, metadatos, claves de disco ni lógica de compatibilidad de `TvLocalUsageRepository`. Una prueba específica verifica historial importado, reinicio y sondeo local.

Los instaladores, demo y documentación específica del XORO ya no son instrucciones del proyecto. Se conserva `scripts/meteocons.py`, sus recursos/licencias y toda configuración privada ignorada por Git. Las especificaciones de diseños intermedios se consolidan en esta carpeta sin alterar el historial Git existente.

## Verificación y recuperación

Antes de publicar o instalar: pruebas unitarias, release, comprobación del nombre/paquete/certificado, enlaces locales y ausencia de símbolos retirados. La instalación de referencia se actualiza con `adb install -r`, nunca mediante desinstalación. No se escribe en datos privados de la app ni se fuerza una nueva autorización para la prueba de nombre.

Una recuperación requiere una APK anterior compatible y de la misma firma; no se promete downgrade ni se usan resets de Git para descartar trabajo previo. La verificación manual a distancia y el Branding externo de OAuth se registran aparte; no se marcan resueltos por compilar.
