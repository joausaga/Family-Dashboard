# Zona horaria de TV según la ubicación del tiempo

- **Estado:** `implemented`
- **Fecha:** 2026-11-02

## Resultado y decisiones confirmadas

Retirar la zona fija de TV y utilizar la zona IANA de la ubicación meteorológica seleccionada, sin cambiar el reloj del dispositivo ni la zona del calendario Google. La persona usuaria confirmó recalcular todo el historial en la nueva zona y utilizar provisionalmente la zona del dispositivo si no se conoce la zona de la ubicación.

## Requisitos

- **FR-01:** Leer `timezone` de geocodificación y pronóstico Open-Meteo, validar con `ZoneId` y asociar la zona guardada a la ubicación meteorológica. No fijar una ciudad ni un offset UTC constante.
- **FR-02:** Al cambiar la ubicación, aplicar su zona y recalcular todos los agregados TV inmediatamente; al obtener posteriormente una zona válida, recalcular sin esperar al siguiente sondeo. No reescribir transiciones, intervalos ni metadatos del historial.
- **FR-03:** Si falta una zona válida, usar la del dispositivo; conservar la zona conocida de la ubicación para reinicios sin conexión y aceptar preferencias/cachés antiguas que carezcan de los nuevos campos.
- **FR-04:** Los rótulos de fecha/hora, «hoy», semanas, meses, comparaciones y hora actual TV deben usar la misma zona que sus agregados, tanto en Inicio como en la pantalla TV. La antigüedad de consulta y las observaciones siguen siendo instantes absolutos.
- **FR-05:** Una respuesta meteorológica de una ubicación anterior no debe aplicar su zona a la nueva ubicación. No cambiar firma, paquete, OAuth, controles, reloj general ni datos de usuario.
- **FR-06:** Actualizar README, arquitectura, instalación y especificación vigente retirando la limitación de zona fija. Instalación en tablet solo tras autorización específica.

## Aceptación

- **AC-01:** Pruebas de resolución de zona válida/inválida, fallback, preferencias/caché antiguas, zona guardada sin red y respuestas de otra ubicación.
- **AC-02:** Pruebas con el mismo instante en distintas fechas/meses/semanas según ubicación; cambio de zona recalcula sin alterar historial ni duración real. Cubrir cambio de horario de verano y coherencia de fecha/hora para UI.
- **AC-03:** Todas las pruebas y release compilan; no queda ciudad fija en código productivo ni documentación vigente. Cualquier ejemplo de ciudad queda limitado a pruebas.
