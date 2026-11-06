package com.adminpro.audit.entity;

public enum AccionAuditoria {
    CREAR,
    ACTUALIZAR,
    ACTIVAR,
    DESACTIVAR,
    LOGIN_OK,
    LOGIN_FALLIDO,
    CUENTA_BLOQUEADA,
    LOGOUT,
    CAMBIO_PASSWORD,
    RESTABLECER_PASSWORD,
    REUTILIZACION_TOKEN
}
