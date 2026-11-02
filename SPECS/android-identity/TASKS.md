# Tareas de identidad Android

- [x] Confirmar paquete, nueva clave y autorización de instalación/OAuth/restauración selectiva de ambos bloques.
- [x] Especificar requisitos, riesgos, diseño y criterios antes de implementar.
- [x] Preservar APK anterior y verificar acceso a copia privada de 39 transiciones (FR-03, FR-05–06).
- [x] Crear firma privada sin sobrescribir clave anterior; configurar Gradle (FR-02).
- [x] Refactorizar paquetes, rutas, namespace, applicationId, R8 y documentación vigente (FR-01).
- [x] Pruebas/release y verificación de certificado/componentes/API 22 (AC-01).
- [x] Instalar en paralelo y verificar arranque (AC-02).
- [x] Facilitar paquete/SHA-1, alta del cliente Cloud por la persona usuaria y conexión Google (AC-03).
- [x] Restaurar ambos bloques y comprobar persistencia/exportación (AC-04).
- [x] Revisión documental, enlaces, privacidad y estado Git tras la migración real; sin commit/push (AC-05).

## Evidencia — 2026-11-02

- `testDebugUnitTest assembleRelease`: **159 pruebas, 0 fallos/errores; BUILD SUCCESSFUL**, suites ejecutadas bajo el nuevo paquete. Advertencias de APIs obsoletas y Gradle conservadas, no bloqueantes.
- APK: nombre Family Dashboard, paquete `com.joausaga.familydashboard`, minSdk 22. `apksigner verify --min-sdk-version 22` correcto y certificado diferente al de la APK anterior preservada en almacenamiento temporal privado.
- Manifiesto empaquetado: actividad principal, administración opcional, servicio y receptores resueltos en el nuevo paquete; sin referencias a componentes antiguos. Regla R8 de modelos trasladada al nuevo paquete; formato JSON y claves persistidas conservados.
- Nueva clave RSA 3072/SHA256withRSA, PKCS12 y configuración privada en `~/.android/`, permisos 0600. No se imprimieron contraseñas ni se sobrescribieron archivos de firma anteriores. Respaldar privadamente `family-dashboard-release.p12` y `family-dashboard-release.properties` juntos.
- Certificado nuevo SHA-1: `96:A5:6E:FF:7F:18:C6:2C:9B:0F:7C:44:4C:E9:82:62:6D:37:A2:AA`.
- Certificado nuevo SHA-256: `32:56:30:AE:9C:65:75:99:01:3C:6B:0C:A1:39:0A:39:B3:F3:23:B2:5C:41:C0:F2:6F:5F:07:C4:68:5D:2A:FA`.
- Fire comprobada: KFDOWI, API 22, 800 × 1280 físico. Instalación nueva mediante `adb install`, sin `-r` ni desinstalación; ambos paquetes presentes después. Arranque explícito de `com.joausaga.familydashboard/.MainActivity`: pantalla de conexión Google correcta, captura privada fuera del repositorio.
- **OAuth (AC-03):** la persona usuaria creó el cliente Android adicional en `family-dashboard-513020` con el paquete y SHA-1 indicados y completó el consentimiento en la tablet. La primera carga falló de forma transitoria; tras reiniciar la app cargaron **4 de 5 calendarios** y **2 listas**, y la sesión se conservó. Solo se solicitaron los dos scopes de lectura existentes; no se cambió el cliente anterior, Branding, DNS ni launcher predeterminado. El diagnóstico temporal usado para descartar el fallo se retiró antes de la release final y esta se reinstaló con `adb install -r`.
- **Restauración real (AC-04):** se importó `/sdcard/Download/family-dashboard-20261102-220531.json` desde Ajustes → Dispositivo, con historial y configuración seleccionados y la confirmación de reemplazo. Mensaje final: «Restauración completada: historial TV y configuración. La sesión Google se conserva».
- **Configuración comprobada:** selección visible **2 de 5 calendarios** (Días feriados en Paraguay y Family) y **1 de 2 listas** (Family); ubicación **Asunción, Paraguay**; horario de lunes a viernes **7:00–23:00** y fin de semana **8:30–23:30**; brillo **automático**; IP de TV `192.168.100.233`, sondeo **1 minuto**; Google cada **5 minutos**.
- **Historial y totales:** Inicio y TV muestran **8 h 23 min** hoy; **12 h 18 min** en noviembre y **4 h 49 min** en octubre. La rejilla semanal refleja el día restaurado y la TV quedó encendida al responder durante la prueba.
- **Persistencia (AC-04):** tras `am force-stop` y nuevo arranque de solo `com.joausaga.familydashboard`, se conservaron sesión Google, **2 de 5** calendarios, **1 de 2** listas y **8 h 23 min**, sin volver a importar.
- **Reexportación (AC-04):** Ajustes → Dispositivo → Exportar copia generó `/sdcard/Download/family-dashboard-20261102-225411.json` (10386 bytes, formato v1). La app lo verificó y una comprobación independiente confirmó: configuración **idéntica** al origen, **39 transiciones originales conservadas en orden** y **una transición adicional** por la respuesta de la TV durante la prueba; solo cambió la marca de creación. SHA-256 del archivo: `2749dafae1ca2895189504304b257eb93824c829f11d9f9fe06d611a45d565a1`.
- **Conservación durante la validación:** la app anterior `com.lito.planify.dashboard`, su firma, su cliente OAuth y sus datos se mantuvieron intactos hasta completar OAuth, restauración, persistencia y reexportación.
- **Retirada autorizada:** tras esa validación, se ejecutó `adb uninstall com.lito.planify.dashboard`; `pm path` ya no encuentra el paquete y la app nueva sigue instalada. Se conservan el APK anterior preservado, la firma y configuración privadas, el cliente OAuth anterior y las dos copias JSON. Solo se eliminaron los datos privados de la instalación desinstalada.
- **Estado del dispositivo tras retirar:** la app nueva se reinició y conservó sesión, **2 de 5** calendarios, **1 de 2** listas e historial; el total pasó de **8 h 23 min** a **8 h 28 min** porque la TV siguió encendida. El launcher predeterminado sigue siendo Fire Launcher y no hay Device Owner/Profile Owner. Captura privada fuera del repositorio.
- Revisión local: 159 pruebas y release correctas tras retirar el diagnóstico; enlaces Markdown válidos, `git diff --check` correcto, firma fuera del repositorio con permisos 0600 y exclusiones Git para almacenes/configuración. Las capturas y copias privadas quedaron fuera de Git; no se hizo commit/push.
