-- Registro de auditoría. El quién y el cuándo son created_by y created_at.
-- entidad_id y created_by aceptan NULL para eventos sin usuario identificado,
-- como un intento de login con un correo que no existe.

CREATE TABLE auditoria (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entidad        VARCHAR(50) NOT NULL,
    entidad_id     BIGINT,
    accion         VARCHAR(30) NOT NULL,
    valor_anterior JSONB,
    valor_nuevo    JSONB,
    ip             VARCHAR(45),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     BIGINT,
    updated_by     BIGINT,
    CONSTRAINT fk_auditoria_created_by FOREIGN KEY (created_by) REFERENCES usuarios (id),
    CONSTRAINT fk_auditoria_updated_by FOREIGN KEY (updated_by) REFERENCES usuarios (id)
);

CREATE INDEX ix_auditoria_entidad ON auditoria (entidad, entidad_id);
CREATE INDEX ix_auditoria_created_by ON auditoria (created_by);
CREATE INDEX ix_auditoria_created_at ON auditoria (created_at DESC);
CREATE INDEX ix_auditoria_accion ON auditoria (accion);

CREATE TRIGGER tg_auditoria_solo_insercion
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION f_impedir_modificacion();
