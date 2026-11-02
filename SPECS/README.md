# Especificaciones de funcionalidades

La fuente de verdad de **Family Dashboard para Amazon Fire** es [current-version/SPEC.md](current-version/SPEC.md), con su [diseño](current-version/DESIGN.md) y [validación](current-version/TASKS.md). Consolida las reglas actuales; los diseños intermedios sustituidos se retiraron con autorización para no mantener instrucciones contradictorias. Se reconoce la base de Planify en el [README](../README.md).

Este directorio contiene las especificaciones que guían la implementación de funcionalidades nuevas y adaptaciones de funciones existentes mediante **Spec-Driven Development (SDD)**. Cada trabajo tiene su propio subdirectorio y mantiene juntos requisitos, diseño y tareas de implementación.

## Estructura

Usa un nombre corto en `kebab-case` que identifique la funcionalidad o adaptación:

```text
SPECS/
  README.md
  _template/
    SPEC.md
    DESIGN.md
    TASKS.md
  nombre-de-la-funcionalidad/
    SPEC.md
    DESIGN.md
    TASKS.md
```

`_template/` es una plantilla, no una especificación implementable. No crees carpetas contenedoras genéricas como `features/` o `adaptations/`: cada subdirectorio directamente bajo `SPECS/` corresponde a una funcionalidad o adaptación concreta.

Para iniciar una especificación, copia la plantilla y reemplaza los marcadores:

```sh
cp -R SPECS/_template SPECS/nombre-de-la-funcionalidad
```

Un mismo trabajo puede ser una funcionalidad completamente nueva o la adaptación de una funcionalidad existente a otra plataforma, dispositivo, fuente de datos o flujo de uso. Usa una sola especificación cuando ambos casos formen un resultado que pueda aceptarse y probarse de forma conjunta; crea especificaciones separadas cuando puedan planearse y entregarse independientemente.

## Flujo SDD

1. **Definir el resultado en `SPEC.md`.** Describe el problema, usuarios, alcance, restricciones, requisitos funcionales identificados (`FR-…`) y criterios de aceptación observables (`AC-…`). Evita describir la solución como si fuera un requisito.
2. **Resolver las incógnitas.** Registra preguntas abiertas, dependencias y riesgos. No marques la especificación como lista mientras una decisión pendiente impida definir o verificar el comportamiento esperado.
3. **Diseñar en `DESIGN.md`.** Explica cómo encaja la solución en la arquitectura actual, qué componentes y datos cambian, cómo se manejan errores y estados límite, y qué decisiones técnicas se tomaron.
4. **Dividir el trabajo en `TASKS.md`.** Cada tarea debe enlazar a uno o más requisitos y describir su verificación. Ordena el trabajo por dependencias; incluye pruebas y validación en el dispositivo cuando apliquen.
5. **Implementar contra la especificación.** Si cambia el comportamiento, actualiza primero `SPEC.md` y luego el diseño o las tareas afectadas. No aceptes código que satisfaga tareas pero incumpla criterios de aceptación.
6. **Verificar y cerrar.** Ejecuta las pruebas apropiadas, registra resultados, marca criterios y tareas completados y cambia el estado a `validated` solo cuando exista evidencia para todos los criterios de aceptación.

## Estados

Usa uno de estos estados en `SPEC.md`:

- `draft`: definición inicial en elaboración.
- `specified`: requisitos y criterios de aceptación listos para diseñar o implementar.
- `implementing`: trabajo de implementación en curso.
- `implemented`: código terminado, con validación pendiente.
- `validated`: criterios de aceptación comprobados.
- `deferred`: trabajo aplazado; documenta el motivo y las condiciones para retomarlo.

Las especificaciones son la fuente de verdad para alcance y comportamiento esperado. Los planes generales del proyecto pueden enlazarlas, pero no deben duplicar sus requisitos detallados.
