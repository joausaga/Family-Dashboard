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

La firma de producción existente se carga desde `~/.android/planify-family-dashboard-release.properties`; el almacén de claves también queda fuera del repositorio. No copiar credenciales al código, a Git ni a la documentación. Sin la configuración de firma, no tratar el APK generado como actualización de producción.

Se conservan `applicationId` y namespace `com.lito.planify.dashboard`, versión y certificado. El nombre que muestra Android es **Family Dashboard**; no cambia la identidad de instalación.

## Actualizar sin perder datos

1. Verifica el dispositivo con `adb devices -l`. Si hay más de uno, utiliza `adb -s <identificador>`.
2. Compila una release con la misma firma que la instalación existente.
3. Instala en sitio:

   ```sh
   adb install -r app/build/outputs/apk/release/app-release.apk
   adb shell am start -n com.lito.planify.dashboard/.MainActivity
   ```

4. Comprueba Inicio, autorización, fuentes seleccionadas, ubicación, horario y total/historial de TV.

`INSTALL_FAILED_UPDATE_INCOMPATIBLE` suele indicar firmas distintas. **No desinstalar ni borrar datos para resolverlo:** revisa el certificado y recompila con la clave correcta. Cambiar el paquete también crearía otra instalación. No hay una migración de datos en esta limpieza.

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

El sondeo TV y la sincronización dependen de que la app esté activa; no son servicios independientes de monitorización continua. Los informes TV usan `America/Asuncion`.

## Diagnóstico y recuperación

- **No autoriza Google:** revisar Play Services, conexión, paquete y SHA-1 del certificado; no sustituir la firma instalada.
- **Google vuelve a pedir acceso:** puede ser renovación fallida, revocación o configuración OAuth de pruebas. Ver [Google/OAuth](../GOOGLE_CLOUD_SETUP.md).
- **TV no responde:** revisar IP, conectividad LAN y disponibilidad del endpoint Samsung; la app infiere apagado ante falta de respuesta.
- **Datos atrasados:** consultar Ajustes → Dispositivo y sincronizar manualmente; la caché no garantiza actualidad.
- **Volver a una APK anterior:** solo una versión compatible con firma/datos/versionCode permite actualización en sitio; no se garantiza downgrade. No borrar datos ni cambiar identidad para forzarlo.

Las capturas de `adb exec-out screencap -p` pueden requerir giro de 90° para verse en horizontal. No cambiar el reloj del dispositivo para probar colores: la función de paleta tiene pruebas por hora y minuto.
