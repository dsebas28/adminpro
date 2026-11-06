# AdminPro — Progreso

**Fase actual:** 3. Backend base, validada con Docker y pendiente de tu aprobación.
**Última actualización:** 2026-11-06

## Estado por fase

| Fase | Estado |
|---|---|
| 1. Análisis y arquitectura | Aprobada (P3 sigue abierta) |
| 2. Base de datos | Aprobada y validada en la imagen `postgres:16` |
| 3. Backend base | Criterio cumplido, pendiente de aprobación |
| 4. Seguridad | No iniciada |
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

**Validación con Docker** (2026-11-06, Docker 29.9.2, Compose 5.6.1, PostgreSQL 16.15 en Debian, `en_US.utf8`):

| Prueba | Resultado |
|---|---|
| `docker compose up --build` | `db` y `backend` en estado `healthy` |
| Migraciones en el contenedor | 7 aplicadas, 26 empleados |
| `/actuator/health` | `UP` |
| Swagger UI y `/v3/api-docs` | 200 |
| Usuario del contenedor backend | `adminpro` (uid 10001), sin privilegios |
| `mvn verify` | `BUILD SUCCESS`: 9 pruebas unitarias y 5 de integración con Testcontainers |

Se corrigió un choque de puertos: tu PostgreSQL local ya usa el 5433, así que AdminPro expone la base de datos en el **55432**.

## Pendientes conocidos

- **P3, sin resolver:** no se ha tocado el `.git`. Contiene Football AI Predictor y puede ser su única copia. No hacer commits hasta decidir.
- **Java:** el `PATH` apunta a Java 17; hay que apuntar `JAVA_HOME` a un JDK 21 instalado.
- **Fase 4:** responder 403 ante `AccessDeniedException` (ver D-24) y reemplazar el bean `auditorActual`.
- **Fase 6:** los adjuntos de prueba son solo metadatos. El backend debe crear archivos de ejemplo al arrancar en `dev` y `demo`, o su descarga dará 404.
- **Fase 6:** las consultas de búsqueda deben repetir la expresión exacta de los índices (ver D-21).
- **Fase 6:** la regla "fecha de ingreso no futura" se valida solo en el backend.
- **Fase 7:** P9, demo y hosting. Agregar JaCoCo para medir el 70 % de cobertura.

## Cambios al contrato

- Se agregó el código 405 a la tabla de respuestas. Ningún endpoint ni DTO cambió.

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

1. Aprobar la Fase 3.
2. Resolver P3.
3. Iniciar la Fase 4: login, refresh token, roles, permisos y auditoría.
