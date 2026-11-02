# Identidad Android propia y migración

- **Estado:** `implemented`
- **Fecha:** 2026-11-02
- **Validación:** OAuth, restauración de ambos bloques, persistencia y reexportación comprobados en la Fire de referencia; evidencia en [TASKS.md](TASKS.md).

## Alcance autorizado

La persona usuaria confirmó `com.joausaga.familydashboard` para applicationId, namespace y paquetes del código; crear una nueva clave privada fuera del repositorio, instalar en paralelo, conectar Google con su intervención y restaurar ambos bloques desde la copia verificada. Conservar la aplicación, cliente OAuth, firma y datos anteriores. Sin commit/push.

## Requisitos

- **FR-01:** Cambiar identidad y paquetes de fuentes/pruebas a `com.joausaga.familydashboard`; conservar nombre visible, créditos, permisos, modelos, claves persistidas y formato de copia v1. Actualizar manifiesto, reglas R8 y referencias activas.
- **FR-02:** Generar nueva firma release y configuración bajo `~/.android/`, sin sobrescribir archivos existentes; contraseñas aleatorias, permisos privados y secretos fuera de Git/logs. Mantener firma anterior. Informar qué archivos respaldar y publicar solo huellas del certificado.
- **FR-03:** Compilar y verificar APK para API 22, paquete correcto y certificado distinto. Instalar como aplicación independiente sin desinstalar, borrar datos, revocar Google ni cambiar launcher predeterminado/Device Owner.
- **FR-04:** Google requiere cliente Android adicional para nuevo paquete/SHA-1 en el proyecto existente; facilitar instrucciones y esperar configuración/intervención de la persona usuaria. No borrar ni sustituir cliente antiguo, ampliar scopes ni modificar Cloud/DNS automáticamente.
- **FR-05:** Tras conectar Google, importar la copia privada verificada y restaurar historial y configuración con confirmación autorizada; verificar selecciones por cuenta, ubicación, horario, brillo e historial y volver a comprobar persistencia al reiniciar. No importar tokens ni copiar almacenamiento privado entre apps.
- **FR-06:** Conservar la instalación anterior hasta completar la validación; retirarla solo con autorización posterior, conservando APK, firma, cliente OAuth y copias como recuperación. Registrar verificaciones reales y pendientes, actualizar documentación vigente y no afirmar migración validada hasta comprobar OAuth/restauración.

## Aceptación

- **AC-01:** Pruebas unitarias y release correctas; manifiesto/componentes y reglas R8 apuntan al nuevo paquete; firma válida y diferente a la anterior.
- **AC-02:** Ambas apps instaladas; anterior intacta, nueva inicia correctamente en Fire KFDOWI/API 22.
- **AC-03:** Nueva autorización Google y contenido Calendar/Tasks comprobados sin nuevos scopes.
- **AC-04:** Restauración real de ambos bloques, preferencias y totales coherentes con la copia, persistencia tras reinicio y nueva exportación verificada.
- **AC-05:** Documentación/enlaces correctos, secretos/copias/capturas fuera de Git, sin commit/push ni eliminación de app/cliente/clave anterior.

## Riesgos y límites

No es una actualización de la app anterior. Android aísla almacenamiento, permisos y autorización por identidad. La copia excluye datos de sesión/cachés, que deben obtenerse de nuevo. Ambas apps conservaban capacidad HOME y receptores de horario; no se cambió la app HOME predeterminada ni se garantiza ausencia de interacción si se ejecutan simultáneamente. La clave nueva es necesaria para futuras actualizaciones: debe respaldarse junto a su configuración privada. La instalación anterior se retiró con autorización el 2 Nov 2026; se conservan APK, firma, cliente OAuth y copias como recuperación.
