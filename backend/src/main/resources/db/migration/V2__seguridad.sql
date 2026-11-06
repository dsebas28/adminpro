-- Usuarios, roles, permisos y refresh tokens.
-- Las columnas created_by y updated_by de todas las tablas apuntan a usuarios, y
-- usuarios apunta a roles. Para romper ese ciclo, las claves foráneas de roles,
-- permisos y rol_permisos se agregan al final, cuando usuarios ya existe.

CREATE TABLE roles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo      VARCHAR(30)  NOT NULL,
    nombre      VARCHAR(60)  NOT NULL,
    descripcion VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    CONSTRAINT uq_roles_codigo UNIQUE (codigo)
);

CREATE TABLE permisos (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo      VARCHAR(60)  NOT NULL,
    modulo      VARCHAR(30)  NOT NULL,
    descripcion VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    CONSTRAINT uq_permisos_codigo UNIQUE (codigo)
);

CREATE TABLE rol_permisos (
    rol_id     BIGINT      NOT NULL,
    permiso_id BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT,
    updated_by BIGINT,
    CONSTRAINT pk_rol_permisos PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permisos_rol FOREIGN KEY (rol_id) REFERENCES roles (id),
    CONSTRAINT fk_rol_permisos_permiso FOREIGN KEY (permiso_id) REFERENCES permisos (id)
);

CREATE INDEX ix_rol_permisos_permiso ON rol_permisos (permiso_id);

CREATE TABLE usuarios (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    correo                VARCHAR(150) NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    nombre_visible        VARCHAR(120) NOT NULL,
    rol_id                BIGINT       NOT NULL,
    empleado_id           BIGINT, -- La clave foránea se agrega en V3, junto con empleados.
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,
    debe_cambiar_password BOOLEAN      NOT NULL DEFAULT TRUE,
    intentos_fallidos     SMALLINT     NOT NULL DEFAULT 0,
    bloqueado_hasta       TIMESTAMPTZ,
    ultimo_acceso_at      TIMESTAMPTZ,
    version               BIGINT       NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by            BIGINT,
    updated_by            BIGINT,
    CONSTRAINT uq_usuarios_empleado UNIQUE (empleado_id),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles (id),
    CONSTRAINT fk_usuarios_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_usuarios_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id),
    CONSTRAINT ck_usuarios_correo CHECK (correo ~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$'),
    CONSTRAINT ck_usuarios_intentos CHECK (intentos_fallidos >= 0)
);

CREATE UNIQUE INDEX uq_usuarios_correo ON usuarios (lower(correo));
CREATE INDEX ix_usuarios_rol ON usuarios (rol_id);
CREATE INDEX ix_usuarios_activo ON usuarios (activo);

CREATE TABLE refresh_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    token_hash  CHAR(64)    NOT NULL,
    familia_id  UUID        NOT NULL,
    expira_at   TIMESTAMPTZ NOT NULL,
    revocado_at TIMESTAMPTZ,
    ip          VARCHAR(45),
    user_agent  VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_refresh_tokens_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_refresh_tokens_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id),
    CONSTRAINT ck_refresh_tokens_hash CHECK (token_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_refresh_tokens_usuario ON refresh_tokens (usuario_id);
CREATE INDEX ix_refresh_tokens_familia ON refresh_tokens (familia_id);
CREATE INDEX ix_refresh_tokens_expira ON refresh_tokens (expira_at);

-- Claves foráneas de auditoría pendientes, ahora que usuarios existe.
ALTER TABLE roles
    ADD CONSTRAINT fk_roles_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    ADD CONSTRAINT fk_roles_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id);

ALTER TABLE permisos
    ADD CONSTRAINT fk_permisos_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    ADD CONSTRAINT fk_permisos_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id);

ALTER TABLE rol_permisos
    ADD CONSTRAINT fk_rol_permisos_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    ADD CONSTRAINT fk_rol_permisos_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id);
