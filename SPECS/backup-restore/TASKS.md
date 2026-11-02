# Tareas de copia/restauración

- [x] Confirmar primera etapa, ubicación de UI, restauración selectiva y JSON sin contraseña.
- [x] Especificar requisitos y diseño antes de implementar.
- [x] Codec/validación y DTO de lista blanca; pruebas de integridad, errores y límites (FR-01–03, FR-07; AC-01–02).
- [x] Instantáneas de preferencias/historial y restauración con rollback/recuperación (FR-05–06; AC-03).
- [x] Coordinación ViewModel y sondeo/respuestas remotas; conservar sesión y actualizar UI (FR-05–06).
- [x] Selector Android, aviso, vista previa, selección, confirmación y mensajes (FR-01, FR-04, FR-08).
- [x] Pruebas automáticas y release; documentación, privacidad y especificación vigente (AC-01–04).
- [x] Instalación y comprobaciones en Fire tras permiso; exportación real verificada y revisión de importación cancelada sin restaurar (AC-04).
- [x] Restauración real: ejecutada después, con autorización, en la nueva app de [identidad Android](../android-identity/TASKS.md); historial y configuración restaurados, persistencia y reexportación verificadas. La app anterior se conservó durante la validación y se retiró después con autorización.

## Evidencia — 2026-11-02

- `testDebugUnitTest assembleRelease`: **159 pruebas, 0 fallos/errores**, release compilada correctamente. Incluye 24 pruebas nuevas de codec, lista blanca, transacciones y operaciones de documentos; reemplazo selectivo, rollback y recuperación se verificaron con almacenamiento de prueba.
- Certificado y `applicationId` comparados con la release anterior: misma identidad y firma. Actualización en sitio con `adb install -r` en Fire HD 8 KFDOWI/API 22, sin desinstalar ni borrar datos.
- Sesión Google, selecciones (2/5 calendarios y 1/2 listas), horario y brillo conservados tras instalar.
- Exportación real en Descargas: mensaje «Copia exportada y verificada». Archivo privado conservado en la tablet, no incorporado al repositorio: 10197 bytes, formato `family-dashboard-backup` v1 y 39 transiciones; sin claves de credenciales ni diario de recuperación.
- Vista previa del mismo archivo: fecha, 39 transiciones, Asunción y preferencias de 1 cuenta. Sin selección inicial; «Continuar» deshabilitado hasta elegir un bloque.
- Segunda confirmación revisada eligiendo solo historial TV; cancelada con Atrás, sin pulsar «Restaurar selección». Comprobación posterior: ningún diálogo pendiente y sesión/configuración visibles conservadas.
- TV e Inicio revisados después de cancelar: total diario conservado de **8 h 23 min**, historial y contenido Google visibles. Tablet devuelta a Inicio.
- Selector Fire: para mostrar Descargas fue necesario activar «Show SD card», abrir «Internal storage» y después «Download». La importación recordó esa carpeta.
- En esta primera etapa no se cambiaron paquete, namespace, firma, OAuth ni sesión Google, no se ejecutó la restauración real y no se hizo commit/push. La restauración real posterior se registra en la evidencia de identidad.
- Revisión final: `git diff --check` sin incidencias y enlaces Markdown locales de la documentación modificada válidos. Capturas privadas fuera del repositorio.
