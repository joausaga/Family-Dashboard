# Diseño de copia/restauración

- `data/backup/`: DTO explícitos, codec JSON versionado con SHA-256 y lectura limitada, validación previa y coordinador transaccional comprobable con almacenamiento falso.
- `Store`: instantánea y sustitución de una lista blanca de preferencias, mediante commit síncrono; no limpiar todas las preferencias. Selecciones por cuenta conservan sus claves existentes portables.
- Persistencia: diario privado con configuración e historial anteriores antes de escribir; rollback ante fallo y recuperación al arrancar antes de crear los repositorios. El diario nunca entra en una exportación.
- ViewModel: instantánea bajo exclusión con el sondeo TV, importación solo en memoria hasta confirmar, restauración en IO y recarga posterior de repositorios/configuración. Invalidar respuestas Google anteriores a la restauración y no permitir cambios durante aplicación/recuperación pendiente.
- Activity: Storage Access Framework (`CreateDocument`/`OpenDocument`), streams de ContentResolver; no rutas de almacenamiento compartidas fijas ni nuevos permisos. No conservar URI de forma persistente.
- UI: bloque en Dispositivo, aviso privado de exportación, selector de bloques y confirmación de reemplazo propios de Compose para respetar la densidad. El estado pendiente reside en ViewModel; Atrás cancela antes de confirmar y no interrumpe una aplicación en curso. Evitar regreso automático a Inicio mientras una operación está pendiente.
- Exportación verificada por relectura del documento y comprobación del contenido; si falla, explicar que el archivo no se verificó y no borrarlo automáticamente del proveedor.

En esta etapa se conserva toda la identidad Android y el esquema del historial existente. El JSON lleva un identificador lógico de formato y no depende del paquete instalado, lo que permitió la migración posterior validada en [android-identity](../android-identity/SPEC.md). Esta etapa no hizo cambios externos en Cloud, permisos de Google ni firma.
