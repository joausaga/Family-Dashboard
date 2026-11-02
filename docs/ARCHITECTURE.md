# Arquitectura de Family Dashboard

Family Dashboard adapta la base Kotlin/Jetpack Compose de [Planify Dashboard](https://github.com/eusonlito/Planify-Dashboard) para Amazon Fire. El código y la identidad Android mantienen `com.lito.planify.dashboard` para conservar actualizaciones y OAuth.

## Componentes activos

Las rutas siguientes están bajo `android/app/src/main/java/com/lito/planify/dashboard/`:

| Componente | Responsabilidad |
|---|---|
| `MainActivity.kt` | Pantalla completa, escala geométrica/tipográfica, ciclo de vida, brillo y autorización Google |
| `DashboardViewModel.kt`, `SyncStatus.kt` | Estado, sincronización, filtros por cuenta, reloj y sondeo TV |
| `data/GoogleWorkspaceApi.kt` | REST de Google Calendar, Tasks y revocación OAuth |
| `data/repository/GoogleWorkspaceRepository.kt` | Paginación, conversión de eventos, recurrentes y tareas |
| `ContentRepository.kt`, `CalendarRepository.kt` en `data/repository/` | Contenido y semanas con caché, caducidad configurable y conservación ante fallos |
| `data/Api.kt`, `data/repository/WeatherRepository.kt` | Open-Meteo y geocodificación, sin autenticación Planify |
| `data/WeatherTimeZone.kt` | Zona de la ubicación meteorológica, validación, compatibilidad y hora TV en el mismo instante |
| `data/cache/` | JSON privado, escrituras atómicas y publicación de la última copia válida |
| `data/Store.kt`, `data/ReferenceClock.kt` | Preferencias compatibles y hora de referencia |
| `data/TvLocalApi.kt`, `data/repository/TvLocalUsageRepository.kt`, `data/TvUsage.kt` | Observaciones locales, historial persistente y agregados de TV |
| `ui/App.kt` | Navegación, indicador de conexión y regreso a Inicio tras 120 s sin interacción |
| `ui/TodayScreen.kt`, `ui/OverviewColors.kt` | Inicio de tres filas y paleta según hora local |
| `ui/CalendarScreen.kt` | Calendario semanal heredado, alimentado por Google |
| `ui/WeatherScreen.kt`, `ui/WeatherUi.kt` | Previsión y gráficos meteorológicos heredados |
| `ui/WeatherIcons.kt`, `ui/MeteoconFrames.kt` | Iconos Meteocons prerenderizados y reloj común de animación |
| `ui/TasksScreen.kt`, `ui/TaskUi.kt`, `data/TaskSections.kt` | Tareas, filtros y reglas compartidas con Inicio |
| `ui/TvUsageScreen.kt`, `ui/TvFormat.kt` | Pantalla TV y formato de duraciones |
| `ui/Settings*.kt` | Índice/detalle, controles, validación y esfera horaria Material 3 |
| `ui/NightScreen.kt`, `power/ScreenSchedule.kt` | Reloj nocturno y alarmas de pantalla |
| `admin/` | Quiosco opcional, botón de retorno desde Ajustes y servicio de apagado |

## Datos y autorización

- Play Services Identity Authorization API entrega el access token. Solo se solicitan `calendar.readonly` y `tasks.readonly`; el token no se persiste en preferencias ni hay secreto OAuth en el APK.
- Ante un 401, se solicita renovar la autorización conservando la caché. Cerrar sesión intenta revocar el token y limpia la caché Google, no el historial local de TV ni la ubicación meteorológica.
- Calendar enumera calendarios (incluidos ocultos), expande recurrencias con `singleEvents=true`, omite cancelados y convierte fechas a la zona del dispositivo. El contenido general consulta desde el primer día del mes de hace 12 meses hasta 366 días después de hoy; las sesiones de ese contenido se limitan a la semana actual. Las vistas también cargan semanas específicas.
- Tasks enumera listas y tareas por páginas y descarta completadas. Se conservan IDs opacos completos; los IDs enteros derivados siguen en la interfaz heredada.
- Selecciones por cuenta: ID del calendario principal como clave; nuevas fuentes visibles inicialmente, preferencias conocidas conservadas e IDs retirados reconciliados. Se permite desactivar todas las fuentes. Ocultar filtra la presentación, no revoca permisos ni evita toda descarga.
- Calendario/tareas: 5 minutos por defecto, configurable entre 5, 10, 20, 30, 40, 50 y 60. Clima: 30 minutos. TV: 1 minuto por defecto, configurable entre 1, 3, 5, 10, 15, 30, 45 y 60. Los bucles dependen del ciclo de vida activo.
- Se conservan 12 semanas descargadas recientemente. La sincronización manual refresca las semanas ya cargadas, contenido, clima y TV.

## Persistencia e invariantes

- `SharedPreferences("dashboard")`: ubicación, horario, brillo, última hora válida, selección por cuenta, estado de autorización y configuración TV.
- `files/cache`: claves existentes `content`, `week-<lunes>`, `forecast` y `tv_usage_history`. No se renombran en la limpieza.
- Los modelos `Event`, `Task`, `Person`, `Content`, `Week`, `Schedule` e historial TV mantienen los campos necesarios para leer datos existentes. `persons` aún tiene consumidores en Calendario, aunque Google no lo rellene.
- `sourceRowCounts` y `sourceOrders` del historial TV conservan metadatos importados anteriormente; el sondeo local los copia al persistir. No hay cliente activo de Sheets/Drive ni importador remoto.
- `Location` conserva opcionalmente su zona IANA; geocodificación y pronóstico leen `timezone`. La caché de pronóstico guarda las coordenadas solicitadas para no aplicar una respuesta antigua a otra ubicación. JSON antiguo sin estos campos sigue siendo válido; se utiliza la zona del dispositivo hasta conocer la zona meteorológica.
- Las preferencias antiguas de sesión y su limpieza se conservan por compatibilidad. Los access tokens activos solo viven en memoria.
- `ReferenceClock` conserva la última hora válida. Sin hora válida no se consultan rangos remotos y el reloj no presenta una hora falsa.

## Presentación y hardware

Geometría basada en 1920 × 1200, escala hasta 1 según lados largo/corto. En 1280 × 800 resulta 0,667; el texto usa `fontScale` mínimo de 1,35 cuando se reduce la geometría. La Fire emplea banderas inmersivas legacy; las versiones recientes usan controladores de insets.

La identidad visual usa crema/azul y tokens de `ui/theme/Theme.kt`. Geist es la tipografía principal; Geist Mono sigue en Tiempo y el reloj nocturno. La cabecera de Inicio interpola colores según hora local y selecciona contenido por contraste, incluyendo celdas con blanco al 20%.

El modo nocturno reduce el brillo de ventana a `1/255`. Las alarmas conservan alternativas para API 22. La instalación documentada no es Device Owner; las políticas de quiosco solo actúan si se aprovisionan por separado. El botón de retorno y el servicio opcional de apagado se conservan por sus referencias activas.

## TV

Consulta GET de solo lectura a `http://<IP>:8001/api/v2/` en la LAN. Respuesta válida confirma el estado según el contrato Samsung; error o timeout registra OFF inferido. Observaciones repetidas no duplican transiciones. La primera observación local no atribuye tiempo anterior desconocido y conserva intervalos importados.

Los totales se reparten por fronteras de hora/día/mes en la zona de la ubicación meteorológica, sin asumir días de 24 horas. Se prefiere la zona del pronóstico vinculado a esas coordenadas, después la guardada para la ubicación y finalmente la del dispositivo. Al cambiar ubicación/zona, reloj o historial, el ViewModel recalcula sin reescribir transiciones ni intervalos.

`UiState.tvTime` conserva zona e instante como `ZonedDateTime`; Inicio usa su fecha para tendencia/promedio y la pantalla TV lo usa para «hoy», meses, comparaciones, hora actual y antigüedad de consulta. No se reinterpreta esa hora como si fuera la del dispositivo. El reloj general y Google Calendar conservan su zona anterior. Detalle en [zona horaria de TV](../SPECS/tv-weather-timezone/SPEC.md).

Estado antiguo tras 6 horas sin observación se considera desactualizado. Inicio resume el apagado como «Apagada»; la pantalla TV añade el aviso de inferencia cuando corresponde.

## Pruebas

```sh
cd android
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :app:testDebugUnitTest :app:assembleRelease
```

Las pruebas de caché y calendario usan APIs Google falsas, sin servidor Planify. Se cubren paginación, recurrencias, selección por cuenta, vencimientos, horarios, intervalos TV, restauración del historial importado y contraste de la cabecera durante los 1440 minutos del día. El inventario de validación vigente está en [TASKS.md](../SPECS/current-version/TASKS.md).
