# AdminPro · Gestión administrativa empresarial

[![CI](https://github.com/dsebas28/adminpro/actions/workflows/ci.yml/badge.svg)](https://github.com/dsebas28/adminpro/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1-green)
![Angular 22](https://img.shields.io/badge/Angular-22-red)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue)
![Licencia MIT](https://img.shields.io/badge/licencia-MIT-lightgrey)

Aplicación web full stack para centralizar la administración de una empresa colombiana: empleados, documentos con su flujo de aprobación, adjuntos, indicadores y auditoría de cada cambio. Interfaz en español, zona horaria de Bogotá y datos de prueba realistas para Colombia.

![Dashboard de AdminPro](docs/screenshots/01-dashboard.png)

## Qué resuelve

En muchas empresas los documentos viven en correos y carpetas compartidas. Nadie sabe qué contrato está por vencer, quién aprobó qué ni en qué estado va un trámite. AdminPro lo reúne en un solo lugar:

- Cada documento recibe un número único, como `DOC-2026-000019`, sin duplicados ni huecos.
- Un flujo de aprobación define quién puede hacer cada paso, y quien registra un documento no puede aprobarlo.
- El dashboard avisa qué está vencido, qué vence pronto y qué documentos quedaron sin responsable activo.
- Cada cambio queda registrado: quién lo hizo, cuándo y qué valor tenía antes.

La identidad visual sale del mismo tema: el azul de tinta y el sello de radicado de una oficina colombiana.

## Funcionalidades

| Módulo | Qué incluye |
|---|---|
| **Autenticación** | JWT de 15 minutos, refresh token de 7 días con rotación y detección de robo, bloqueo tras 5 intentos, límite de intentos por IP y contraseñas temporales |
| **Roles y permisos** | 4 roles y 13 permisos granulares, validados en el backend con `@PreAuthorize` |
| **Empleados** | CRUD, búsqueda sin tildes, filtros, activar y desactivar, historial con valor anterior y nuevo |
| **Documentos** | Consecutivo automático por año, flujo de 5 estados, devolución con comentario, categorías, responsable y vencimiento |
| **Adjuntos** | PDF, DOCX, XLSX e imágenes hasta 10 MB, validados por su contenido real y no por la extensión |
| **Dashboard** | Indicadores, documentos por estado, próximos vencimientos, alertas y actividad, calculados en la base de datos |
| **Catálogos** | Departamentos, cargos y categorías de documento |
| **Auditoría** | Registro de solo inserción, protegido en la base de datos contra cambios y borrados |

## Capturas

### Inicio de sesión

El login muestra un sello de radicado con el consecutivo de los documentos; sus dígitos ruedan al cargar, como los de una numeradora. El tema claro u oscuro se elige con un botón y se recuerda.

| Tema claro | Tema oscuro |
|---|---|
| ![Inicio de sesión](docs/screenshots/02-login.png) | ![Inicio de sesión en tema oscuro](docs/screenshots/03-login-oscuro.png) |

### Dashboard

Los indicadores salen de endpoints de agregación, no de cálculos en el navegador. Las alertas indican su urgencia con texto además de color, para no depender solo de él. El gráfico usa barras en el orden del flujo, con el valor escrito en cada una, porque una dona dificultaría comparar los estados.

![Dashboard en tema oscuro](docs/screenshots/04-dashboard-oscuro.png)

### Empleados

Listado con paginación, orden y filtros en el servidor. La búsqueda ignora tildes y mayúsculas: "gomez" encuentra "Gómez".

| Listado | Búsqueda sin tildes |
|---|---|
| ![Listado de empleados](docs/screenshots/05-empleados.png) | ![Búsqueda de empleados](docs/screenshots/06-empleados-busqueda.png) |

Cada campo muestra su error debajo, y las mismas reglas se validan en el formulario, en el backend y en la base de datos. El detalle guarda el historial con el valor anterior y el nuevo.

| Validación por campo | Detalle e historial |
|---|---|
| ![Validación del formulario](docs/screenshots/07-empleado-validacion.png) | ![Detalle del empleado](docs/screenshots/08-empleado-detalle.png) |

### Documentos

El listado se puede filtrar por número, título, categoría, estado y fechas. Los documentos vencidos se marcan en la tabla.

| Listado | Búsqueda |
|---|---|
| ![Listado de documentos](docs/screenshots/09-documentos.png) | ![Búsqueda de documentos](docs/screenshots/10-documentos-busqueda.png) |

El número se asigna al guardar. En el detalle, los botones cambian según el estado del documento y el rol de quien lo mira; la línea de tiempo cuenta todo lo que le ha pasado, incluido el motivo de una devolución.

| Registrar | Detalle, adjuntos e historial |
|---|---|
| ![Registrar documento](docs/screenshots/11-documento-registrar.png) | ![Detalle del documento](docs/screenshots/12-documento-detalle.png) |

Devolver un documento exige escribir el motivo, y el comentario queda en el historial.

![Devolver un documento](docs/screenshots/13-documento-devolver.png)

### Catálogos

Departamentos, cargos y categorías comparten una sola pantalla. Un catálogo en uso no se puede desactivar.

| Catálogos | Formulario en modal |
|---|---|
| ![Catálogos](docs/screenshots/14-catalogos.png) | ![Agregar catálogo](docs/screenshots/15-catalogo-modal.png) |

### Usuarios y auditoría

El Administrador crea usuarios con una contraseña temporal, que deben cambiar antes de usar el sistema. Siempre debe quedar al menos un Administrador activo.

| Usuarios | Crear usuario |
|---|---|
| ![Usuarios](docs/screenshots/16-usuarios.png) | ![Crear usuario](docs/screenshots/17-usuario-crear.png) |

La auditoría responde quién, qué, cuándo y qué cambió. Contraseñas y tokens nunca llegan a este registro.

| Registro de auditoría | Detalle del cambio |
|---|---|
| ![Auditoría](docs/screenshots/18-auditoria.png) | ![Detalle del cambio](docs/screenshots/19-auditoria-detalle.png) |

### Sistema de diseño

Las pantallas se construyen con un catálogo propio de componentes: botones, campos, tabla, paginador, modal, avisos, estados, estado vacío y esqueletos de carga. La aplicación incluye una página que los muestra.

![Sistema de diseño](docs/screenshots/20-sistema-diseno.png)

| Confirmación antes de desactivar | Menú lateral colapsado |
|---|---|
| ![Confirmación](docs/screenshots/21-confirmacion.png) | ![Menú colapsado](docs/screenshots/22-menu-colapsado.png) |

### Permisos

El menú y los botones cambian según el rol, y entrar por la URL a una pantalla sin permiso muestra este aviso. Eso solo mejora la experiencia: la seguridad real está en el backend, que responde 403.

![Acceso denegado](docs/screenshots/23-acceso-denegado.png)

### En el celular

La interfaz se adapta a móvil: el menú se abre como panel lateral y las tablas se desplazan dentro de su propio contenedor.

| Dashboard | Menú | Documentos |
|---|---|---|
| ![Dashboard en móvil](docs/screenshots/24-movil-dashboard.png) | ![Menú en móvil](docs/screenshots/25-movil-menu.png) | ![Documentos en móvil](docs/screenshots/26-movil-documentos.png) |

### Documentación de la API

La API se documenta sola con OpenAPI, en Swagger UI.

![Swagger](docs/screenshots/27-swagger.png)

## Arquitectura

Es un monolito modular: un solo desplegable con módulos de negocio separados. Para una empresa y un equipo pequeño, los microservicios solo agregarían red y consistencia distribuida sin ningún beneficio.

```mermaid
flowchart LR
    U["Navegador<br/>Angular"]
    subgraph DC["Docker Compose"]
        N["frontend<br/>Nginx"]
        subgraph API["backend: Spring Boot"]
            SEC["Seguridad<br/>JWT y permisos"]
            MOD["Módulos de negocio"]
            ST["StorageService"]
            SEC --> MOD --> ST
        end
        DB[("PostgreSQL 16")]
        V[("Volumen de adjuntos")]
    end
    U -->|"mismo origen"| N
    N -->|"/api/v1"| SEC
    MOD --> DB
    ST --> V
```

- **Nginx** sirve Angular y hace de proxy hacia la API. El navegador solo ve un dominio, lo que permite la cookie `SameSite=Strict`.
- **El access token vive en memoria** y el refresh token en una cookie `HttpOnly`: un ataque XSS no puede leerlos para usarlos después.
- **`StorageService` es una interfaz**, así que pasar de un volumen Docker a S3 solo requiere otra implementación.

## Modelo de datos

14 tablas normalizadas, con migraciones Flyway. El diagrama completo y la descripción de cada tabla están en el [documento de arquitectura](docs/fase-1-analisis-y-arquitectura.md#4-modelo-de-datos-del-mvp).

```mermaid
erDiagram
    roles ||--o{ usuarios : asigna
    roles ||--o{ rol_permisos : tiene
    permisos ||--o{ rol_permisos : "incluido en"
    usuarios ||--o{ refresh_tokens : abre
    empleados |o--o| usuarios : "accede como"
    departamentos ||--o{ empleados : agrupa
    cargos ||--o{ empleados : clasifica
    categorias_documento ||--o{ documentos : clasifica
    empleados ||--o{ documentos : "responsable de"
    documentos ||--o{ documento_adjuntos : contiene
    documentos ||--o{ documento_historial : registra
    usuarios ||--o{ auditoria : genera
```

### Flujo de un documento

```mermaid
stateDiagram-v2
    direction LR
    [*] --> Registrado: crear
    Registrado --> Pendiente: enviar
    Pendiente --> EnRevision: iniciar revisión
    EnRevision --> Aprobado: aprobar
    EnRevision --> Pendiente: devolver con comentario
    Aprobado --> Finalizado: finalizar
    Finalizado --> [*]
```

Para enviar un documento hace falta al menos un adjunto. Desde En revisión, sus datos y adjuntos quedan congelados.

## Roles y permisos

L es leer, E es crear o editar y D es desactivar.

| Módulo | Administrador | Supervisor | Auxiliar | Consulta |
|---|---|---|---|---|
| Dashboard | L | L | L | L |
| Empleados | L E D | L E | L E | L |
| Catálogos | L E D | L | L | L |
| Documentos | L E D y aprobar | L E y aprobar | L E | L |
| Usuarios y roles | L E D | No | No | No |
| Auditoría | L | L | No | No |

## Decisiones técnicas destacadas

La lista completa, con su justificación, está en [DECISIONS.md](DECISIONS.md). Estas son las más interesantes:

- **Consecutivo sin huecos ante concurrencia.** Una secuencia nativa de PostgreSQL deja huecos al revertir una transacción y reiniciarla cada año requiere DDL. Se usa una tabla con `INSERT ... ON CONFLICT DO UPDATE ... RETURNING`: el bloqueo de la fila serializa la operación hasta el commit. Se probó con peticiones simultáneas reales y miles de transacciones con reversiones.
- **403 antes de leer el cuerpo.** Spring valida `@Valid` antes que `@PreAuthorize`, así que un rol sin permiso recibía los errores de validación en lugar de un 403. Un interceptor evalúa la misma anotación antes de leer la petición.
- **Refresh tokens con detección de robo.** Se guardan como hash y rotan en cada uso. Reutilizar uno viejo revoca toda la sesión, con una gracia de 10 segundos para dos pestañas que renuevan a la vez.
- **Fallos de login en transacción propia.** Si el conteo de intentos ocurriera en la misma transacción del error 401, se revertiría y el bloqueo nunca se activaría.
- **Archivos validados por contenido.** La extensión la controla quien sube el archivo. Tika revisa los primeros bytes y, como DOCX y XLSX son archivos ZIP, se inspecciona su estructura interna.
- **Auditoría protegida en la base de datos.** Un trigger rechaza `UPDATE` y `DELETE`: un registro que la aplicación pueda alterar no prueba nada.

## Tecnologías

| Capa | Tecnologías |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA, MapStruct, Bean Validation, Apache Tika, Bucket4j |
| Frontend | Angular 22 (signals, componentes standalone), TypeScript, Tailwind CSS 4, Chart.js |
| Base de datos | PostgreSQL 16, Flyway |
| Pruebas | JUnit 5, Mockito, Testcontainers, JaCoCo, Vitest |
| Infraestructura | Docker, Docker Compose, Nginx, GitHub Actions |

## Pruebas

| Tipo | Cantidad | Qué cubre |
|---|---|---|
| Backend unitarias | 43 | Servicios, flujo de estados, manejo de errores |
| Backend de integración | 387 | PostgreSQL real con Testcontainers; 292 casos son la matriz de permisos: cada endpoint contra cada rol |
| Frontend | 61 | Servicios, guards, interceptors y validación de formularios |

La cobertura de los servicios va de 86 % a 100 %, y la compilación falla si alguno baja del 70 %. Además, todo el sistema se probó de punta a punta en un navegador real.

```bash
cd backend && mvn clean verify                # requiere Docker y JDK 21
cd frontend && npx ng test --watch=false
```

## Puesta en marcha

Requisitos: [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
git clone https://github.com/dsebas28/adminpro.git
cd adminpro
cp .env.example .env        # en PowerShell: Copy-Item .env.example .env
docker compose up --build -d --wait
```

Antes de usarlo fuera de tu equipo, cambia `POSTGRES_PASSWORD` y `JWT_SECRET` en `.env`.

| Servicio | Dirección |
|---|---|
| Aplicación | http://localhost:4200 |
| Swagger | http://localhost:8080/swagger-ui/index.html |

### Usuarios de prueba

Son cuentas ficticias de los perfiles `dev` y `demo`. En el perfil `prod` no existen.

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@adminpro.test` | `AdminPro.Demo2026` |
| Supervisor | `supervisor@adminpro.test` | `AdminPro.Demo2026` |
| Auxiliar | `auxiliar@adminpro.test` | `AdminPro.Demo2026` |
| Consulta | `consulta@adminpro.test` | `AdminPro.Demo2026` |

Para ver el flujo completo: registra un documento como Auxiliar, adjunta un PDF y envíalo; luego entra como Supervisor y apruébalo.

## Estructura

```text
adminpro/
├── backend/                  Spring Boot
│   └── src/main/
│       ├── java/com/adminpro/
│       │   ├── auth/         Login, tokens y contraseñas
│       │   ├── usuarios/     Usuarios, roles y permisos
│       │   ├── empleados/    Empleados, departamentos y cargos
│       │   ├── documentos/   Documentos, flujo, consecutivo y adjuntos
│       │   ├── dashboard/    Indicadores
│       │   ├── audit/        Auditoría
│       │   ├── security/     JWT, filtros y límite de intentos
│       │   ├── storage/      Almacenamiento y validación de archivos
│       │   └── common/       Paginación, catálogos y utilidades
│       └── resources/db/     Migraciones Flyway y datos de prueba
├── frontend/                 Angular
│   └── src/app/
│       ├── core/             Sesión, guards, interceptors y servicios
│       ├── shared/           Sistema de diseño
│       ├── layouts/          Estructura de las pantallas
│       └── features/         Un módulo por pantalla, con carga diferida
├── docs/                     Arquitectura, contrato de API y capturas
├── .github/workflows/        Integración continua
├── docker-compose.yml
└── render.yaml               Despliegue del demo
```

## Documentación

- [Arquitectura y contrato de API](docs/fase-1-analisis-y-arquitectura.md)
- [Decisiones técnicas](DECISIONS.md)
- [Estado del proyecto](PROGRESS.md)

## Próximos pasos

Las siguientes iteraciones agregan solicitudes administrativas, proveedores con validación del NIT, inventario, informes en Excel, CSV y PDF, y tareas automáticas.

## Licencia

[MIT](LICENSE). Todos los datos de prueba son ficticios: cédulas, teléfonos, personas y empresas son inventados.
