# Family Dashboard — versión actual para Amazon Fire

- **Estado:** `implemented`
- **Tipo:** versión vigente; incluye copias y migración autorizada a identidad Android propia
- **Última actualización:** 2026-11-02

## Propósito y alcance

Family Dashboard adapta [Planify Dashboard](https://github.com/eusonlito/Planify-Dashboard) para Amazon Fire HD 8 de 7.ª generación (KFDOWI, API 22, 1280 × 800 horizontal). Conserva la base Android, Open-Meteo, Calendario, reloj nocturno y caché; utiliza Google Calendar/Tasks y sondeo local Samsung para los datos familiares y TV.

Este documento consolida las reglas vigentes de selección de calendarios/listas, frecuencia de actualización, Inicio, Tareas, TV, Ajustes y autorización. No conserva diseños intermedios sustituidos: calendario mensual en Inicio, card separada de rutinas, reloj de paletas, barras de horario, importación remota TV o gráficos TV anteriores. El historial Git de los archivos rastreados sigue disponible; esta consolidación no modifica commits existentes.

No incluye rebranding del icono, cambios de DNS/Branding, activación de Device Owner ni eliminación de componentes con consumidores tangenciales. La etapa posterior autorizada cambió paquete/firma y migró mediante copia, preservando la instalación anterior durante la validación; después se retiró con autorización. El cliente OAuth adicional requirió intervención de la persona usuaria. Estado real en [identidad Android](../android-identity/TASKS.md).

## Identidad, plataforma y fuentes

- **FR-01:** El nombre visible es **Family Dashboard** en Android, login y sitio informativo; se conservan créditos/licencias de Planify y terceros. Nueva identidad autorizada: `com.joausaga.familydashboard` en applicationId, namespace y paquetes, con firma propia fuera del repositorio. Claves/modelos persistidos y formato de copia no cambian. La instalación anterior se retiró tras validar la migración con autorización; se conservan su APK, firma, cliente OAuth y copias como recuperación. Migración y validación según [android-identity](../android-identity/SPEC.md).
- **FR-02:** Compatibilidad de referencia API 22/Fire OS con geometría escalada desde 1920 × 1200 y tipografía independiente; pantalla completa legacy, alternativas de alarmas y superposición para API antigua. No se garantiza toda la familia Amazon Fire.
- **FR-03:** Autorización Google de solo lectura (`calendar.readonly`, `tasks.readonly`) mediante Play Services. Token solo en memoria, renovación al arrancar o recibir 401 sin borrar caché, y revocación intentada al cerrar sesión. Sin login Planify ni backend propio.
- **FR-04:** Calendar y Tasks usan paginación. Calendar incluye calendarios ocultos, expande recurrentes, omite cancelados, aplica zona local y conserva fin cuando existe. Las ocurrencias con `recurringEventId` son rutinas. Tasks excluye completadas, conserva lista y vencimiento; tareas sin título útil no se muestran.
- **FR-05:** Las selecciones visibles se guardan por cuenta usando el ID completo del calendario principal. Todas las fuentes nuevas se activan inicialmente; las conocidas conservan preferencia y las retiradas se reconcilian. Se permite desactivar todas. Los IDs completos no se sustituyen por hashes al persistir selección.
- **FR-06:** Filtros de calendarios afectan eventos y rutinas en Inicio/Calendario; filtros de listas afectan tareas en Inicio/Tareas. No alteran clima ni TV. Antes de identificar cuenta/selección, no se muestra contenido Google de otra selección. Ocultar una fuente filtra su presentación, no modifica permisos en Google.
- **FR-07:** Actualización calendario/tareas configurable en 5, 10, 20, 30, 40, 50 o 60 minutos (5 inicialmente), guardada y aplicada al seleccionar. Semanas cargadas bajo demanda con copia local y límite de 12 recientes. Clima cada 30 minutos. Sin hora válida no se consultan rangos remotos.
- **FR-08:** La caché conserva la última descarga válida ante fallos; escrituras atómicas. Sincronización manual refresca contenido, semanas cargadas, clima y TV. Resultados antiguos no pisan un estado de sincronización más reciente. Al recuperar red se intenta actualizar.

## Navegación e Inicio

- **FR-09:** Barra lateral con Hoy, Calendario, Tiempo, Tareas, TV, Sincroniza, Ajustes y Apagar; iconos/etiquetas, seleccionado y respuesta al pulsar. Indicador Conectado/Sincroniza/Sin conexión/Error de sync, sin reloj lateral. Tras 120 s sin interacción fuera de Inicio se vuelve a Hoy; Atrás no cierra el launcher.
- **FR-10:** Inicio conserva tres filas de aproximadamente 24,5%, 50,5% y 25%: resumen y siguiente actividad; agenda de hoy más cuatro días; Tareas y TV. Es de consulta, salvo la acción de configuración de TV. No hay edición de eventos ni tareas.
- **FR-11:** La tarjeta de hora/fecha/clima usa hora local, no `is_day`: 06–07 aclarado; 07–12 celeste matinal; 12–18 oscurecimiento; 18–21 transición nocturna; 21–06 azul oscuro. Texto/iconos adaptativos con contraste ≥4,5:1 en ambos extremos del degradado y en las celdas. En las cinco previsiones, texto e iconos son blancos desde las 12:00 inclusive hasta las 21:00 exclusive; su fondo azul se oscurece lo necesario para mantener ≥4,5:1. Fuera de ese rango conservan contenido adaptativo sobre blanco al 20%. Esta excepción no modifica reloj, fecha, clima actual ni degradado general. Funciona sin previsión y sin saltos de etapa perceptibles en el fondo general.
- **FR-12:** Clima actual con temperatura 62 sp, icono 64 dp, mín./máx. y probabilidad de lluvia siempre visible; cinco previsiones con icono 48 dp, máxima 24 sp y mínima 20 sp. Reloj 164 sp y fecha 36 sp. Las cifras y composición no cambian durante la limpieza.
- **FR-13:** Agenda y «Lo próximo»: recurrente verde; resto color de su calendario; barra y fondo tintado al 16%, texto oscuro. Se muestran rangos horarios si hay fin, repetición para recurrentes y atenuación del pasado. «Lo próximo» incluye cuenta regresiva natural con cápsula de contraste adaptativo.
- **FR-14:** Hoy destacado con tinte, franja roja y número en círculo rojo; días sin actividades muestran «Libre». Listas con desbordamiento muestran «+N más». No hay card independiente de rutinas semanales: recurrentes y eventos comparten agenda.
- **FR-15:** Tareas de Inicio: la próxima por vencimiento es protagonista; hasta dos adicionales en tiras y «+N más» para el resto. Título, lista, cuenta atrás e icono por palabras clave; urgencia roja vencida/hoy, ámbar 1–2 días, verde posterior y neutro sin fecha. Sin casillas. Carga, error, sin listas seleccionadas y «Todo al día» se distinguen.
- **FR-16:** TV de Inicio muestra estado, total de hoy, diferencia frente al promedio de cinco días completos anteriores (ceros incluidos) y tendencia de esos cinco días, de antiguo a reciente, con máximo destacado. No dibuja el promedio. OFF confirmado/inferido se presenta «Apagada»; error interno como «Sin confirmación». Sin configuración ofrece abrir Ajustes. Geist tabular, identidad violeta y tamaño informativo ≥20 sp.

## Calendario, Tiempo, Tareas y noche

- **FR-17:** Calendario conserva dos semanas navegables verticalmente y descarga semanas vecinas. Cada día muestra clima, eventos y rutinas; personas si un dato compatible las incluye. Los días pasados se atenúan y «Volver a hoy» regresa a la semana actual.
- **FR-18:** Tiempo conserva dos páginas de previsión hasta 14 días, detalle horario de un día, navegación por gesto y Meteocons. Máximas/mínimas se derivan de horas cuando hay suficientes datos. Barras de lluvia: alto por cantidad, relleno por probabilidad, sin barra por debajo de 0,1 mm; hora actual indicada. No se cambia este comportamiento en la limpieza.
- **FR-19:** Tareas muestra resumen sin filtros de pendientes/vencidas/hoy/esta semana; esta semana cuenta mañana hasta domingo. Vista inicial «Por fecha», alternativa «Por lista». Filtros combinables por lista y día, línea temporal de 14 días con vencidas cuando existan y selección reversible al volver a tocar.
- **FR-20:** Secciones por fecha: vencidas, hoy, mañana, esta semana después de mañana, más adelante y sin fecha; tres columnas agrupadas. Por lista, una columna por lista, desplazamiento horizontal si hay más de tres. Orden por vencimiento y orden original en empate. Una fila expandida como máximo; sin resultados permite limpiar filtros. Iconos/urgencia compartidos con Inicio.
- **FR-21:** Reloj nocturno: hora, fecha y clima blancos sobre negro, brillo 1/255. Fuera de horario, tocar enseña el dashboard cinco minutos. La luna activa noche cuando corresponde o intenta apagar pantalla según la configuración y capacidades del dispositivo. El apagado por Device Owner no se presume disponible en la Fire.

## TV: sondeo e historial

- **FR-22:** Fuente activa: GET de solo lectura al endpoint Samsung/Tizen de la IP configurada. Sin IP no sondea y muestra cómo configurar. Frecuencias 1, 3, 5, 10, 15, 30, 45, 60 min (1 inicialmente). Consulta mientras la app está activa; no requiere OAuth de Sheets/Drive ni transmite la IP a esos servicios.
- **FR-23:** Estado ON abre intervalo y OFF lo cierra; error/timeout se registra OFF inferido. Observaciones iguales no duplican transiciones; timestamps iguales conservan orden y pueden producir duración cero. La primera observación local no atribuye tiempo anterior desconocido. Estado sin observación fresca en 6 h se marca desactualizado.
- **FR-24:** Historial TV persistente sin borrado automático, compatible con registros importados previamente y sus metadatos. Los totales reparten tiempo por hora/día/mes en la zona horaria de la ubicación elegida para el tiempo, incluyendo fronteras de semana y días de duración no uniforme. Al cambiar ubicación/zona se recalcula todo el historial sin modificar registros. Se conserva la zona conocida sin conexión; si aún no se conoce una válida, se utiliza provisionalmente la del dispositivo. La presentación de fechas, horas y comparaciones coincide con los agregados. No se cambia la zona de Android/Google Calendar ni se elimina historial al cerrar sesión. Requisitos y pruebas en [tv-weather-timezone](../tv-weather-timezone/SPEC.md).
- **FR-25:** Pantalla TV: héroe con estado/total de hoy, sesión si encendida, promedio y antigüedad de consulta; violeta más vivo cuando encendida. En el área de análisis, gráfico horario arriba con altura ampliada; heatmap horizontal a ancho completo en la fila central; comparativas semana/mes y totales mensuales lado a lado en una última fila de altura reducida. Actividad de 24 horas en gráfico de barras, una por hora, con etiqueta compacta y legible del consumo en el extremo superior, separada de la barra, solo si hubo consumo; selección táctil, horas del eje y hora actual; horas futuras atenuadas. Mapa de calor lunes–domingo con rangos 4/8/26 semanas (8 inicialmente), cinco niveles, meses en una banda separada sobre las celdas, número del día en esquina superior izquierda gris y consumo positivo centrado; pico y detalle completo del día; accesibilidad anuncia fecha/consumo. Historial extenso abre en lo reciente.
- **FR-26:** Semana y mes comparan el mismo tramo del periodo anterior; sin referencia se indica. Totales mensuales en barras horizontales de reciente a antiguo, mes actual destacado y scroll si hace falta. Falta de respuesta y apagado inferido se explican en el héroe; sin IP incluye «Ir a Ajustes». No se reintroducen los gráficos anteriores.

## Ajustes

- **FR-27:** Índice fijo con icono, nombre y resumen; Contenido inicial. Detalle desplazable, una sección a la vez; no repite título «Ajustes» ni cabecera de sección. Destinos: Contenido, Pantalla, Servicios, Dispositivo.
- **FR-28:** Contenido: calendarios y listas en una columna, contador N de M, filas tocables e interruptores accesibles. Calendarios activos primero conservando orden dentro de cada grupo; color Google en punto, gris cuando inactivo; nombres inactivos atenuados. Carga/error con reintento y sin scroll interno anidado.
- **FR-29:** Pantalla: horario activable por días laborables/fin de semana, explicación de reloj nocturno, dos botones Enciende/Apaga, esfera Material 3 vertical ampliada 1,7×, 12 horas y AM/PM. Horas/minutos escalonados con campo activo oscuro/blanco e inactivo transparente/gris; Cancelar no guarda y Aceptar valida ≥15 min. Sin barras de horario. Brillo mantiene deslizador de 5–100% por pasos de 5 y control Automático; al desactivarlo recupera el valor manual recordado en la pantalla.
- **FR-30:** Servicios: ubicación con campo y Buscar alineados y estados de búsqueda; frecuencia Google guardada al seleccionar; TV con IP, sondeo, Probar conexión y Guardar en la misma fila. Prueba exige IP válida, no escribe historial y muestra resultado debajo. Guardado visible temporalmente y errores claros.
- **FR-31:** Dispositivo: cuenta Google y correo obtenido del ID del calendario principal, con estados de carga/no disponible; antigüedad de sincronización por servicio; Wi-Fi con detalles; acciones Wi-Fi, Ajustes de Android y Cerrar sesión. Confirmación propia de cierre con explicación, Cancelar y acción destructiva, sin cierre al tocar fuera. No ofrece la antigua acción de apagar/reiniciar.
- **FR-34:** Dispositivo incluye «Copia de seguridad»: exportación JSON sin contraseña mediante selector Android y restauración selectiva de historial TV/configuración/ambos, con vista previa y confirmación de reemplazo. No exporta/importa autorización Google ni modifica identidad. Validación completa, verificación de archivo escrito y recuperación transaccional según [backup-restore](../backup-restore/SPEC.md).

## Limpieza y compatibilidad

- **FR-32:** Retirar componentes sin llamadas y ramas antiguas autorizadas: reloj de paletas, texto animado, login/backend Planify, importación Sheets/Drive y sus pruebas exclusivas. Trasladar pruebas útiles de repositorios a Google. Conservar código activo, consumidores indirectos, modelos persistidos, campos heredados, fuentes, Meteocons e infraestructura opcional Android.
- **FR-33:** Documentación actual en README, docs y esta especificación. Retirar documentación/instaladores del XORO y diseños sustituidos, preservando reconocimiento de Planify y licencias. No modificar DNS, `CNAME`, Branding ni publicar sin permiso. Cambios de identidad y cliente OAuth solo en el alcance autorizado de [android-identity](../android-identity/SPEC.md); sin commit/push hasta nueva petición.

## Criterios de aceptación

| ID | Verificación |
|---|---|
| AC-01 | Nombre Family Dashboard en APK/login/sitio; nueva identidad/firma según android-identity, créditos/licencias presentes; app anterior preservada durante la migración y retirada después con autorización. |
| AC-02 | Pruebas de APIs Google, filtros por cuenta, caché, 401, reinicio y limpieza pasan sin una API Planify. |
| AC-03 | Las pruebas de paleta comprueban 1440 minutos, horarios y contraste; captura comparativa 09/15/18/21 disponible. |
| AC-04 | Tareas, TV y Ajustes conservan funciones y valores; pruebas de lógica, horarios y formatos pasan. |
| AC-05 | Historial importado TV y metadatos sobreviven reinicio y sondeo local; datos/cache del dispositivo no se borran. |
| AC-06 | No quedan llamadas ni definiciones del backend/importador retirados; referencias documentales locales resuelven y no describen como vigente el hardware original. |
| AC-07 | Release compila y se verifica en Fire: migración inicial en paralelo según android-identity; actualizaciones posteriores del nuevo paquete con su misma firma, sin desinstalar. |
| AC-08 | La persona usuaria comprueba legibilidad a 2–3 m y manejo con el dedo del selector horario; no se da por validado mediante captura. |

## Límites conocidos

- El icono por palabra clave de tarea es heurístico; Google Tasks no proporciona color de lista en este modelo.
- Los IDs enteros derivados siguen usándose para agrupación visual; los filtros persistidos usan los IDs completos.
- El texto secundario de eventos sobre algunos tintes puede quedar por debajo de 4,5:1. No se cambia la paleta de eventos en esta limpieza; la garantía exhaustiva corresponde a la cabecera horaria.
- Fire OS y Play Services condicionan autorización y modo de pantalla; ocultar barras no equivale a Device Owner.
- Producción OAuth y Branding son configuración externa que debe comprobarse antes de distribuir; renombrar el APK no actualiza la consola.
