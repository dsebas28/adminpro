-- Extensiones y funciones compartidas por el resto de las migraciones.
-- pg_trgm y unaccent son extensiones "trusted": las puede crear el dueño de la
-- base de datos sin ser superusuario, lo que también funciona en servicios gestionados.

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- unaccent() es STABLE, y PostgreSQL exige funciones IMMUTABLE en los índices.
-- Fijar el diccionario de forma explícita hace seguro declararla IMMUTABLE.
CREATE FUNCTION f_normalizar(texto TEXT)
    RETURNS TEXT
    LANGUAGE sql
    IMMUTABLE
    PARALLEL SAFE
    STRICT
AS $$
    SELECT lower(public.unaccent('public.unaccent'::regdictionary, texto));
$$;

COMMENT ON FUNCTION f_normalizar(TEXT) IS
    'Minúsculas y sin tildes: buscar "gomez" encuentra "Gómez".';

-- Protege las tablas de solo inserción (auditoría e historial).
CREATE FUNCTION f_impedir_modificacion()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'La tabla % es de solo inserción', TG_TABLE_NAME
        USING ERRCODE = 'restrict_violation';
END;
$$;
