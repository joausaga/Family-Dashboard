# Validación vigente — Family Dashboard

## Evidencia anterior conservada

- [x] Adaptación API 22, escala, pantalla completa, Google Calendar/Tasks y reloj nocturno probados en Fire HD 8 KFDOWI.
- [x] Inicio, Tareas, TV y Ajustes revisados visualmente en el panel durante su implementación.
- [x] Paleta horaria probada a las 09:00, 15:00, 18:00 y 21:00 sin cambiar la hora del dispositivo; comparación en [overview-times.png](../../docs/images/overview-times.png). Datos de presentación temporales retirados antes de instalar la release final.
- [x] Ejecución previa a la limpieza: 120 pruebas aprobadas y release instalada conservando datos.

## Limpieza actual

- [x] Confirmar retirada de backend antiguo/importador con adaptación de pruebas y consolidación documental con la persona usuaria.
- [x] Nombre visible Family Dashboard, conservando paquete, firma, icono, versión y preferencias.
- [x] Retirar reloj de paletas, texto animado, utilidades sin llamadas, login/backend Planify e importador remoto Sheets/Drive.
- [x] Adaptar regresiones de calendario a APIs Google simuladas y añadir pruebas de contenido en caché.
- [x] Añadir regresión de historial TV importado, metadatos, reinicio y sondeo local.
- [x] Consolidar requisitos vigentes, actualizar guías de Amazon Fire, reconocer Planify y conservar licencias.
- [x] Retirar documentación/scripts/snapshots del XORO y referencias locales obsoletas; conservar el generador Meteocons. No se borran configuraciones privadas ignoradas por Git.
- [x] Comprobar enlaces Markdown/HTML locales y ausencia de símbolos retirados en código principal — Resultado: sin enlaces rotos ni definiciones/llamadas del backend/importador retirados.
- [x] Ejecutar `testDebugUnitTest assembleRelease` tras terminar la limpieza — Resultado: **123 pruebas, 0 fallos; BUILD SUCCESSFUL**. Se retiró la prueba exclusiva del importador y se añadieron tres pruebas de contenido y una de compatibilidad del historial.
- [x] Comparar nombre, applicationId y certificado del APK con la release previa — `aapt` confirma Family Dashboard y `com.lito.planify.dashboard`; `apksigner` verifica firma válida y SHA-256 del certificado idéntico.
- [x] Con autorización expresa, actualizar la Fire HD 8 con `adb install -r` y revisar Inicio, TV y Contenido — Resultado: sin desinstalar ni cerrar sesión; mismos eventos/tarea y selección (2 de 5 calendarios y 1 de 2 listas), TV conserva 8 h 23 min del día y los periodos previos. Horario activo y brillo automático siguen indicados. No se cambió ningún control. Capturas de verificación con datos privados se mantienen fuera del repositorio; `docs/images/today.png` se actualizó con Inicio.

## Pendientes ajenos a la limpieza

- [ ] **AC-08:** legibilidad a 2–3 m y manejo de esfera horaria con el dedo, a cargo de la persona usuaria.
- [ ] Selecciones Google: comprobar aislamiento/persistencia con una segunda cuenta, listas vacías/nuevas, selección vacía, scroll largo y reintento tras fallo de carga. No cambiar de cuenta en esta limpieza.
- [ ] Tareas: comprobar varias listas reales, expansión y desplazamiento horizontal con cuatro o más listas; los datos reales de la revisión anterior eran limitados.
- [ ] TV: comprobar historial real de varias semanas/meses y estado desactualizado; fallos con TV apagada, red desconectada e IP errónea; intervalos de 1 min y otro valor, suspensión/reanudación, timeout y ausencia de duplicados. La prueba previa de «Probar conexión» solo confirmó TV encendida.
- [ ] Autorización: observar uso más allá de siete días, renovación ordinaria y reinicio de la Fire; probar revocación y reintento sin conexión con autorización específica para interrumpir la sesión.
- [ ] Confirmar cierre de sesión real; hasta ahora se probó el diálogo y Cancelar, sin cerrar para conservar el acceso. No ejecutarlo durante esta limpieza.
- [ ] Revisar en Google Cloud el nombre de Branding y las concesiones/scopes históricos de Sheets/Drive. No se modifican consola, dominios ni credenciales desde este trabajo.
- [ ] Publicar cambios de sitio/documentación solo cuando se autorice commit/push. Los archivos locales no implican publicación.

## Trazabilidad

| Criterio | Evidencia prevista |
|---|---|
| AC-01 | Recursos APK, certificado, README/licencias y HTML |
| AC-02 | `GoogleWorkspaceRepositoryTest`, `CalendarRepositoryTest`, `ContentRepositoryTest`, caché/selección |
| AC-03 | `OverviewColorsTest`, captura comparativa |
| AC-04 | Pruebas de Tareas, TV, Settings y ScreenSchedule; captura del panel |
| AC-05 | `TvLocalUsageRepositoryTest`, historial del dispositivo tras actualización |
| AC-06 | Búsqueda de símbolos y verificación de enlaces/documentos |
| AC-07 | Build release y actualización firmada en sitio |
| AC-08 | Confirmación futura de la persona usuaria |
