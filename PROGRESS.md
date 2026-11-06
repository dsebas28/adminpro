# AdminPro — Progreso

**Fase actual:** 4. Seguridad, entregada y pendiente de tu aprobación.
**Última actualización:** 2026-11-06

## Estado por fase

| Fase | Estado |
|---|---|
| 1. Análisis y arquitectura | Aprobada (P3 sigue abierta) |
| 2. Base de datos | Aprobada y validada en la imagen `postgres:16` |
| 3. Backend base | Aprobada y validada con Docker |
| 4. Seguridad | Criterio cumplido, pendiente de aprobación |
| 5. Frontend base | No iniciada |
| 6. Módulos del MVP | No iniciada |
| 7. CI y documentación | No iniciada |
| 8+. Iteraciones 2 a 4 | No iniciada |

## Decisiones tomadas al aprobar la Fase 1

La aprobación fue un "siga" sin respuestas puntuales, así que se aplicaron las recomendaciones del documento:

- P1: Spring Boot 4.1.2.
- P2: Vitest.
- P5: devolución con comentario, y quien crea un documento no lo aprueba.
- P6: matriz de permisos revisada.
- P7: se mantienen las 4 columnas de auditoría en todas las tablas.
- P8: se mantiene la regla 3 (tú confirmas la salida antes de dar algo por terminado).

## Terminado

**Documentación:** [contrato del MVP y diagrama ER](docs/fase-1-analisis-y-arquitectura.md) y [decisiones técnicas](DECISIONS.md).

**Base de datos** (`backend/src/main/resources/db/`): migraciones `V1` a `V6` y datos de prueba `seed/R__datos_prueba.sql`.

**Backend base** (`backend/`)

| Elemento | Ubicación |
|---|---|
| Proyecto Maven, Spring Boot 4.1.2, Java 21 | `pom.xml` |
| Perfiles `dev`, `demo` y `prod` | `src/main/resources/application*.yml` |
| Errores en Problem Details | `exception/` |
| Swagger, CORS y auditoría JPA | `config/` |
| `BaseEntity` y `PageResponse` | `common/` |
| Pruebas unitarias e integración | `src/test/` |

**Seguridad** (`backend/src/main/java/com/adminpro/`)

| Elemento | Ubicación |
|---|---|
| Login, refresh, logout, perfil y cambio de contraseña | `auth/` |
| Usuarios, roles y Administrador inicial | `usuarios/` |
| JWT, filtros, límite de intentos y autorización temprana | `security/` |
| Registro y consulta de auditoría | `audit/` |
| Imagen Docker | `Dockerfile` |

En la raíz: `docker-compose.yml`, `.env.example` y `.gitignore`.

## Verificación

**Fase 2.** Ejecutada por Claude con PostgreSQL 16.13 para Windows y Flyway 12.4.0:

- Migraciones limpias en base vacía, con y sin datos de prueba. Segunda ejecución sin cambios.
- 23 casos inválidos rechazados y 4 válidos aceptados.
- Consecutivo con 16 clientes, 4.800 transacciones y 30 % de rollback: 3.363 números, sin duplicados ni huecos.
- Diagrama ER contra esquema real: 14 tablas, 0 diferencias.

**Fase 3.** Ejecutada por Claude con JDK 21 y el jar real contra PostgreSQL 16.13 con locale `es-CO`:

| Prueba | Resultado |
|---|---|
| `mvn clean package` | Correcto, 9 pruebas unitarias pasan |
| Arranque en `dev` | 7 migraciones aplicadas, 26 empleados |
| Arranque en `prod` | 6 migraciones, 0 empleados, Swagger responde 404 |
| `/actuator/health` | `UP` |
| Swagger UI y `/v3/api-docs` en `dev` | 200 |
| Ruta inexistente y método incorrecto | Problem Details en español, `application/problem+json` |
| `/actuator/env` | 404, no expuesto |
| CORS | Origen permitido aceptado, otro origen rechazado con 403 |
| Sin variables de conexión | La aplicación no arranca (exit 1) |
| "TESORERÍA" contra "Tesorería" | Rechazado por duplicado |
| Comando del healthcheck de compose | exit 0 con la app arriba, exit 1 abajo |
| Sintaxis de `docker-compose.yml` | YAML válido, tras corregir un error |

**Fase 4.** `mvn verify` con PostgreSQL 16 real, ejecutado por Claude: **132 pruebas** (29 unitarias y 103 de integración).

- Matriz de permisos: 12 rutas contra los 4 roles y sin sesión, más los permisos exactos de cada rol.
- Tokens: rotación, reutilización, gracia entre pestañas, logout, token alterado y solo hash en la base de datos.
- Login: bloqueo, límite por IP, respuesta idéntica con correo inexistente y contraseñas de más de 72 bytes.
- Auditoría: quién, antes y después, sin datos sensibles, y se revierte si el cambio falla.

También se probó por HTTP contra los contenedores: cookie `HttpOnly` y `SameSite=Strict`, CORS, límite real de intentos, y `prod` sin datos de prueba con Administrador inicial.

**Validación con Docker, Fase 3** (2026-11-06, Docker 29.9.2, Compose 5.6.1, PostgreSQL 16.15 en Debian, `en_US.utf8`):

| Prueba | Resultado |
|---|---|
| `docker compose up --build` | `db` y `backend` en estado `healthy` |
| Migraciones en el contenedor | 7 aplicadas, 26 empleados |
| `/actuator/health` | `UP` |
| Swagger UI y `/v3/api-docs` | 200 |
| Usuario del contenedor backend | `adminpro` (uid 10001), sin privilegios |
| `mvn verify` | `BUILD SUCCESS`: 9 pruebas unitarias y 5 de integración con Testcontainers |

Se corrigió un choque de puertos: tu PostgreSQL local ya usa el 5433, así que AdminPro expone la base de datos en el **55432**.

## Repositorio

Historial limpio desde el 2026-11-06. Football AI Predictor se retiró a petición del usuario; su respaldo está fuera del repositorio, en `Desktop\respaldo-football-ai-predictor.bundle`. Se puede restaurar con `git clone` sobre ese archivo.

## Pendientes conocidos

- **Java:** el `PATH` apunta a Java 17; hay que apuntar `JAVA_HOME` a un JDK 21 instalado.
- **Fase 5:** Nginx debe sobrescribir `X-Forwarded-For` (ver D-31). Hoy un valor falso evade el límite de intentos, aunque el backend solo escucha en `127.0.0.1`.
- **Fase 6:** agregar a `PermisosIT` cada endpoint nuevo. Hoy cubre autenticación, usuarios, roles y auditoría.
- **Fase 6:** desactivar un empleado debe desactivar su usuario (supuesto S13).
- **Iteración 4:** limpiar periódicamente los refresh tokens vencidos.
- **Fase 6:** los adjuntos de prueba son solo metadatos. El backend debe crear archivos de ejemplo al arrancar en `dev` y `demo`, o su descarga dará 404.
- **Fase 6:** las consultas de búsqueda deben repetir la expresión exacta de los índices (ver D-21).
- **Fase 6:** la regla "fecha de ingreso no futura" se valida solo en el backend.
- **Fase 7:** P9, demo y hosting. Agregar JaCoCo para medir el 70 % de cobertura.

## Cambios al contrato

- Se agregó el código 405 a la tabla de respuestas. Ningún endpoint ni DTO cambió.
- `/auth/refresh` se documenta como paso siguiente a `/auth/cambiar-password`.
- `/actuator/env` y otras rutas no públicas responden 401 sin sesión, no 404.

## Entorno verificado (2026-11-06)

| Herramienta | Encontrado | Necesario |
|---|---|---|
| Java | 17 en el `PATH`; JDK 21 instalado | 21: ajustar `JAVA_HOME` |
| Docker | Docker Desktop 29.9.2 con WSL 3.0.3.0 | Correcto |
| Maven | 3.9.11 | Correcto |
| Node.js | 22.23.2 | Correcto |
| Angular CLI global | 21.2.4 | El proyecto usará 22.2.6 con su CLI local |
| PostgreSQL | Servicios 14 y 18 activos (uno ocupa el 5433) | No se usan; el proyecto va en Docker con la 16 |

## Próximos pasos

1. Aprobar la Fase 4.
2. Iniciar la Fase 5: sistema de diseño, layout, login, guards e interceptors.
