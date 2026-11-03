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

## Previsiones blancas por la tarde (FR-11)

- [x] Confirmar texto/iconos blancos entre 12:00 y 21:00 y autorización para oscurecer solo las mini-tarjetas; no cambiar el resto de la cabecera.
- [x] Aplicar la excepción después de resolver la paleta original, ajustando opacidad de azul oscuro hasta contraste ≥4,5:1; reloj, fecha y clima actual no cambian.
- [x] Probar límites 11:59/12:00/18:00/20:59/21:00, todos los minutos y conservación del degradado general — `OverviewColorsTest`, incluyendo contraste de todas las superficies durante los 1440 minutos.
- [x] Ejecutar `testDebugUnitTest assembleRelease` — **135 pruebas, 0 fallos; BUILD SUCCESSFUL**. Verificados mismo applicationId y certificado. Actualización en sitio autorizada e instalada en Fire HD 8 sin cambiar reloj ni ajustes.
- [x] Revisar visualmente las cinco previsiones tras actualizar a las 18:13: días, iconos, temperaturas y lluvia blancos sobre azul oscuro; resto de Inicio conservado. Captura renovada en `docs/images/today.png`. La comparativa `overview-times.png` conserva evidencia anterior del degradado general; la apariencia de sus mini-tarjetas queda sustituida por este cambio.

## Etapas posteriores: copias e identidad propia

- [x] Copias: implementación y 159 pruebas correctas; release con identidad antigua instalada en sitio, exportación real verificada y vista previa/confirmación canceladas sin restaurar. Evidencia en [backup-restore](../backup-restore/TASKS.md).
- [x] Tras autorización posterior, cambiar applicationId/namespace/paquetes a `com.joausaga.familydashboard`, generar firma propia fuera del repositorio y conservar clave/app/cliente anteriores. Release correcta y 159 pruebas; nueva app instalada en paralelo y pantalla de conexión Google comprobada en Fire.
- [x] Cliente OAuth Android adicional y conexión Google validados; restauración real de historial/configuración, **2 de 5** calendarios, **1 de 2** listas, **8 h 23 min**, persistencia tras reinicio y nueva copia verificada con 39 transiciones originales conservadas. Tras validar, con autorización, se desinstaló la app anterior; se conservan APK, firma, cliente OAuth y copias. Evidencia completa en [android-identity](../android-identity/TASKS.md); sin commit/push.

## Rediseño de pantalla TV

- [x] Especificar visualización horaria seleccionable con consumo, y fecha/consumo en celdas del heatmap.
- [x] Reemplazar barras por línea horaria de 24 puntos con selector, hora actual, etiquetas de eje y valores de detalle.
- [x] Añadir día del mes y consumo compacto solo si es positivo; mantener detalle de fecha completa y etiqueta accesible.
- [x] Pruebas, build release y revisión visual en Fire KFDOWI/API 22; comprobar rangos heatmap de 4/8/26 semanas.

### Evidencia — 2026-11-03

- `testDebugUnitTest assembleRelease`: **160 pruebas, 0 fallos/errores; BUILD SUCCESSFUL**.
- Release instalada en sitio con `adb install -r`, misma identidad y firma; sin borrar datos. Fire HD 8 KFDOWI/API 22.
- Revisado visualmente el gráfico horario: trayectoria de 24 puntos, hora actual marcada, horas futuras atenuadas, valor de consumo en detalle y selección táctil por intervalo.
- Revisado heatmap con 4, 8 y 26 semanas. Día del mes visible y consumo compacto únicamente en días con uso; al seleccionar la celda del 2 Nov, detalle «Lun 2 Nov · 10 h 11 min». Rango restituido a 8 semanas.
- `git diff --check`: limpio. Capturas de prueba conservadas fuera del repositorio; sin commit/push.

### Ajuste posterior — barras por hora

- [x] Sustituir la línea horaria por 24 barras; anotar el consumo sobre el extremo superior solo si es positivo, manteniendo selección, hora actual y atenuación de horas futuras.
- [x] Pruebas, build e inspección visual de etiquetas de las 24 barras en Fire.

### Evidencia — 2026-11-03

- `testDebugUnitTest assembleRelease`: **161 pruebas, 0 fallos/errores; BUILD SUCCESSFUL**.
- Release actualizada en sitio con la misma firma y paquete en Fire KFDOWI/API 22; no se borraron datos.
- Inspección visual: las 24 horas se representan; solo las barras con consumo tienen rótulo sobre el extremo superior, mientras que el cero queda sin etiqueta. La selección de una hora continúa mostrando intervalo y consumo completos en el encabezado.
- Captura privada fuera del repositorio; `git diff --check` limpio. Sin commit/push.

### Ajuste posterior — ocultar consumo cero

- [x] Omitir por completo las etiquetas en horas con consumo cero; mantener las etiquetas compactas positivas y la descripción accesible «sin uso».
- [x] `testDebugUnitTest assembleRelease`: **161 pruebas, 0 fallos/errores; BUILD SUCCESSFUL**. Release actualizada en Fire con `adb install -r` y comprobación visual: solo las barras de 31 min y 46 min tienen etiquetas.

### Ajuste posterior — heatmap horizontal

- [x] Reorganizar análisis TV: gráfico arriba; heatmap a dos columnas/ancho completo en una fila; comparativas y totales mensuales en la última fila.
- [x] Compactar etiquetas/celdas heatmap y resúmenes para el nuevo reparto sin retirar rangos, detalle ni accesibilidad.
- [x] `testDebugUnitTest assembleRelease`: **161 pruebas, 0 fallos/errores; BUILD SUCCESSFUL**. Release instalada en Fire KFDOWI/API 22. Comprobación visual: disposición horizontal correcta con 8 y 26 semanas; comparativas y totales mensuales visibles en la última fila.
- [ ] Completar comprobación visual del rango de 4 semanas y selección de una celda; la captura del rango de 26 semanas queda en `/private/var/folders/lm/ldxqrxyd4b73tgv90dsy28xh0000gn/T/opencode/tv-26-rotated.png`.
- [x] Expandir el heatmap por el ancho y alto disponibles; distribuir las comparativas y los totales mensuales por la altura de sus tarjetas. Verificado en Fire KFDOWI/API 22 tras instalar la release.
- [x] Dar más altura al gráfico horario, reservar una banda de meses legible sobre el heatmap, separar el número de día (esquina superior izquierda en gris) del consumo y reducir a la mitad la tercera fila. Release compilada, instalada y revisada visualmente en Fire KFDOWI/API 22; **161 pruebas aprobadas**.
- [x] Aumentar la tipografía del consumo en heatmap y barras horarias y de los rótulos comparativos semanal/mensual, con ajuste por anchura de celda para rangos extensos. Release compilada e instalada en Fire; comprobación visual de tipografía ampliada.
- [x] Aumentar el tamaño de las etiquetas horarias y reservar una franja sobre las barras para evitar que texto y barras se solapen. Release instalada y comprobada visualmente en Fire KFDOWI/API 22; **161 pruebas aprobadas**.
- [x] Aumentar las etiquetas de consumo y los números de día del heatmap; abreviar el consumo de horas para celdas estrechas sin alterar el detalle accesible completo. Release compilada e instalada; comprobación visual en Fire KFDOWI/API 22; **161 pruebas aprobadas**.

## Otros pendientes

La mejora posterior de zona horaria TV está especificada y verificada en [tv-weather-timezone](../tv-weather-timezone/TASKS.md): usa la ubicación meteorológica, recalcula sin alterar registros y conserva compatibilidad con preferencias/caché antiguas. Su ejecución de pruebas es posterior a los resultados de la limpieza registrados arriba.

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
| AC-07 | Build release; instalación inicial en paralelo según android-identity y futuras actualizaciones del nuevo paquete con su misma firma |
| AC-08 | Confirmación futura de la persona usuaria |
