-- Departamentos, cargos y empleados.
-- La regla "fecha_ingreso no futura" se valida en el backend: un CHECK con
-- CURRENT_DATE no es determinista y podría fallar al restaurar un respaldo.

CREATE TABLE departamentos (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    CONSTRAINT fk_departamentos_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_departamentos_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id)
);

CREATE UNIQUE INDEX uq_departamentos_nombre ON departamentos (lower(nombre));
CREATE INDEX ix_departamentos_activo ON departamentos (activo);

CREATE TABLE cargos (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    CONSTRAINT fk_cargos_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_cargos_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id)
);

CREATE UNIQUE INDEX uq_cargos_nombre ON cargos (lower(nombre));
CREATE INDEX ix_cargos_activo ON cargos (activo);

CREATE TABLE empleados (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cedula          VARCHAR(10)  NOT NULL,
    nombres         VARCHAR(100) NOT NULL,
    apellidos       VARCHAR(100) NOT NULL,
    correo          VARCHAR(150) NOT NULL,
    telefono        VARCHAR(10),
    fecha_ingreso   DATE         NOT NULL,
    departamento_id BIGINT       NOT NULL,
    cargo_id        BIGINT       NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_empleados_cedula UNIQUE (cedula),
    CONSTRAINT fk_empleados_departamento FOREIGN KEY (departamento_id) REFERENCES departamentos (id),
    CONSTRAINT fk_empleados_cargo FOREIGN KEY (cargo_id) REFERENCES cargos (id),
    CONSTRAINT fk_empleados_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_empleados_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id),
    CONSTRAINT ck_empleados_cedula CHECK (cedula ~ '^[0-9]{6,10}$'),
    CONSTRAINT ck_empleados_telefono CHECK (telefono ~ '^[0-9]{10}$'),
    CONSTRAINT ck_empleados_correo CHECK (correo ~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$')
);

CREATE UNIQUE INDEX uq_empleados_correo ON empleados (lower(correo));
CREATE INDEX ix_empleados_departamento ON empleados (departamento_id);
CREATE INDEX ix_empleados_cargo ON empleados (cargo_id);
CREATE INDEX ix_empleados_activo ON empleados (activo);

-- Un solo índice sirve al filtro q (nombre, cédula y correo). La consulta del
-- repositorio debe usar exactamente esta misma expresión para aprovecharlo.
CREATE INDEX ix_empleados_busqueda ON empleados
    USING gin (f_normalizar(nombres || ' ' || apellidos || ' ' || cedula || ' ' || correo) gin_trgm_ops);

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_empleado FOREIGN KEY (empleado_id) REFERENCES empleados (id);
