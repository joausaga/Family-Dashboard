# Google y OAuth — Family Dashboard

Family Dashboard adapta Planify Dashboard para Amazon Fire y lee Google Calendar/Tasks directamente mediante Play Services Identity Authorization API y REST. No necesita Planify-Backend, `google-services.json` ni un secreto de cliente OAuth en el APK.

## Permisos actuales

La solicitud del código en `MainActivity.kt` contiene únicamente:

- `https://www.googleapis.com/auth/calendar.readonly`
- `https://www.googleapis.com/auth/tasks.readonly`

La monitorización TV es local. **No se solicitan scopes de Sheets o Drive ni se usan esas API.** Si todavía figuran en Data Access o autorizaciones antiguas de Google Cloud, revisar su retirada manualmente; cambiar el repositorio no actualiza la consola ni revoca concesiones previas.

## Identidad Android: no cambiar

- Paquete: `com.lito.planify.dashboard`.
- Firma: certificado release existente, configurado fuera del repositorio mediante `~/.android/planify-family-dashboard-release.properties`.
- El cliente OAuth Android debe registrar ese paquete y el SHA-1 del certificado que realmente firma el APK.
- Debug y release pueden tener certificados distintos: no sustituir la firma de producción ni desinstalar la app para probar otra.
- El nombre visible de la APK es **Family Dashboard**. Esto no exige cambiar paquete, client ID, proyecto Cloud ni almacén de claves.

## Configuración externa

La instalación documentada utiliza el proyecto de producción `family-dashboard` (`family-dashboard-513020`) y conserva sus credenciales existentes. No se modifican desde esta limpieza.

Para comprobar o preparar una instalación:

1. Habilitar Google Calendar API y Google Tasks API.
2. Verificar que el cliente Android corresponda al paquete y certificado release.
3. Revisar audiencia, usuarios permitidos y permisos de lectura de Calendar/Tasks.
4. Revisar Branding: **Family Dashboard**, contacto y URLs públicas deben coincidir con la app y sus políticas. La consola puede seguir mostrando el nombre anterior hasta cambiarlo manualmente.
5. El sitio se sirve desde los archivos HTML de `docs/`; `docs/CNAME` contiene el dominio configurado en este repositorio. No cambiar dominio, DNS ni credenciales como parte del renombrado.

Actualizar los archivos locales no publica GitHub Pages ni cambia la pantalla OAuth. La documentación anterior registró la audiencia en producción; el estado externo actual y cualquier verificación pendiente deben comprobarse en Google Cloud antes de distribuir la aplicación.

## Tokens, conservación y reconexión

El access token vive en memoria y Play Services gestiona la autorización. Al arrancar o recibir un 401, la app puede solicitar un token nuevo. La caché se conserva durante esa renovación. Cerrar sesión intenta revocar el token actual y limpia los datos Google en caché.

En modo externo **Testing**, las autorizaciones con estos scopes pueden caducar a los siete días. Producción evita esa caducidad propia del modo de pruebas, pero no impide revocaciones, errores de Play Services o nuevos consentimientos. Los avisos y límites de una app no verificada dependen de Google; no se promete distribución ilimitada ni aprobación de verificación.

## Referencias

- [Branding OAuth](https://support.google.com/cloud/answer/15549049)
- [Requisitos de homepage](https://support.google.com/cloud/answer/13807376)
- [Requisitos de privacidad](https://support.google.com/cloud/answer/13806988)
- [GitHub Pages y dominio personalizado](https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/managing-a-custom-domain-for-your-github-pages-site)
