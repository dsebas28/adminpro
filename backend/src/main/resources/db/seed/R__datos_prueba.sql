-- Datos ficticios para los perfiles dev y demo. Esta carpeta no se incluye en prod.
--
-- Es una migración repetible: Flyway la ejecuta después de las versionadas y otra
-- vez si su contenido cambia, así que cada INSERT debe poder repetirse sin duplicar.
--
-- Usuarios de prueba (contraseña de demostración, sin valor fuera de este entorno):
--   admin@adminpro.test, supervisor@adminpro.test,
--   auxiliar@adminpro.test, consulta@adminpro.test
--   Contraseña: AdminPro.Demo2026
--
-- Cédulas, teléfonos y personas son inventados. El dominio .test está reservado
-- (RFC 2606), por lo que ningún correo puede llegar a una persona real.

-- Catálogos -----------------------------------------------------------------

INSERT INTO departamentos (nombre, descripcion) VALUES
    ('Gerencia',             'Dirección general de la empresa'),
    ('Talento Humano',       'Selección, contratación y bienestar'),
    ('Contabilidad',         'Registros contables e impuestos'),
    ('Tesorería',            'Pagos, recaudos y flujo de caja'),
    ('Compras',              'Negociación y relación con proveedores'),
    ('Comercial',            'Ventas y atención a clientes'),
    ('Tecnología',           'Sistemas de información y soporte'),
    ('Jurídica',             'Contratos y cumplimiento normativo'),
    ('Archivo Histórico',    'Área fusionada con Gerencia')
ON CONFLICT DO NOTHING;

UPDATE departamentos SET activo = FALSE WHERE nombre = 'Archivo Histórico' AND activo;

INSERT INTO cargos (nombre, descripcion) VALUES
    ('Gerente General',        'Máxima autoridad administrativa'),
    ('Director de Área',       'Lidera un departamento'),
    ('Coordinador',            'Coordina equipos y procesos'),
    ('Analista Senior',        'Analista con experiencia avanzada'),
    ('Analista',               'Ejecuta y analiza procesos del área'),
    ('Contador',               'Responsable de los estados financieros'),
    ('Abogado',                'Asesoría jurídica'),
    ('Asistente Administrativo', 'Apoyo administrativo general'),
    ('Auxiliar Administrativo',  'Apoyo operativo y de archivo'),
    ('Aprendiz SENA',          'Etapa productiva de contrato de aprendizaje')
ON CONFLICT DO NOTHING;

INSERT INTO categorias_documento (nombre, descripcion) VALUES
    ('Contratos',             'Contratos con empleados, clientes y proveedores'),
    ('Pólizas y seguros',     'Pólizas de cumplimiento, responsabilidad y ARL'),
    ('Facturación',           'Facturas y soportes contables'),
    ('Tributario',            'Declaraciones, RUT y certificados de la DIAN'),
    ('Cámara de Comercio',    'Certificados de existencia y representación legal'),
    ('Talento Humano',        'Hojas de vida, afiliaciones y seguridad social'),
    ('Actas',                 'Actas de reunión y de comité'),
    ('Políticas internas',    'Políticas, manuales y reglamentos')
ON CONFLICT DO NOTHING;

-- Empleados -----------------------------------------------------------------

INSERT INTO empleados (cedula, nombres, apellidos, correo, telefono, fecha_ingreso, departamento_id, cargo_id, activo)
SELECT e.cedula, e.nombres, e.apellidos, e.correo, e.telefono, e.fecha_ingreso::DATE, d.id, c.id, e.activo
FROM (VALUES
    ('1000200301', 'Andrea Carolina',  'Gómez Restrepo',    'andrea.gomez@adminpro.test',      '3005550101', '2019-02-04', 'Gerencia',          'Gerente General',          TRUE),
    ('1000200302', 'Julián David',     'Ramírez Ospina',    'julian.ramirez@adminpro.test',    '3005550102', '2020-06-16', 'Talento Humano',    'Director de Área',         TRUE),
    ('1000200303', 'Laura Marcela',    'Peña Cárdenas',     'laura.pena@adminpro.test',        '3005550103', '2022-03-01', 'Talento Humano',    'Auxiliar Administrativo',  TRUE),
    ('1000200304', 'Sebastián',        'Muñoz Londoño',     'sebastian.munoz@adminpro.test',   '3005550104', '2021-09-13', 'Contabilidad',      'Analista',                 TRUE),
    ('1000200305', 'Paola Andrea',     'Rodríguez Vélez',   'paola.rodriguez@adminpro.test',   '3005550105', '2018-11-05', 'Contabilidad',      'Contador',                 TRUE),
    ('1000200306', 'Carlos Eduardo',   'Martínez Herrera',  'carlos.martinez@adminpro.test',   '3005550106', '2017-08-22', 'Contabilidad',      'Director de Área',         TRUE),
    ('1000200307', 'Natalia',          'Castaño Giraldo',   'natalia.castano@adminpro.test',   '3005550107', '2023-01-16', 'Contabilidad',      'Asistente Administrativo', TRUE),
    ('1000200308', 'Jorge Iván',       'Sánchez Zapata',    'jorge.sanchez@adminpro.test',     '3005550108', '2020-02-10', 'Tesorería',         'Coordinador',              TRUE),
    ('1000200309', 'Diana Patricia',   'Hernández Mejía',   'diana.hernandez@adminpro.test',   '3005550109', '2021-05-03', 'Tesorería',         'Analista',                 TRUE),
    ('1000200310', 'Mauricio',         'Torres Agudelo',    'mauricio.torres@adminpro.test',   '3005550110', '2019-07-08', 'Compras',           'Director de Área',         TRUE),
    ('1000200311', 'Valentina',        'Quintero Salazar',  'valentina.quintero@adminpro.test','3005550111', '2022-08-01', 'Compras',           'Analista Senior',          TRUE),
    ('1000200312', 'Andrés Felipe',    'Díaz Montoya',      'andres.diaz@adminpro.test',       '3005550112', '2024-02-05', 'Compras',           'Auxiliar Administrativo',  TRUE),
    ('1000200313', 'Catalina',         'Vargas Rincón',     'catalina.vargas@adminpro.test',   '3005550113', '2018-04-23', 'Comercial',         'Director de Área',         TRUE),
    ('1000200314', 'Felipe',           'Jaramillo Duque',   'felipe.jaramillo@adminpro.test',  '3005550114', '2021-10-18', 'Comercial',         'Coordinador',              TRUE),
    ('1000200315', 'María Camila',     'Arango Betancur',   'camila.arango@adminpro.test',     '3005550115', '2023-06-13', 'Comercial',         'Analista',                 TRUE),
    ('1000200316', 'Santiago',         'Ríos Cardona',      'santiago.rios@adminpro.test',     '3005550116', '2022-01-24', 'Comercial',         'Analista',                 TRUE),
    ('1000200317', 'Luisa Fernanda',   'Ortiz Bedoya',      'luisa.ortiz@adminpro.test',       '3005550117', '2024-09-02', 'Comercial',         'Asistente Administrativo', TRUE),
    ('1000200318', 'Óscar Javier',     'Pérez Castro',      'oscar.perez@adminpro.test',       '3005550118', '2019-03-11', 'Tecnología',        'Director de Área',         TRUE),
    ('1000200319', 'Daniela',          'Moreno Suárez',     'daniela.moreno@adminpro.test',    '3005550119', '2021-02-15', 'Tecnología',        'Analista Senior',          TRUE),
    ('1000200320', 'Camilo Andrés',    'Rojas Gutiérrez',   'camilo.rojas@adminpro.test',      '3005550120', '2023-03-06', 'Tecnología',        'Analista',                 TRUE),
    ('1000200321', 'Juan Pablo',       'Acosta Navarro',    'juan.acosta@adminpro.test',       '3005550121', '2025-07-14', 'Tecnología',        'Aprendiz SENA',            TRUE),
    ('1000200322', 'Ángela María',     'Cruz Forero',       'angela.cruz@adminpro.test',       '3005550122', '2020-10-05', 'Jurídica',          'Abogado',                  TRUE),
    ('1000200323', 'Ricardo',          'Mendoza Parra',     'ricardo.mendoza@adminpro.test',   '3005550123', '2017-12-04', 'Jurídica',          'Director de Área',         TRUE),
    ('1000200324', 'Sofía',            'Guzmán Pineda',     'sofia.guzman@adminpro.test',      '3005550124', '2024-04-15', 'Gerencia',          'Asistente Administrativo', TRUE),
    ('1000200325', 'Héctor Fabio',     'Bermúdez Lozano',   'hector.bermudez@adminpro.test',   '3005550125', '2016-05-09', 'Tesorería',         'Analista',                 FALSE),
    ('1000200326', 'Gloria Inés',      'Valencia Ochoa',    'gloria.valencia@adminpro.test',   '3005550126', '2015-09-21', 'Archivo Histórico', 'Auxiliar Administrativo',  FALSE)
) AS e (cedula, nombres, apellidos, correo, telefono, fecha_ingreso, departamento, cargo, activo)
JOIN departamentos d ON d.nombre = e.departamento
JOIN cargos c ON c.nombre = e.cargo
ON CONFLICT DO NOTHING;

-- Usuarios: uno por rol. El hash corresponde a la contraseña de la cabecera. -----

INSERT INTO usuarios (correo, password_hash, nombre_visible, rol_id, empleado_id, debe_cambiar_password)
SELECT u.correo,
       '$2b$10$XfDs34BS5oqxTpBDJTaWLe0RPgIPmS3x6wvdTUSlQKedmVUiykLSO',
       u.nombre,
       r.id,
       e.id,
       FALSE
FROM (VALUES
    ('admin@adminpro.test',      'Andrea Gómez',    'ADMINISTRADOR', '1000200301'),
    ('supervisor@adminpro.test', 'Julián Ramírez',  'SUPERVISOR',    '1000200302'),
    ('auxiliar@adminpro.test',   'Laura Peña',      'AUXILIAR',      '1000200303'),
    ('consulta@adminpro.test',   'Sebastián Muñoz', 'CONSULTA',      '1000200304')
) AS u (correo, nombre, rol, cedula)
JOIN roles r ON r.codigo = u.rol
JOIN empleados e ON e.cedula = u.cedula
ON CONFLICT DO NOTHING;

-- Documentos ----------------------------------------------------------------
-- fecha_documento es fija porque el consecutivo lleva el año. El vencimiento es
-- relativo al día de la carga, para que el dashboard siempre tenga alertas.
-- Hay documentos de 2025 y 2026 para mostrar el reinicio anual del consecutivo.

INSERT INTO documentos (consecutivo, titulo, descripcion, categoria_id, responsable_id, estado,
                        fecha_documento, fecha_vencimiento, activo, created_at, updated_at, created_by, updated_by)
SELECT d.consecutivo, d.titulo, d.descripcion, c.id, e.id, d.estado,
       d.fecha_documento::DATE,
       CASE WHEN d.vence_en_dias IS NULL THEN NULL ELSE CURRENT_DATE + d.vence_en_dias END,
       d.activo,
       (d.fecha_documento || ' 09:00:00-05')::TIMESTAMPTZ,
       (d.fecha_documento || ' 09:00:00-05')::TIMESTAMPTZ,
       u.id, u.id
FROM (VALUES
    ('DOC-2025-000001', 'Contrato de arrendamiento sede principal',         'Oficinas del centro de Medellín',          'Contratos',          '1000200323', 'FINALIZADO',  '2025-01-20', 420,  TRUE),
    ('DOC-2025-000002', 'Reglamento interno de trabajo',                    'Versión vigente aprobada por Gerencia',    'Políticas internas', '1000200302', 'FINALIZADO',  '2025-03-11', NULL, TRUE),
    ('DOC-2025-000003', 'Póliza de responsabilidad civil 2025',             'Cobertura anual',                          'Pólizas y seguros',  '1000200323', 'FINALIZADO',  '2025-06-02', -40,  TRUE),
    ('DOC-2025-000004', 'Acta de asamblea ordinaria de accionistas',        NULL,                                       'Actas',              '1000200301', 'FINALIZADO',  '2025-03-28', NULL, TRUE),
    ('DOC-2026-000001', 'Certificado de existencia y representación legal', 'Expedido por la Cámara de Comercio',       'Cámara de Comercio', '1000200322', 'APROBADO',    '2026-01-15', 12,   TRUE),
    ('DOC-2026-000002', 'RUT actualizado de la empresa',                    NULL,                                       'Tributario',         '1000200305', 'FINALIZADO',  '2026-01-22', NULL, TRUE),
    ('DOC-2026-000003', 'Declaración de renta año gravable 2025',           'Formulario 110',                           'Tributario',         '1000200305', 'APROBADO',    '2026-02-10', 25,   TRUE),
    ('DOC-2026-000004', 'Contrato de soporte de infraestructura',           'Proveedor de nube y copias de seguridad',  'Contratos',          '1000200318', 'EN_REVISION', '2026-03-04', 28,   TRUE),
    ('DOC-2026-000005', 'Póliza de cumplimiento contrato comercial',        NULL,                                       'Pólizas y seguros',  '1000200323', 'EN_REVISION', '2026-03-18', 6,    TRUE),
    ('DOC-2026-000006', 'Afiliaciones a seguridad social primer semestre',  'Soportes de EPS, ARL y pensión',           'Talento Humano',     '1000200302', 'APROBADO',    '2026-04-01', NULL, TRUE),
    ('DOC-2026-000007', 'Acta de comité de convivencia laboral',            NULL,                                       'Actas',              '1000200302', 'FINALIZADO',  '2026-04-16', NULL, TRUE),
    ('DOC-2026-000008', 'Política de tratamiento de datos personales',      'Ajustes según la Ley 1581 de 2012',        'Políticas internas', '1000200322', 'PENDIENTE',   '2026-05-05', NULL, TRUE),
    ('DOC-2026-000009', 'Factura de compra de equipos de cómputo',          'Renovación de portátiles',                 'Facturación',        '1000200311', 'FINALIZADO',  '2026-05-19', NULL, TRUE),
    ('DOC-2026-000010', 'Contrato de prestación de servicios de aseo',      NULL,                                       'Contratos',          '1000200310', 'PENDIENTE',   '2026-06-09', -8,   TRUE),
    ('DOC-2026-000011', 'Certificado de retención en la fuente',            NULL,                                       'Tributario',         '1000200307', 'PENDIENTE',   '2026-07-01', 45,   TRUE),
    ('DOC-2026-000012', 'Manual de seguridad de la información',            'Primera versión para revisión',            'Políticas internas', '1000200319', 'EN_REVISION', '2026-07-21', NULL, TRUE),
    ('DOC-2026-000013', 'Acta de comité de compras',                        NULL,                                       'Actas',              '1000200310', 'APROBADO',    '2026-08-11', NULL, TRUE),
    ('DOC-2026-000014', 'Póliza de equipos electrónicos',                   'Vence pronto: gestionar renovación',       'Pólizas y seguros',  '1000200325', 'PENDIENTE',   '2026-08-25', 18,   TRUE),
    ('DOC-2026-000015', 'Factura de servicios públicos de la sede',         NULL,                                       'Facturación',        '1000200309', 'REGISTRADO',  '2026-09-08', 3,    TRUE),
    ('DOC-2026-000016', 'Hoja de vida de candidato a analista',             NULL,                                       'Talento Humano',     '1000200303', 'REGISTRADO',  '2026-09-22', NULL, TRUE),
    ('DOC-2026-000017', 'Contrato de aprendizaje SENA',                     NULL,                                       'Contratos',          '1000200302', 'REGISTRADO',  '2026-10-06', 200,  TRUE),
    ('DOC-2026-000018', 'Borrador de propuesta comercial',                  'Registrado por error, desactivado',        'Contratos',          '1000200314', 'REGISTRADO',  '2026-10-20', NULL, FALSE)
) AS d (consecutivo, titulo, descripcion, categoria, cedula_responsable, estado, fecha_documento, vence_en_dias, activo)
JOIN categorias_documento c ON c.nombre = d.categoria
JOIN empleados e ON e.cedula = d.cedula_responsable
JOIN usuarios u ON u.correo = 'auxiliar@adminpro.test'
ON CONFLICT DO NOTHING;

-- Un adjunto por cada documento que ya salió de Registrado, porque enviarlo lo exige.
-- Solo son metadatos: la clave y el hash se derivan del consecutivo para ser estables.
-- El backend debe crear los archivos de ejemplo al arrancar en dev y demo (Fase 6).

INSERT INTO documento_adjuntos (documento_id, nombre_original, storage_key, tipo_mime, tamano_bytes,
                                sha256, created_at, updated_at, created_by, updated_by)
SELECT d.id,
       d.consecutivo || '.pdf',
       md5(d.consecutivo)::UUID,
       'application/pdf',
       204800,
       encode(sha256(convert_to(d.consecutivo, 'UTF8')), 'hex'),
       d.created_at, d.created_at, d.created_by, d.created_by
FROM documentos d
WHERE d.estado <> 'REGISTRADO'
ON CONFLICT DO NOTHING;

-- Historial: reconstruye el camino de cada documento hasta su estado actual.

INSERT INTO documento_historial (documento_id, accion, estado_nuevo, comentario, created_at, updated_at, created_by, updated_by)
SELECT d.id, 'CREADO', 'REGISTRADO', NULL, d.created_at, d.created_at, d.created_by, d.created_by
FROM documentos d
WHERE NOT EXISTS (SELECT 1 FROM documento_historial h WHERE h.documento_id = d.id);

INSERT INTO documento_historial (documento_id, accion, estado_anterior, estado_nuevo, created_at, updated_at, created_by, updated_by)
SELECT d.id, 'CAMBIO_ESTADO', p.anterior, p.nuevo,
       d.created_at + p.orden * INTERVAL '1 day',
       d.created_at + p.orden * INTERVAL '1 day',
       u.id, u.id
FROM documentos d
JOIN (VALUES
    (1, 1, 'REGISTRADO',  'PENDIENTE',   'auxiliar@adminpro.test'),
    (2, 2, 'PENDIENTE',   'EN_REVISION', 'supervisor@adminpro.test'),
    (3, 3, 'EN_REVISION', 'APROBADO',    'supervisor@adminpro.test'),
    (4, 4, 'APROBADO',    'FINALIZADO',  'supervisor@adminpro.test')
) AS p (orden, nivel, anterior, nuevo, correo)
  ON p.nivel <= CASE d.estado
                    WHEN 'PENDIENTE'   THEN 1
                    WHEN 'EN_REVISION' THEN 2
                    WHEN 'APROBADO'    THEN 3
                    WHEN 'FINALIZADO'  THEN 4
                    ELSE 0
                END
JOIN usuarios u ON u.correo = p.correo
WHERE NOT EXISTS (
    SELECT 1 FROM documento_historial h
    WHERE h.documento_id = d.id AND h.accion = 'CAMBIO_ESTADO'
);

-- Un ejemplo de devolución, para que se vea en la línea de tiempo.
INSERT INTO documento_historial (documento_id, accion, estado_anterior, estado_nuevo, comentario, created_at, updated_at, created_by, updated_by)
SELECT d.id, 'CAMBIO_ESTADO', p.anterior, p.nuevo, p.comentario,
       d.created_at + p.dias * INTERVAL '1 day',
       d.created_at + p.dias * INTERVAL '1 day',
       u.id, u.id
FROM documentos d
JOIN (VALUES
    ('PENDIENTE',   'EN_REVISION', NULL,                                            2, 'supervisor@adminpro.test'),
    ('EN_REVISION', 'PENDIENTE',   'Falta incluir la finalidad del tratamiento.',   3, 'supervisor@adminpro.test')
) AS p (anterior, nuevo, comentario, dias, correo) ON TRUE
JOIN usuarios u ON u.correo = p.correo
WHERE d.consecutivo = 'DOC-2026-000008'
  AND NOT EXISTS (
      SELECT 1 FROM documento_historial h
      WHERE h.documento_id = d.id AND h.estado_anterior = 'EN_REVISION'
  );

-- Secuencias: deben quedar en el último número usado, o el siguiente documento
-- chocaría con uno de prueba.

INSERT INTO secuencias_consecutivo (tipo, anio, ultimo_valor)
SELECT 'DOC', substring(consecutivo FROM 5 FOR 4)::SMALLINT, max(substring(consecutivo FROM 10)::BIGINT)
FROM documentos
GROUP BY 2
ON CONFLICT (tipo, anio) DO UPDATE
    SET ultimo_valor = GREATEST(secuencias_consecutivo.ultimo_valor, EXCLUDED.ultimo_valor);

-- Auditoría de ejemplo para la actividad reciente del dashboard.

INSERT INTO auditoria (entidad, entidad_id, accion, valor_nuevo, ip, created_at, updated_at, created_by, updated_by)
SELECT 'DOCUMENTO', d.id, 'CREAR',
       jsonb_build_object('consecutivo', d.consecutivo, 'titulo', d.titulo, 'estado', 'REGISTRADO'),
       '127.0.0.1', d.created_at, d.created_at, d.created_by, d.created_by
FROM documentos d
WHERE NOT EXISTS (
    SELECT 1 FROM auditoria a WHERE a.entidad = 'DOCUMENTO' AND a.entidad_id = d.id AND a.accion = 'CREAR'
);

INSERT INTO auditoria (entidad, entidad_id, accion, valor_anterior, valor_nuevo, ip, created_at, updated_at, created_by, updated_by)
SELECT 'EMPLEADO', e.id, 'DESACTIVAR',
       '{"activo": true}'::JSONB, '{"activo": false}'::JSONB,
       '127.0.0.1', now() - INTERVAL '10 days', now() - INTERVAL '10 days', u.id, u.id
FROM empleados e
JOIN usuarios u ON u.correo = 'admin@adminpro.test'
WHERE NOT e.activo
  AND NOT EXISTS (
      SELECT 1 FROM auditoria a WHERE a.entidad = 'EMPLEADO' AND a.entidad_id = e.id AND a.accion = 'DESACTIVAR'
  );
