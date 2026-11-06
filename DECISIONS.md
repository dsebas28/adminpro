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

**D-26. `@PreAuthorize` evaluado antes de leer el cuerpo.**
Spring resuelve `@Valid` antes de la seguridad de métodos, así que un rol sin permiso recibía 400 con detalles de validación en vez de 403. Un interceptor evalúa la misma anotación antes de leer el cuerpo: la regla sigue en un solo lugar y aplica a todo controlador nuevo.

**D-27. Los fallos de login y la revocación por robo confirman en su propia transacción.**
Esos cambios deben sobrevivir al 401 que viene después, y una excepción dentro de la transacción los revertiría. Por eso `AuthService` no es transaccional en esos flujos y `RefreshTokenService` devuelve un `Optional` vacío en lugar de lanzar la excepción.

**D-28. Gracia de 10 segundos en la rotación, solo si la familia sigue activa.**
Dos pestañas pueden renovar a la vez con el mismo token, y sin gracia eso cerraría la sesión como si fuera un robo. Comprobar que la familia siga activa impide usar la gracia después de un logout, sin agregar columnas.

**D-29. Los campos de login y `updated_at`/`updated_by` no suben la `version`.**
Un login no es una edición: si subiera la versión, invalidaría el formulario que un Administrador tenga abierto. Se excluyen con `@OptimisticLock(excluded = true)`.

**D-30. Límite de BCrypt validado en bytes.**
BCrypt admite 72 bytes, no 72 caracteres, y una tilde ocupa dos bytes en UTF-8. `PasswordPolicy` lo valida para responder 400 y no 500.

**D-31. `forward-headers-strategy: native` y Nginx sobrescribe `X-Forwarded-For`.**
El límite de intentos usa la IP del cliente. Tomcat solo confía en esa cabecera si viene de una red interna, pero la red de Docker lo es, así que el proxy debe usar `proxy_set_header X-Forwarded-For $remote_addr` y nunca conservar lo que envió el cliente. El backend no debe quedar expuesto al público sin ese proxy.

## Lógica de negocio

**D-40. Catálogos con una base genérica.**
Departamentos, cargos y categorías tienen las mismas reglas. `CatalogoService` y `CatalogoController` las escriben una vez, y cada catálogo solo define cómo se crea y cuándo está en uso. El permiso de lectura de cada uno se resuelve con `#root.this.permisoLectura()`.

**D-41. DOCX y XLSX se reconocen leyendo la estructura del ZIP.**
`tika-core` solo los ve como ZIP, y el módulo completo de parsers agrega decenas de dependencias. Basta con leer los nombres de las entradas, sin descomprimirlas: `[Content_Types].xml` más `word/document.xml` o `xl/workbook.xml`. Un ZIP cualquiera se rechaza.

**D-42. Reactivar a un empleado no reactiva su usuario.**
Devolverle el acceso a alguien debe ser una decisión explícita de un Administrador, no un efecto secundario.

**D-43. JaCoCo hace fallar la compilación si un servicio baja del 70 %.**
Así el mínimo de cobertura se cumple solo, en vez de depender de revisarlo a mano. Une las pruebas unitarias y las de integración.

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

**D-24. Spring Security se agregó en la Fase 4, no en la 3.**
Incluirlo antes habría bloqueado Swagger y obligado a escribir una configuración temporal. `AccessDeniedException` tiene su propio manejador para responder 403: el genérico la convertiría en 500.

**D-25. Variables de conexión sin valor por defecto.**
Si falta `DB_URL`, `DB_USER` o `DB_PASSWORD`, la aplicación no arranca. Es preferible un fallo inmediato a conectarse por error con una configuración insegura.

## Frontend

**D-15. Chart.js con ng2-charts como única librería de gráficos.**
El MVP y los informes solo necesitan barras, dona y líneas, y Chart.js las cubre con poco peso. ng2-charts 11 declara compatibilidad con Angular 22. ECharts es más potente, pero ese poder no se usa aquí.

**D-32. Identidad visual basada en el sello de radicado.**
El azul de tinta sobre papel gris frío y la numeradora vienen del trabajo real de una oficina colombiana, no de una plantilla. La única pieza llamativa es el sello del login; el resto de la interfaz es sobria para que dominen los datos.

**D-33. Fuentes empaquetadas con Fontsource, no desde Google Fonts.**
Se sirven desde el mismo origen, lo que permite una política CSP estricta, evita enviar datos a un tercero y no depende de una red externa.

**D-34. Signals y componentes con detección de cambios OnPush.**
Angular 22 funciona sin zone.js: los signals dicen exactamente qué actualizar. El estado de la sesión es un signal que guards, menú y directivas leen sin suscripciones manuales.

**D-35. Una sola renovación compartida entre peticiones simultáneas.**
Si varias peticiones reciben 401 a la vez y cada una renovara, el backend vería un token reutilizado y cerraría la sesión. `AuthService.renovar()` comparte la petición en curso.

**D-36. Elemento `dialog` nativo para los modales.**
El navegador ya mantiene el foco dentro, cierra con Escape y devuelve el foco al salir. Reimplementarlo sería más código y peor accesibilidad.

**D-37. Política CSP sin scripts en línea.**
Limita el daño de un ataque XSS. Por eso el tema inicial va en `tema-inicial.js` y se apagó `inlineCritical`, que depende de un manejador `onload` en línea. Los estilos en línea siguen permitidos porque Angular los necesita.

**D-38. Clase `Listado<T>` para todos los listados.**
Paginación, orden, filtros y carga se escriben una sola vez. Usa `switchMap` para cancelar la consulta anterior: si el usuario escribe rápido, una respuesta vieja no pisa a la nueva.

**D-39. Barras de un solo color para los documentos por estado.**
Es una sola serie en el orden fijo del flujo. Una dona dificulta comparar y pierde ese orden. Las cifras generales van como números, no como gráfico, y el gráfico trae valores directos y una tabla para lectores de pantalla.

## Informes (Iteración 4)

**D-18. OpenPDF en lugar de JasperReports.**
Los informes son tablas con encabezado y totales. OpenPDF se programa directo en Java, sin plantillas ni diseñador externo, y es más liviano.

## Sin Python

**D-19. No se incluye Python.**
Ningún requerimiento lo necesita: Java cubre informes, validación de archivos y tareas programadas.
