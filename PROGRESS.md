# AdminPro — Progreso

**Fase actual:** 7. CI y documentación, en curso: falta publicar el repositorio y desplegar el demo.
**Última actualización:** 2026-11-06

## Estado por fase

| Fase | Estado |
|---|---|
| 1. Análisis y arquitectura | Aprobada (P3 sigue abierta) |
| 2. Base de datos | Aprobada y validada en la imagen `postgres:16` |
| 3. Backend base | Aprobada y validada con Docker |
| 4. Seguridad | Aprobada |
| 5. Frontend base | Aprobada |
| 6. Módulos del MVP | Aprobada |
| 7. CI y documentación | README, CI y modo demo listos. Faltan el primer pipeline y el demo en vivo |
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

**Frontend** (`frontend/`, Angular 22, Tailwind 4, Vitest)

| Elemento | Ubicación |
|---|---|
| Tokens de diseño, tema claro y oscuro | `src/styles.css` |
| Sesión, guards, interceptors, tema, avisos | `src/app/core/` |
| Botón, campo, tabla, paginador, modal, confirmación, avisos, estado, vacío, esqueleto | `src/app/shared/` |
| Sidebar colapsable, encabezado, breadcrumbs, sello del login | `src/app/layouts/` |
| Login, cambio de contraseña, catálogo de componentes | `src/app/features/` |
| Imagen Docker y proxy Nginx | `Dockerfile`, `nginx.conf` |

**Módulos del MVP**

| Módulo | Backend | Frontend |
|---|---|---|
| Catálogos | `common/catalogo/`, `empleados/`, `documentos/` | `features/catalogos/` |
| Empleados | `empleados/` | `features/empleados/` |
| Documentos, consecutivo y flujo | `documentos/` | `features/documentos/` |
| Adjuntos | `storage/`, `documentos/service/AdjuntoService` | Detalle del documento |
| Dashboard | `dashboard/` | `features/dashboard/` |
| Usuarios y auditoría | Fase 4 | `features/usuarios/`, `features/auditoria/` |

**CI y documentación**

| Elemento | Ubicación |
|---|---|
| README con 27 capturas | `README.md`, `docs/screenshots/` |
| Integración continua | `.github/workflows/ci.yml` |
| Despliegue del demo | `render.yaml` |
| Modo demo | `security/ModoDemo`, `demo/DemoReset` |
| Licencia MIT | `LICENSE` |

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

**Fase 7.** Ejecutada por Claude:

- `mvn clean verify`: 430 pruebas (43 unitarias y 387 de integración).
- Modo demo con límite de 512 MB: las 4 cuentas responden 409 al modificarlas, el reset programado devuelve los datos iniciales y el backend usó unos 385 MB.

**Sin probar todavía:** el pipeline en GitHub Actions y el despliegue en Render. Solo se validó la sintaxis de `ci.yml` y `render.yaml`. En particular, falta confirmar que Render reenvía `/api` y conserva las cookies.

**Fase 6.** Ejecutada por Claude con Docker y Microsoft Edge real:

- `mvn clean verify`: **427 pruebas** (40 unitarias y 387 de integración), con 292 casos en la matriz de permisos.
- Cobertura de servicios: entre 86 % y 100 %. JaCoCo rompe la compilación por debajo de 70 %.
- `ng test`: 61 pruebas.
- 36 comprobaciones de punta a punta: CRUD de empleados y catálogos, flujo de documentos entre dos usuarios, adjuntos, renovación del token, permisos por rol y móvil. Las 39 de la Fase 5 siguen pasando.
- Consecutivo con 24 peticiones simultáneas: sin duplicados ni huecos.
- Subida real por Nginx: 10 MB exactos entran; 10,6 MB y 12 MB responden 413 en Problem Details.

**Fase 5.** Ejecutada por Claude con `docker compose` y Microsoft Edge real:

- `ng test`: 55 pruebas de servicios, guards, interceptors y formularios.
- 39 comprobaciones en navegador: login contra el backend, tema claro y oscuro, sesión al recargar, logout, menú por rol, acceso denegado, contraseña temporal, diálogos, sidebar, móvil sin scroll horizontal y CSP.
- Contraste WCAG: 40 pares de colores, mínimo 5,42:1.
- `X-Forwarded-For` falso a través de Nginx: la petición 11 recibe 429, aunque cada una invente una IP distinta.

Los scripts de navegador viven en un directorio temporal y no están en el repositorio.

**Fase 4.** `mvn clean verify` con PostgreSQL 16 real, ejecutado por Claude: **132 pruebas** (29 unitarias y 103 de integración).

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
- **Fase 7:** versionar pruebas de navegador en el repositorio, si se quieren en CI.
- **Maven:** usar `mvn clean verify`. Sin `clean`, una compilación incremental dejó clases corruptas una vez.
- **Iteración 4:** limpiar periódicamente los refresh tokens vencidos.
- **Fase 7:** P9, demo y hosting.
- **Selects de empleados:** cargan hasta 100 activos, el máximo de la API. Con más empleados harían falta búsqueda dentro del campo.

## Cambios al contrato

- Se agregó el código 405 a la tabla de respuestas. Ningún endpoint ni DTO cambió.
- `/auth/refresh` se documenta como paso siguiente a `/auth/cambiar-password`.
- `/actuator/env` y otras rutas no públicas responden 401 sin sesión, no 404.
- `EmpleadoResponse` incluye `version`, y `departamento` y `cargo` como referencias en lugar de sus ids.
- `ActividadResponse` incluye `entidadId`.
- 403 con código `SEPARACION_FUNCIONES` cuando quien creó un documento intenta aprobarlo.
- Pantalla nueva `/catalogos`, sin cambios en la API.

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

1. Crear el repositorio `adminpro` en GitHub y subir el código.
2. Confirmar que el pipeline pasa.
3. Crear cuentas en Neon y Render, y publicar el demo.
4. Agregar el enlace del demo al README.
