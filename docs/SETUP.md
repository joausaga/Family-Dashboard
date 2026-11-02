# Instalación de Family Dashboard en Amazon Fire

Adaptación de Planify Dashboard para la **Fire HD 8 de 7.ª generación (KFDOWI)**, Fire OS/Android 5.1.1/API 22, 1280 × 800 horizontal. El comportamiento se comprobó en ese hardware; otros modelos requieren validación propia.

## Requisitos

- Ordenador con JDK 17 o superior, Android SDK plataforma 37 y platform-tools (`adb`). El entorno de referencia usa macOS y `~/Library/Android/sdk`.
- Depuración USB habilitada, cable de datos y dispositivo autorizado en `adb devices -l`.
- Google Play Services operativo y cuenta Google disponible para autorizar Calendar/Tasks. Esta guía no instala ni modifica Play Services o Fire OS.
- Credencial OAuth Android que corresponda al paquete y certificado del APK; ver [Google/OAuth](../GOOGLE_CLOUD_SETUP.md).

No se requieren backend Planify, root, escrituras en `/system`, cortafuegos del fabricante ni modificaciones de particiones.

## Compilar

```sh
cd android
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :app:testDebugUnitTest :app:assembleRelease
```

La nueva firma de producción se carga desde `~/.android/family-dashboard-release.properties`; el almacén PKCS12 es `~/.android/family-dashboard-release.p12`. Ambos quedan fuera del repositorio y deben respaldarse juntos en un lugar privado y seguro. No copiar contraseñas al código, Git, documentación ni chat. Sin esa configuración, no tratar el APK generado como actualización de producción. Los archivos de firma anteriores se conservan sin sobrescribir.

El nuevo `applicationId`, namespace y paquete del código es `com.joausaga.familydashboard`; el nombre visible sigue siendo **Family Dashboard**. La app anterior `com.lito.planify.dashboard` tenía otra firma, no se actualizaba con esta APK y ya se retiró tras validar la migración.

## Migración inicial a la nueva identidad

Procedimiento seguido y validado el 2 Nov 2026; la app anterior ya no está instalada. Consérvalo para una reinstalación o para entender la recuperación disponible.

1. Conserva instalada la app anterior y una exportación verificada de su historial/configuración. No desinstales, borres sus datos, cierres su sesión ni retires su cliente OAuth.
2. Registra un cliente OAuth Android adicional para el nuevo paquete y el SHA-1 del certificado de la nueva release, según [Google/OAuth](../GOOGLE_CLOUD_SETUP.md).
3. Verifica la nueva APK y usa `adb install app/build/outputs/apk/release/app-release.apk` para instalar en paralelo. Abre explícitamente `com.joausaga.familydashboard/.MainActivity`; no cambies HOME/launcher ni Device Owner como parte de esta prueba.
4. Conecta Google en la app nueva y usa Ajustes → Dispositivo para importar la copia. Tras autorización, selecciona historial y configuración y confirma el reemplazo solo en la app nueva.
5. Comprueba fuentes seleccionadas, ubicación, horario, brillo, historial/totales y persistencia al reiniciar; exporta y verifica una nueva copia. Tras validar, la app anterior se desinstaló con autorización. Se conservan su APK, firma, cliente OAuth y copias, no sus datos privados de instalación.

Las dos apps tenían receptores de horario. Durante la migración se evitó ejecutarlas alternadamente; ahora solo queda la nueva. El launcher predeterminado sigue siendo Fire Launcher y no se registró Device Owner. Evidencia: [tareas](../SPECS/android-identity/TASKS.md).

## Actualizaciones posteriores sin perder datos

1. Verifica el dispositivo con `adb devices -l`. Si hay más de uno, utiliza `adb -s <identificador>`.
2. Compila una release con la misma firma que la instalación existente.
3. Instala en sitio:

   ```sh
   adb install -r app/build/outputs/apk/release/app-release.apk
    adb shell am start -n com.joausaga.familydashboard/.MainActivity
   ```

4. Comprueba Inicio, autorización, fuentes seleccionadas, ubicación, horario y total/historial de TV.

`INSTALL_FAILED_UPDATE_INCOMPATIBLE` suele indicar firmas distintas. **No desinstalar ni borrar datos para resolverlo:** revisa paquete/certificado y recompila con la clave correcta. La migración desde el paquete antiguo usa el flujo independiente anterior; futuras actualizaciones del nuevo paquete deben conservar su nueva firma.

## Primer uso

1. Conecta la Fire a Wi-Fi y comprueba fecha y zona horaria del sistema.
2. Abre Family Dashboard y pulsa «Continuar con Google». Solo deben solicitarse Calendar y Tasks de lectura.
3. En Ajustes → Contenido, elige calendarios y listas. Los nuevos se activan inicialmente; se permite ocultarlos todos.
4. En Servicios, selecciona la ubicación meteorológica y el intervalo de actualización de calendario/tareas.
5. Para TV, configura su IP local y frecuencia, usa «Probar conexión» y guarda. Probar no escribe en el historial. Sin IP no se inicia el sondeo.
6. En Pantalla, configura horario laboral/fin de semana y brillo. Los botones abren una esfera de 12 horas con AM/PM; el apagado debe quedar al menos 15 minutos después del encendido.

## Pantalla completa, noche y quiosco

La Fire de referencia oculta barras de estado/navegación con la ruta legacy de Android. Esto **no es lock task ni Device Owner**. No se elimina ninguna cuenta ni se aprovisiona Device Owner automáticamente.

El reloj nocturno muestra hora, fecha y clima con brillo mínimo. Tocar permite consultar el panel durante cinco minutos fuera del horario. El apagado completo mediante `lockNow()` requiere Device Owner; sin él puede no estar disponible. La infraestructura opcional de quiosco, retorno desde Ajustes y servicio de apagado sigue en el código, pero no se activa por instalar la APK.

El sondeo TV y la sincronización dependen de que la app esté activa; no son servicios independientes de monitorización continua. Los informes TV usan la zona horaria de la ubicación seleccionada para el tiempo. Cambiar esa ubicación recalcula el historial completo, pero no modifica sus registros originales. La zona conocida se conserva sin conexión; si aún no hay una válida se utiliza provisionalmente la del dispositivo. No se cambia la zona de Android ni la del calendario Google.

## Diagnóstico y recuperación

Para una copia portable, usa **Ajustes → Dispositivo → Copia de seguridad**. Exporta el JSON a un destino privado y espera el mensaje de verificación. Al importar, la vista previa no escribe: elige historial TV, configuración o ambos y confirma el reemplazo. No importa autorización Google ni credenciales, ni modifica paquete/firma. Consulta el [formato y garantías](../SPECS/backup-restore/SPEC.md).

En el selector de la Fire puede aparecer inicialmente solo «Recent». Abre el menú, activa **«Show SD card»** y elige **«Internal storage» → «Download»** para guardar en Descargas. No requiere una tarjeta SD física. Al importar, el selector puede recordar esa carpeta.

No pruebes una restauración real sin autorización: reemplaza los bloques seleccionados y puede cambiar horario, brillo, ubicación e IP TV. Conserva antes una exportación verificada del estado actual. El JSON no lleva contraseña; si eliges un proveedor en la nube, ese proveedor almacenará la copia. Un fallo de escritura/verificación requiere repetir la exportación, no usar un archivo posiblemente incompleto.

Si se interrumpe una restauración, se intenta recuperar el estado anterior al arrancar; si queda pendiente, libera espacio si corresponde y usa «Reintentar recuperación». No borres los datos de la aplicación para resolverlo: perderías también el diario y el historial.

- **No autoriza Google:** revisar Play Services, conexión, paquete y SHA-1 del certificado; no sustituir la firma instalada.
- **Google vuelve a pedir acceso:** puede ser renovación fallida, revocación o configuración OAuth de pruebas. Ver [Google/OAuth](../GOOGLE_CLOUD_SETUP.md).
- **TV no responde:** revisar IP, conectividad LAN y disponibilidad del endpoint Samsung; la app infiere apagado ante falta de respuesta.
- **Datos atrasados:** consultar Ajustes → Dispositivo y sincronizar manualmente; la caché no garantiza actualidad.
- **Volver a una APK anterior:** solo una versión compatible con firma/datos/versionCode permite actualización en sitio; no se garantiza downgrade. No borrar datos ni cambiar identidad para forzarlo.

Las capturas de `adb exec-out screencap -p` pueden requerir giro de 90° para verse en horizontal. No cambiar el reloj del dispositivo para probar colores: la función de paleta tiene pruebas por hora y minuto.
