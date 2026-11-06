-- Datos de referencia que el sistema necesita en todos los perfiles.
-- Roles y permisos del MVP según la matriz de la sección 6 del documento de Fase 1.
-- Los permisos de las iteraciones siguientes se agregan en la migración de cada una.

INSERT INTO roles (codigo, nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Administrador', 'Acceso total al sistema'),
    ('SUPERVISOR',    'Supervisor',    'Opera los módulos, aprueba documentos y consulta la auditoría'),
    ('AUXILIAR',      'Auxiliar',      'Registra y edita la información del día a día'),
    ('CONSULTA',      'Consulta',      'Acceso de solo lectura');

INSERT INTO permisos (codigo, modulo, descripcion) VALUES
    ('DASHBOARD_LEER',        'DASHBOARD',  'Ver el dashboard'),
    ('EMPLEADOS_LEER',        'EMPLEADOS',  'Ver empleados, departamentos, cargos e historial'),
    ('EMPLEADOS_ESCRIBIR',    'EMPLEADOS',  'Crear y editar empleados'),
    ('EMPLEADOS_DESACTIVAR',  'EMPLEADOS',  'Activar y desactivar empleados'),
    ('CATALOGOS_ESCRIBIR',    'CATALOGOS',  'Administrar departamentos, cargos y categorías'),
    ('DOCUMENTOS_LEER',       'DOCUMENTOS', 'Ver documentos, adjuntos, categorías e historial'),
    ('DOCUMENTOS_ESCRIBIR',   'DOCUMENTOS', 'Crear, editar y enviar documentos'),
    ('DOCUMENTOS_APROBAR',    'DOCUMENTOS', 'Revisar, aprobar, devolver y finalizar documentos'),
    ('DOCUMENTOS_DESACTIVAR', 'DOCUMENTOS', 'Activar y desactivar documentos'),
    ('USUARIOS_LEER',         'USUARIOS',   'Ver usuarios y roles'),
    ('USUARIOS_ESCRIBIR',     'USUARIOS',   'Crear y editar usuarios'),
    ('USUARIOS_DESACTIVAR',   'USUARIOS',   'Activar y desactivar usuarios'),
    ('AUDITORIA_LEER',        'AUDITORIA',  'Consultar el registro de auditoría');

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM (VALUES
    ('ADMINISTRADOR', 'DASHBOARD_LEER'),
    ('ADMINISTRADOR', 'EMPLEADOS_LEER'),
    ('ADMINISTRADOR', 'EMPLEADOS_ESCRIBIR'),
    ('ADMINISTRADOR', 'EMPLEADOS_DESACTIVAR'),
    ('ADMINISTRADOR', 'CATALOGOS_ESCRIBIR'),
    ('ADMINISTRADOR', 'DOCUMENTOS_LEER'),
    ('ADMINISTRADOR', 'DOCUMENTOS_ESCRIBIR'),
    ('ADMINISTRADOR', 'DOCUMENTOS_APROBAR'),
    ('ADMINISTRADOR', 'DOCUMENTOS_DESACTIVAR'),
    ('ADMINISTRADOR', 'USUARIOS_LEER'),
    ('ADMINISTRADOR', 'USUARIOS_ESCRIBIR'),
    ('ADMINISTRADOR', 'USUARIOS_DESACTIVAR'),
    ('ADMINISTRADOR', 'AUDITORIA_LEER'),
    ('SUPERVISOR',    'DASHBOARD_LEER'),
    ('SUPERVISOR',    'EMPLEADOS_LEER'),
    ('SUPERVISOR',    'EMPLEADOS_ESCRIBIR'),
    ('SUPERVISOR',    'DOCUMENTOS_LEER'),
    ('SUPERVISOR',    'DOCUMENTOS_ESCRIBIR'),
    ('SUPERVISOR',    'DOCUMENTOS_APROBAR'),
    ('SUPERVISOR',    'AUDITORIA_LEER'),
    ('AUXILIAR',      'DASHBOARD_LEER'),
    ('AUXILIAR',      'EMPLEADOS_LEER'),
    ('AUXILIAR',      'EMPLEADOS_ESCRIBIR'),
    ('AUXILIAR',      'DOCUMENTOS_LEER'),
    ('AUXILIAR',      'DOCUMENTOS_ESCRIBIR'),
    ('CONSULTA',      'DASHBOARD_LEER'),
    ('CONSULTA',      'EMPLEADOS_LEER'),
    ('CONSULTA',      'DOCUMENTOS_LEER')
) AS matriz (rol, permiso)
JOIN roles r ON r.codigo = matriz.rol
JOIN permisos p ON p.codigo = matriz.permiso;
