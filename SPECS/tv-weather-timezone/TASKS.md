# Tareas: zona TV de la ubicación meteorológica

- [x] Confirmar con la persona usuaria: recalcular todo y fallback al dispositivo.
- [x] Especificar requisitos, compatibilidad y diseño antes de implementar.
- [x] Campos opcionales, resolución de zona y asociación de pronóstico con ubicación — `WeatherTimeZone.kt`, `Location`, `GeoResult`, `Forecast` y `WeatherRepository`.
- [x] Recalcular TV al cambiar reloj, historial, ubicación o zona; persistir zona conocida — helper único del ViewModel, geocodificación y respuesta meteorológica aceptada.
- [x] Alinear fecha/hora y comparaciones de Inicio y TV — `UiState.tvTime`, `ZonedDateTime` en la pantalla TV y fecha TV en tendencia/promedio de Inicio.
- [x] Pruebas de zonas, fallback, JSON antiguo, respuestas obsoletas, fronteras y DST — 8 pruebas nuevas de resolución/tiempo/historial y 2 de repositorio meteorológico. Se conserva la duración y el JSON del historial al reagrupar.
- [x] Actualizar documentación vigente; ejecutar `testDebugUnitTest assembleRelease` — **133 pruebas, 0 fallos; BUILD SUCCESSFUL**. Enlaces locales y `git diff --check` correctos; sin ciudad fija en código productivo.
- [x] Tras autorización expresa, verificar firma/paquete e instalar en sitio en Fire HD 8 — mismo certificado y `com.lito.planify.dashboard`, instalación correcta con `adb install -r`, sin desinstalar ni cerrar sesión.
- [x] Revisar Inicio y TV con la ubicación real, sin cambiar ubicación ni reloj — se conservan 8 h 23 min de hoy y los periodos históricos (noviembre 12 h 18 min, octubre 4 h 49 min); hora actual y fecha coherentes. Capturas privadas fuera del repositorio. Los cambios de ubicación, fronteras y uso sin red se verificaron con pruebas automatizadas, no alterando la configuración real del dispositivo.
