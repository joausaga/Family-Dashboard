# Google y OAuth — Family Dashboard

Family Dashboard adapta Planify Dashboard para Amazon Fire y lee Google Calendar/Tasks directamente mediante Play Services Identity Authorization API y REST. No necesita Planify-Backend, `google-services.json` ni un secreto de cliente OAuth en el APK.

## Permisos actuales

La solicitud del código en `MainActivity.kt` contiene únicamente:

- `https://www.googleapis.com/auth/calendar.readonly`
- `https://www.googleapis.com/auth/tasks.readonly`

La monitorización TV es local. **No se solicitan scopes de Sheets o Drive ni se usan esas API.** Si todavía figuran en Data Access o autorizaciones antiguas de Google Cloud, revisar su retirada manualmente; cambiar el repositorio no actualiza la consola ni revoca concesiones previas.

## Nueva identidad Android

- Paquete nuevo: `com.joausaga.familydashboard`.
- Firma nueva: certificado release propio, configurado fuera del repositorio mediante `~/.android/family-dashboard-release.properties` y almacén `~/.android/family-dashboard-release.p12`.
- El cliente OAuth Android debe registrar ese paquete y el SHA-1 del certificado que realmente firma el APK.
- Debug y release pueden tener certificados distintos: no sustituir la firma de producción ni desinstalar la app para probar otra.
- El nombre visible sigue siendo **Family Dashboard**. Esta migración autorizada sí cambia paquete/firma; no cambia proyecto Cloud, scopes, Branding ni DNS.
- La instalación antigua `com.lito.planify.dashboard` se retiró tras validar la migración. Su APK, certificado y cliente Android anterior se conservan como recuperación; no sustituirlos ni eliminarlos sin autorización.

## Configuración externa

La instalación documentada utiliza el proyecto de producción `family-dashboard` (`family-dashboard-513020`). La migración usó un **cliente OAuth Android adicional**, sin modificar el cliente anterior; su creación en la consola y el consentimiento los realizó la persona usuaria. Para una reinstalación nueva se repite ese alta con el paquete y SHA-1 vigentes.

Para comprobar o preparar una instalación:

1. Habilitar Google Calendar API y Google Tasks API.
2. En Google Auth Platform → Clients (o APIs y servicios → Credenciales), crear un cliente OAuth de tipo **Android**, por ejemplo «Family Dashboard — identidad propia». Usar el paquete `com.joausaga.familydashboard` y el SHA-1 del certificado que firma la nueva release. No usar la huella de la APK antigua ni la de debug. Conservar el cliente anterior.
3. Revisar audiencia, usuarios permitidos y permisos de lectura de Calendar/Tasks.
4. Revisar Branding: **Family Dashboard**, contacto y URLs públicas deben coincidir con la app y sus políticas. La consola puede seguir mostrando el nombre anterior hasta cambiarlo manualmente.
5. El sitio se sirve desde los archivos HTML de `docs/`; `docs/CNAME` contiene el dominio configurado en este repositorio. No cambiar dominio, DNS ni publicar el sitio como parte de esta migración.

No hace falta incorporar el client ID Android ni un secreto al APK. El cliente adicional se creó y la conexión se validó en la Fire de referencia el 2 Nov 2026, con Calendar/Tasks de lectura y sin importar la autorización anterior desde el JSON. Registro de huellas y verificaciones: [tareas de identidad](SPECS/android-identity/TASKS.md).

Actualizar los archivos locales no publica GitHub Pages ni cambia la pantalla OAuth. La documentación anterior registró la audiencia en producción; el estado externo actual y cualquier verificación pendiente deben comprobarse en Google Cloud antes de distribuir la aplicación.

## Tokens, conservación y reconexión

El access token vive en memoria y Play Services gestiona la autorización. Al arrancar o recibir un 401, la app puede solicitar un token nuevo. La caché se conserva durante esa renovación. Cerrar sesión intenta revocar el token actual y limpia los datos Google en caché.

En modo externo **Testing**, las autorizaciones con estos scopes pueden caducar a los siete días. Producción evita esa caducidad propia del modo de pruebas, pero no impide revocaciones, errores de Play Services o nuevos consentimientos. Los avisos y límites de una app no verificada dependen de Google; no se promete distribución ilimitada ni aprobación de verificación.

## Referencias

- [Branding OAuth](https://support.google.com/cloud/answer/15549049)
- [Requisitos de homepage](https://support.google.com/cloud/answer/13807376)
- [Requisitos de privacidad](https://support.google.com/cloud/answer/13806988)
- [GitHub Pages y dominio personalizado](https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/managing-a-custom-domain-for-your-github-pages-site)
