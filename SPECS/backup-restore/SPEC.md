# Copia de seguridad y restauración selectiva

- **Estado:** `implemented`
- **Fecha:** 2026-11-02

## Alcance confirmado

Esta especificación registra la primera etapa sin cambio de identidad. La autorización posterior de paquete/firma y la restauración real se completaron en [android-identity](../android-identity/SPEC.md); el 2 Nov 2026 se restauraron ambos bloques en la nueva app y se verificó la reexportación. Esa evidencia posterior no altera los resultados de esta etapa.

Primera etapa para una futura migración de identidad: añadir copia de seguridad sin cambiar paquete, firma, namespace ni OAuth. Ubicación: Ajustes → Dispositivo, bloque «Copia de seguridad», bajo servicios y antes de las acciones. La persona usuaria eligió JSON sin contraseña y selección de historial TV, configuración o ambos al importar.

## Requisitos

- **FR-01:** Exportar un archivo JSON versionado mediante el selector de archivos de Android, compatible con API 22 y sin permisos generales de almacenamiento. Mostrar antes aviso: archivo sin cifrar, contiene datos privados y debe guardarse en lugar seguro.
- **FR-02:** Incluir historial TV completo (transiciones, intervalos y metadatos) y configuración: ubicación/zona del tiempo, horario, brillo, IP/frecuencia TV, frecuencia Google y selecciones de calendarios/listas por cuenta. No incluir tokens, contraseñas, credenciales, estado de autorización, reloj del sistema, Wi-Fi ni cachés de eventos/tareas/pronóstico.
- **FR-03:** Leer y validar completamente antes de ofrecer restaurar: identidad y versión del formato, integridad SHA-256, estructura, valores, referencias de selección y coherencia del historial. Límite de archivo 32 MiB, sin truncar ni omitir registros silenciosamente; informar si excede el límite. No considerar la suma una firma de autenticidad.
- **FR-04:** Mostrar fecha de la copia, cantidad de registros y resumen de configuración. Permitir elegir historial, configuración o ambos, sin selección inicial. Pedir una segunda confirmación que explique qué datos locales se reemplazan; no mezclar historiales. Cancelar, versión desconocida, archivo inválido o lectura fallida no alteran datos locales.
- **FR-05:** Aplicar solo los bloques seleccionados, conservar los demás y la sesión Google. Las selecciones por cuenta se restauran pero solo se aplican a la cuenta conectada correspondiente. Reflejar configuración y agregados, reprogramar horario e invalidar clima si corresponde; no iniciar consentimiento OAuth por importar.
- **FR-06:** Serializar restauración con sondeo TV y evitar respuestas remotas anteriores que sobrescriban el estado restaurado. Guardar antes un diario privado de recuperación; ante fallo, revertir lo aplicado. Recuperar una operación interrumpida antes de reanudar actividad normal. No anunciar éxito antes de persistir.
- **FR-07:** Exportar una instantánea consistente; comprobar el archivo escrito antes de anunciar que está verificado. Si el proveedor no permite verificarlo o falla la escritura, informar y no recomendar usar el resultado como copia válida. Cancelar el selector no es un error ni cambia datos locales.
- **FR-08:** No registrar contenidos sensibles ni incorporar copias reales a Git. Desactivar controles de copia mientras hay una operación y mostrar progreso/resultado. Manejar ausencia del selector y permisos revocados con mensajes claros. No hacer commit/push ni instalar/probar restauración real sin autorización específica.

## Aceptación

- **AC-01:** Round-trip de archivo completo/selección vacía y con datos históricos; ausencia de credenciales y compatibilidad de formato independiente del applicationId.
- **AC-02:** Rechazo sin escrituras de archivo alterado, incompleto, demasiado grande, con versión desconocida, valores inválidos o historial incoherente.
- **AC-03:** Restauración solo de TV, solo configuración y ambos; no modificar autorización ni preferencias ajenas. Rollback y recuperación tras interrupción probados con fallos inyectados.
- **AC-04:** Pruebas unitarias y release correctas. UI verificada en Fire tras permiso; cancelar no restaura. Registrar aparte cualquier prueba destructiva no ejecutada.

## Límites

Archivo sin cifrar, sin garantía de autenticidad: guardar y compartir de forma privada. No exporta la autorización Google ni reemplaza una copia de Fire OS. Esta etapa no cambia paquete/firma; la migración posterior se rige por [android-identity](../android-identity/SPEC.md). Un selector o proveedor de archivos debe estar disponible en el dispositivo. No desinstalar hasta haber verificado una copia y probado su restauración con autorización.
