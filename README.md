# Family Dashboard

Panel familiar Android **adaptado de [Planify Dashboard](https://github.com/eusonlito/Planify-Dashboard) para Amazon Fire**. La instalación de referencia es una Fire HD 8 de 7.ª generación, Fire OS basado en Android 5.1.1/API 22, en horizontal a 1280 × 800.

Conserva la base nativa Kotlin/Jetpack Compose, Open-Meteo, el calendario semanal, el reloj nocturno y la infraestructura de caché del proyecto original. Sustituye Planify-Backend por **Google Calendar y Google Tasks**, rediseña Inicio, Tareas y Ajustes e incorpora monitorización local de una TV Samsung. **No necesita Planify-APP ni Planify-Backend.**

## Funciones actuales

- **Inicio:** hora, fecha y clima; fondo celeste matinal que oscurece según la hora local; siguiente actividad; agenda de hoy y cuatro días más; próximas tareas y resumen de TV.
- **Calendario:** dos semanas navegables, eventos y ocurrencias recurrentes de Google Calendar, con previsión meteorológica.
- **Tiempo:** previsión por horas y hasta 14 días, gráficos de temperatura y lluvia e iconos Meteocons.
- **Tareas:** resumen, línea temporal de 14 días, vistas por fecha/lista, filtros y detalles expandibles.
- **TV:** sondeo de la API local Samsung/Tizen, sesiones, uso horario, mapa de calor y comparaciones por periodo.
- **Ajustes:** calendarios y listas visibles, horario con reloj de 12 horas y AM/PM, brillo, ubicación meteorológica, frecuencias de actualización, prueba de conexión de TV y cuenta Google.
- **Reloj nocturno:** hora, fecha y clima sobre negro, con brillo mínimo. Tocar permite consultar el panel temporalmente.

Google se consulta con permisos de **solo lectura**: no se crean, editan ni completan eventos o tareas. La interfaz táctil permite navegar, filtrar y configurar; la agenda de Inicio no edita datos. La acción «Configurar TV» abre Ajustes.

![Inicio en la Fire HD 8](docs/images/today.png)

## Compilar y actualizar

Requiere JDK 17 o superior, Android SDK con plataforma 37 y Google Play Services operativo en la Fire para autorizar Calendar/Tasks.

```sh
cd android
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :app:testDebugUnitTest :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

La release utiliza la firma de producción existente, configurada fuera del repositorio. **No cambiar la firma ni el paquete `com.lito.planify.dashboard`**: son parte de la identidad de instalación y de la autorización OAuth. No instalar una APK debug encima de producción ni desinstalar para resolver un conflicto de firmas: se perderían datos locales.

Consulta [Instalación en Amazon Fire](docs/SETUP.md) antes de instalar y [Google/OAuth](GOOGLE_CLOUD_SETUP.md) antes de configurar credenciales.

## Documentación

| Documento | Contenido |
|---|---|
| [Instalación](docs/SETUP.md) | Requisitos, actualización sin perder datos, ajustes y límites de Fire OS |
| [Arquitectura](docs/ARCHITECTURE.md) | Componentes activos, datos, persistencia y pruebas |
| [Google/OAuth](GOOGLE_CLOUD_SETUP.md) | Permisos reales, identidad Android y configuración externa |
| [Especificación vigente](SPECS/current-version/SPEC.md) | Comportamiento actual, sin diseños intermedios sustituidos |
| [Diseño vigente](SPECS/current-version/DESIGN.md) | Decisiones e invariantes de compatibilidad |
| [Validación](SPECS/current-version/TASKS.md) | Evidencia y verificaciones pendientes |

## Límites importantes

- La referencia probada es la Fire HD 8 indicada; no se garantiza compatibilidad con todos los modelos Amazon Fire.
- La instalación de referencia usa pantalla completa, **no Device Owner**. El código opcional de quiosco se conserva, pero no implica que el bloqueo estricto esté activado.
- El sondeo de TV funciona mientras la app está activa. Una falta de respuesta se registra como apagado **inferido**, no como confirmación física.
- El historial TV se calcula en `America/Asuncion`. Calendario y fondo horario usan la hora local del dispositivo.
- La caché conserva la última descarga; no equivale a disponer de Google sin autorización. Un arranque sin token puede requerir recuperar la autorización con Play Services.
- La legibilidad a 2–3 m y el manejo de la esfera horaria con el dedo siguen pendientes de validación por la persona usuaria.

## Origen, licencias y créditos

Family Dashboard es una adaptación, no un reemplazo oficial de Planify ni una aplicación de Amazon. El código base procede de **[Planify Dashboard, de eusonlito](https://github.com/eusonlito/Planify-Dashboard)**, publicado bajo [MIT](LICENSE). Se conserva su aviso de copyright. El proyecto original reconoce la referencia de [wesbos/photo-frame-dashboard](https://github.com/wesbos/photo-frame-dashboard).

- Fuentes **Geist y Geist Mono**: [licencia OFL](licenses/Geist-OFL.txt). Ambas siguen en uso.
- Iconos meteorológicos **Meteocons**, de Bas Milius: [licencia y procedencia](third_party/meteocons/). El generador activo es `scripts/meteocons.py`.
- El icono heredado se conserva; el nombre visible es **Family Dashboard**. Los nombres internos de paquete, firma y credenciales no se renombran.
