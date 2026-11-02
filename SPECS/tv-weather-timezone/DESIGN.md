# Diseño: zona TV de la ubicación meteorológica

1. Añadir campos opcionales `timezone` a `Location`, `GeoResult` y `Forecast`. Persistirlos con los modelos existentes y conservar compatibilidad con JSON antiguo. Asociar el pronóstico a las coordenadas solicitadas, no a las coordenadas de la celda redondeada del proveedor.
2. Resolver la zona con funciones puras: pronóstico válido asociado a la ubicación, zona guardada de la ubicación y finalmente zona del dispositivo. Ignorar zonas IANA inválidas y respuestas de otra ubicación. La geocodificación permite conocer la zona antes de descargar el pronóstico.
3. `UiState` expone la zona TV y la hora TV derivada del mismo instante que `now`. Un único helper de presentación calcula agregados, fecha/hora e instante; se reutiliza cuando cambia reloj, historial o ubicación/zona. Guardar la zona aceptada en la ubicación para uso sin red.
4. Inicio utiliza la fecha TV para tendencia/promedio; la pantalla TV recibe `ZonedDateTime`, que conserva juntos zona e instante absoluto, sin reinterpretarlo con la zona del dispositivo.
5. El historial sigue almacenando milisegundos epoch. Reagrupar no modifica el historial ni sus claves de disco; un cambio de zona solo cambia los límites de calendario usados por `TvUsageCalculator`.

No se altera la zona horaria de Android, Google Calendar, cabecera horaria general ni pronóstico. Una caché meteorológica antigua puede seguir mostrando el clima, pero sin metadatos suficientes no se usa para atribuir una zona a otra ubicación.
