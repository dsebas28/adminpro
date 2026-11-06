# AdminPro — Decisiones técnicas

Registro de decisiones y su justificación. Estados: **Propuesta** (espera aprobación) y **Aprobada**.
Las preguntas P1 a P9 están en [docs/fase-1-analisis-y-arquitectura.md](docs/fase-1-analisis-y-arquitectura.md#8-riesgos-y-dudas).

Todas están **Aprobadas** desde el 2026-11-05, cuando se aceptaron las recomendaciones de la Fase 1.

## Arquitectura

**D-01. Monolito modular.**
Para un equipo de una persona y una sola empresa, los microservicios agregan red, despliegue y consistencia distribuida sin beneficio. Los módulos separados por paquete permiten extraer uno después si hiciera falta.

**D-02. Maven como herramienta de construcción.**
Ya está instalado, es el estándar más común en empresas Java y su configuración declarativa es fácil de leer.

**D-03. Spring Boot 4.1.2 en lugar de 3.x.**
La línea 3.x perdió el soporte OSS el 30 de junio de 2026. Empezar hoy sobre ella significa no recibir parches de seguridad.

**D-04. Angular 22 con Vitest.**
Angular 22 es la versión con soporte activo y cumple "18+". Vitest es el corredor por defecto del CLI, así que evita configuración adicional.

## Base de datos

**D-05. Claves primarias `BIGINT` identity.**
Son compactas, rápidas de indexar y fáciles de leer. Los UUID solo se usan donde adivinar el valor sería un riesgo, como `storage_key` de los adjuntos.

**D-06. Estados en `VARCHAR` con `CHECK`, no `ENUM` de PostgreSQL.**
Agregar o quitar valores a un `CHECK` es una migración simple, mientras que modificar un `ENUM` es más rígido. Se mapea directo con `@Enumerated(EnumType.STRING)`.

**D-07. Borrado lógico con `activo`, sin `deleted_at`.**
Tener ambos sería información redundante. El cuándo y el quién ya están en `updated_at`, `updated_by` y `auditoria`. La unicidad incluye inactivos para reactivar registros y no duplicar personas.

**D-16. Datos de prueba como migración repetible `R__`.**
Una migración versionada de datos quedaría antes de las migraciones de esquema futuras y Flyway la rechazaría por fuera de orden. Una `R__` corre después de las versionadas y, con `ON CONFLICT`, es idempotente. Su carpeta solo se incluye en los perfiles `dev` y `demo`.

**D-20. Tablas de solo inserción protegidas con trigger.**
`auditoria` y `documento_historial` rechazan UPDATE y DELETE en la base de datos, no solo en el código. Un registro de auditoría que la aplicación pueda alterar no prueba nada. TRUNCATE no se bloquea, para poder restaurar el demo.

**D-21. Búsqueda con `f_normalizar` e índices de trigramas sobre expresiones.**
`unaccent` no es IMMUTABLE y no se puede indexar directamente, por eso se envuelve fijando el diccionario. Un solo índice GIN por tabla sirve al filtro `q`, con la condición de que el repositorio use la misma expresión que el índice.

**D-22. Fechas de vencimiento relativas en los datos de prueba.**
El vencimiento se calcula desde el día de la carga para que el dashboard siempre muestre alertas. `fecha_documento` es fija porque el consecutivo lleva el año.

## Seguridad

**D-08. Refresh token en cookie `HttpOnly` y access token en memoria.**
Un ataque XSS no puede leer la cookie, y el token de acceso no queda en `localStorage`. Servir frontend y API desde el mismo origen permite `SameSite=Strict`, que además protege contra CSRF.

**D-10. Refresh tokens con hash SHA-256, rotación y familias.**
Si se filtra la base de datos, los hashes no sirven como tokens. SHA-256 basta porque el token es aleatorio de alta entropía, a diferencia de una contraseña. Reutilizar un token ya rotado revoca toda su familia, porque indica robo.

**D-14. JWT con HS256 usando el soporte OAuth2 Resource Server de Spring.**
Un solo servicio firma y valida, así que no hace falta clave asimétrica. Usar el decodificador de Spring evita escribir a mano un filtro de validación, una fuente habitual de errores. Los permisos van en el token para no consultar la base de datos en cada petición.

**D-17. Un rol por usuario.**
El sistema define 4 roles fijos y ningún caso del MVP requiere combinarlos. Una clave foránea simplifica el modelo y las pruebas de la matriz. Pasar a N:M después es una migración acotada.

## Lógica de negocio

**D-09. Concurrencia optimista con `version`.**
Evita que dos personas se pisen los cambios sin bloquear filas durante la edición. `@Version` de JPA lo resuelve, y el usuario recibe un 409 con un mensaje claro.

**D-11. Auditoría explícita con `AuditService` en la misma transacción.**
Envers crea una tabla por entidad y no da un registro unificado de quién, qué y cuándo. Un servicio explícito guarda JSON de antes y después en una sola tabla, y permite excluir campos sensibles. Si el cambio falla, la auditoría se revierte con él.

**D-12. Consecutivo con tabla y `INSERT ... ON CONFLICT DO UPDATE ... RETURNING`.**
Una secuencia nativa deja huecos al hacer rollback y reiniciarla cada año requiere DDL. Con una fila por tipo y año, el bloqueo de esa fila serializa la operación hasta el commit: sin duplicados, sin huecos y con reinicio anual automático. El costo es serializar solo la creación de documentos, algo aceptable en este volumen. El año se toma en `America/Bogota`.

**D-13. Validación de adjuntos con Apache Tika y lista blanca.**
La extensión y el `Content-Type` los controla el cliente, mientras que Tika inspecciona el contenido real. Los archivos se guardan con nombre UUID y se descargan con `Content-Disposition: attachment` y `X-Content-Type-Options: nosniff`, para que nunca se ejecuten en el navegador.

## Backend base

**D-23. Manejador global que hereda de `ResponseEntityExceptionHandler`.**
Así los errores propios de Spring MVC (404, 405, JSON mal formado) también salen en Problem Details y en español, no solo las excepciones de negocio. Los errores 500 y de integridad responden un mensaje genérico para no filtrar detalles internos.

**D-24. Spring Security se agrega en la Fase 4, no en la 3.**
Incluirlo antes bloquearía Swagger y obligaría a escribir una configuración temporal. Al agregarlo, `AccessDeniedException` debe responder 403: el manejador genérico la convertiría en 500.

**D-25. Variables de conexión sin valor por defecto.**
Si falta `DB_URL`, `DB_USER` o `DB_PASSWORD`, la aplicación no arranca. Es preferible un fallo inmediato a conectarse por error con una configuración insegura.

## Frontend

**D-15. Chart.js con ng2-charts como única librería de gráficos.**
El MVP y los informes solo necesitan barras, dona y líneas, y Chart.js las cubre con poco peso. ng2-charts 11 declara compatibilidad con Angular 22. ECharts es más potente, pero ese poder no se usa aquí.

## Informes (Iteración 4)

**D-18. OpenPDF en lugar de JasperReports.**
Los informes son tablas con encabezado y totales. OpenPDF se programa directo en Java, sin plantillas ni diseñador externo, y es más liviano.

## Sin Python

**D-19. No se incluye Python.**
Ningún requerimiento lo necesita: Java cubre informes, validación de archivos y tareas programadas.
