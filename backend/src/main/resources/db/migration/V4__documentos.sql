-- Gestión documental: categorías, documentos, adjuntos, historial y consecutivos.

CREATE TABLE categorias_documento (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    CONSTRAINT fk_categorias_documento_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_categorias_documento_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id)
);

CREATE UNIQUE INDEX uq_categorias_documento_nombre ON categorias_documento (lower(nombre));
CREATE INDEX ix_categorias_documento_activo ON categorias_documento (activo);

CREATE TABLE documentos (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consecutivo       VARCHAR(15)  NOT NULL,
    titulo            VARCHAR(200) NOT NULL,
    descripcion       TEXT,
    categoria_id      BIGINT       NOT NULL,
    responsable_id    BIGINT       NOT NULL,
    estado            VARCHAR(20)  NOT NULL DEFAULT 'REGISTRADO',
    fecha_documento   DATE         NOT NULL,
    fecha_vencimiento DATE,
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        BIGINT,
    updated_by        BIGINT,
    CONSTRAINT uq_documentos_consecutivo UNIQUE (consecutivo),
    CONSTRAINT fk_documentos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias_documento (id),
    CONSTRAINT fk_documentos_responsable FOREIGN KEY (responsable_id) REFERENCES empleados (id),
    CONSTRAINT fk_documentos_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_documentos_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id),
    CONSTRAINT ck_documentos_consecutivo CHECK (consecutivo ~ '^DOC-[0-9]{4}-[0-9]{6}$'),
    CONSTRAINT ck_documentos_estado CHECK (
        estado IN ('REGISTRADO', 'PENDIENTE', 'EN_REVISION', 'APROBADO', 'FINALIZADO')
    ),
    CONSTRAINT ck_documentos_descripcion CHECK (char_length(descripcion) <= 2000),
    CONSTRAINT ck_documentos_fechas CHECK (fecha_vencimiento >= fecha_documento)
);

CREATE INDEX ix_documentos_estado ON documentos (estado);
CREATE INDEX ix_documentos_categoria ON documentos (categoria_id);
CREATE INDEX ix_documentos_responsable ON documentos (responsable_id);
CREATE INDEX ix_documentos_fecha_documento ON documentos (fecha_documento);
CREATE INDEX ix_documentos_fecha_vencimiento ON documentos (fecha_vencimiento);
CREATE INDEX ix_documentos_activo ON documentos (activo);

-- Igual que en empleados: la consulta debe repetir esta expresión.
CREATE INDEX ix_documentos_busqueda ON documentos
    USING gin (f_normalizar(consecutivo || ' ' || titulo) gin_trgm_ops);

CREATE TABLE documento_adjuntos (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    documento_id    BIGINT       NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    storage_key     UUID         NOT NULL,
    tipo_mime       VARCHAR(100) NOT NULL,
    tamano_bytes    BIGINT       NOT NULL,
    sha256          CHAR(64)     NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT uq_documento_adjuntos_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_documento_adjuntos_documento FOREIGN KEY (documento_id) REFERENCES documentos (id),
    CONSTRAINT fk_documento_adjuntos_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_documento_adjuntos_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id),
    CONSTRAINT ck_documento_adjuntos_tamano CHECK (tamano_bytes BETWEEN 1 AND 10485760),
    CONSTRAINT ck_documento_adjuntos_sha256 CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_documento_adjuntos_tipo CHECK (
        tipo_mime IN (
            'application/pdf',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'image/png',
            'image/jpeg',
            'image/webp'
        )
    )
);

CREATE INDEX ix_documento_adjuntos_documento ON documento_adjuntos (documento_id);

CREATE TABLE documento_historial (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    documento_id    BIGINT      NOT NULL,
    accion          VARCHAR(30) NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20),
    comentario      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    CONSTRAINT fk_documento_historial_documento FOREIGN KEY (documento_id) REFERENCES documentos (id),
    CONSTRAINT fk_documento_historial_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_documento_historial_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id),
    CONSTRAINT ck_documento_historial_accion CHECK (
        accion IN (
            'CREADO', 'EDITADO', 'CAMBIO_ESTADO', 'ADJUNTO_AGREGADO',
            'ADJUNTO_ELIMINADO', 'DESACTIVADO', 'REACTIVADO'
        )
    ),
    CONSTRAINT ck_documento_historial_estado_anterior CHECK (
        estado_anterior IN ('REGISTRADO', 'PENDIENTE', 'EN_REVISION', 'APROBADO', 'FINALIZADO')
    ),
    CONSTRAINT ck_documento_historial_estado_nuevo CHECK (
        estado_nuevo IN ('REGISTRADO', 'PENDIENTE', 'EN_REVISION', 'APROBADO', 'FINALIZADO')
    ),
    CONSTRAINT ck_documento_historial_cambio CHECK (
        accion <> 'CAMBIO_ESTADO' OR (estado_anterior IS NOT NULL AND estado_nuevo IS NOT NULL)
    ),
    CONSTRAINT ck_documento_historial_devolucion CHECK (
        NOT (estado_anterior = 'EN_REVISION' AND estado_nuevo = 'PENDIENTE')
        OR COALESCE(length(btrim(comentario)), 0) > 0 -- Sin COALESCE, NULL pasaría el CHECK.
    ),
    CONSTRAINT ck_documento_historial_comentario CHECK (char_length(comentario) <= 1000)
);

CREATE INDEX ix_documento_historial_documento ON documento_historial (documento_id, created_at);

CREATE TRIGGER tg_documento_historial_solo_insercion
    BEFORE UPDATE OR DELETE ON documento_historial
    FOR EACH ROW EXECUTE FUNCTION f_impedir_modificacion();

-- Una fila por tipo y año. Ver D-12 en DECISIONS.md. El backend obtiene el número con:
--   INSERT INTO secuencias_consecutivo (tipo, anio, ultimo_valor) VALUES (?, ?, 1)
--   ON CONFLICT (tipo, anio) DO UPDATE
--     SET ultimo_valor = secuencias_consecutivo.ultimo_valor + 1, updated_at = now()
--   RETURNING ultimo_valor;
CREATE TABLE secuencias_consecutivo (
    tipo         VARCHAR(20) NOT NULL,
    anio         SMALLINT    NOT NULL,
    ultimo_valor BIGINT      NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   BIGINT,
    updated_by   BIGINT,
    CONSTRAINT pk_secuencias_consecutivo PRIMARY KEY (tipo, anio),
    CONSTRAINT fk_secuencias_consecutivo_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_secuencias_consecutivo_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id),
    CONSTRAINT ck_secuencias_consecutivo_anio CHECK (anio BETWEEN 2000 AND 9999),
    CONSTRAINT ck_secuencias_consecutivo_valor CHECK (ultimo_valor BETWEEN 1 AND 999999)
);
