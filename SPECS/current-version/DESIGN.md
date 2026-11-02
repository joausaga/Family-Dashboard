# Diseño vigente — Family Dashboard

## Procedencia y organización

Adaptación de Planify Dashboard para Amazon Fire. El paquete Java/Kotlin y Android nuevo es `com.joausaga.familydashboard`; el nombre visible sigue siendo Family Dashboard. La app anterior `com.lito.planify.dashboard` se desinstaló tras validar la migración; se conservan su APK, firma, cliente OAuth y copias como recuperación. El [documento de arquitectura](../../docs/ARCHITECTURE.md) detalla componentes, fuentes y persistencia; [SPEC.md](SPEC.md) contiene los requisitos consolidados.

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
| Contraste durante transición | Se reduce la amplitud del degradado si sus extremos no permiten 4,5:1; después de resolver ese fondo, las cinco previsiones fuerzan blanco entre 12:00 y 21:00 con una superficie azul oscura de opacidad ajustada hasta alcanzar ≥4,5:1 en ambos extremos. Fuera del rango mantienen contenido adaptativo sobre blanco al 20%. La excepción no participa en la elección del degradado, para conservar reloj, fecha y clima actual |
| Tareas con reglas compartidas | `TaskUi.kt` y `TaskSections.kt`; orden, filtros, vencimientos e iconos comunes a Inicio/Tareas |
| TV local e historial independiente de Google | `TvLocalUsageRepository`; conserva registros importados y campos de metadatos aunque se retire el importador |
| Zona TV de la ubicación del tiempo | `WeatherTimeZone.kt`, campos opcionales de zona/coordenadas, recálculo al cambiar ubicación y `ZonedDateTime` compartido para agregados/presentación; fallback al dispositivo, sin migrar registros |
| Apagado TV inferido | Modelo conserva certeza; Inicio resume «Apagada» y pantalla TV explica falta de respuesta |
| Ajustes índice/detalle | `SettingsScreen`, `SettingsSections`, `SettingsComponents`, `SettingsLogic`, `SettingsTimePicker` |
| Diálogos dentro de Compose | Conservan densidad propia de la app; esfera Material vertical 1,7× y formato 12 h con AM/PM |
| Identidad propia autorizada | Nuevo applicationId/namespace/paquetes y firma, instalación independiente; preferencias/modelos y versión conservados, migración SAF con antigua app intacta según [android-identity](../android-identity/DESIGN.md) |
| Copia portable y restauración selectiva | `data/backup/`, SAF y `SettingsBackup`; lista blanca sin credenciales, verificación SHA-256/relectura y diario privado de rollback; primera etapa sin cambiar identidad |
| Conservar lo tangencialmente usado | `Person`/`PersonInitials`, Geist Mono, código de quiosco, servicio de apagado, campos heredados y funciones con pruebas permanecen |

## Limpieza autorizada

Se eliminan `FlipClock`, `RollingText`, utilidades visuales sin llamadas, las rutas `user/auth`, `event`, `task` del backend Planify y el stub de login por contraseña. `Api.kt` queda dedicado a Open-Meteo. Los repositorios de contenido/calendario reciben únicamente `GoogleWorkspaceRepository`; las pruebas de caché usan Google simulado.

Se retiran cliente/modelos Drive y Sheets y `TvUsageRepository` remoto, junto con su prueba exclusiva. No se retiran `TvUsageHistory`, transiciones, intervalos, metadatos, claves de disco ni lógica de compatibilidad de `TvLocalUsageRepository`. Una prueba específica verifica historial importado, reinicio y sondeo local.

Los instaladores, demo y documentación específica del XORO ya no son instrucciones del proyecto. Se conserva `scripts/meteocons.py`, sus recursos/licencias y toda configuración privada ignorada por Git. Las especificaciones de diseños intermedios se consolidan en esta carpeta sin alterar el historial Git existente.

## Verificación y recuperación

Antes de publicar o instalar: pruebas unitarias, release, comprobación del nombre/paquete/certificado, enlaces locales y ausencia de símbolos retirados. La migración inicial instala la nueva app en paralelo y requiere nueva autorización Google y restauración explícita; no escribe en los datos privados de la anterior. Después, las actualizaciones de la nueva identidad usan `adb install -r` con su misma firma, nunca desinstalación.

Durante la migración se conservan app, APK, clave y cliente antiguos como recuperación; la nueva app no hereda su autorización ni almacenamiento. Para recuperar actualizaciones de una identidad se necesita una APK compatible y de su misma firma; no se promete downgrade ni se usan resets de Git. La validación real de OAuth/restauración y la verificación manual a distancia se registran aparte; no se marcan resueltas por compilar.
