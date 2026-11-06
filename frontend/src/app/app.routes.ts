import { Routes } from '@angular/router';

import { authGuard, invitadoGuard, permisoGuard, sesionGuard } from './core/guards/guards';

const authLayout = () => import('./layouts/auth-layout/auth-layout').then((m) => m.AuthLayout);

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [invitadoGuard],
    loadComponent: authLayout,
    children: [
      {
        path: '',
        title: 'Inicia sesión | AdminPro',
        loadComponent: () => import('./features/auth/login').then((m) => m.Login),
      },
    ],
  },
  {
    path: 'cambiar-password',
    canActivate: [sesionGuard],
    loadComponent: authLayout,
    children: [
      {
        path: '',
        title: 'Cambia tu contraseña | AdminPro',
        loadComponent: () => import('./features/auth/cambiar-password').then((m) => m.CambiarPassword),
      },
    ],
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layouts/main-layout/main-layout').then((m) => m.MainLayout),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Inicio | AdminPro',
        data: { miga: 'Inicio' },
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'empleados',
        data: { miga: 'Empleados' },
        canActivate: [permisoGuard('EMPLEADOS_LEER')],
        children: [
          {
            path: '',
            pathMatch: 'full',
            title: 'Empleados | AdminPro',
            loadComponent: () => import('./features/empleados/empleados-lista').then((m) => m.EmpleadosLista),
          },
          {
            path: 'nuevo',
            title: 'Nuevo empleado | AdminPro',
            data: { miga: 'Nuevo' },
            canActivate: [permisoGuard('EMPLEADOS_ESCRIBIR')],
            loadComponent: () => import('./features/empleados/empleado-form').then((m) => m.EmpleadoForm),
          },
          {
            path: ':id/editar',
            title: 'Editar empleado | AdminPro',
            data: { miga: 'Editar' },
            canActivate: [permisoGuard('EMPLEADOS_ESCRIBIR')],
            loadComponent: () => import('./features/empleados/empleado-form').then((m) => m.EmpleadoForm),
          },
          {
            path: ':id',
            title: 'Empleado | AdminPro',
            data: { miga: 'Detalle' },
            loadComponent: () => import('./features/empleados/empleado-detalle').then((m) => m.EmpleadoDetalle),
          },
        ],
      },
      {
        path: 'documentos',
        data: { miga: 'Documentos' },
        canActivate: [permisoGuard('DOCUMENTOS_LEER')],
        children: [
          {
            path: '',
            pathMatch: 'full',
            title: 'Documentos | AdminPro',
            loadComponent: () => import('./features/documentos/documentos-lista').then((m) => m.DocumentosLista),
          },
          {
            path: 'nuevo',
            title: 'Registrar documento | AdminPro',
            data: { miga: 'Registrar' },
            canActivate: [permisoGuard('DOCUMENTOS_ESCRIBIR')],
            loadComponent: () => import('./features/documentos/documento-form').then((m) => m.DocumentoForm),
          },
          {
            path: ':id/editar',
            title: 'Editar documento | AdminPro',
            data: { miga: 'Editar' },
            canActivate: [permisoGuard('DOCUMENTOS_ESCRIBIR')],
            loadComponent: () => import('./features/documentos/documento-form').then((m) => m.DocumentoForm),
          },
          {
            path: ':id',
            title: 'Documento | AdminPro',
            data: { miga: 'Detalle' },
            loadComponent: () => import('./features/documentos/documento-detalle').then((m) => m.DocumentoDetalle),
          },
        ],
      },
      {
        path: 'catalogos',
        title: 'Catálogos | AdminPro',
        data: { miga: 'Catálogos' },
        canActivate: [permisoGuard('EMPLEADOS_LEER')],
        loadComponent: () => import('./features/catalogos/catalogos').then((m) => m.Catalogos),
      },
      {
        path: 'usuarios',
        title: 'Usuarios | AdminPro',
        data: { miga: 'Usuarios' },
        canActivate: [permisoGuard('USUARIOS_LEER')],
        loadComponent: () => import('./features/usuarios/usuarios').then((m) => m.Usuarios),
      },
      {
        path: 'auditoria',
        title: 'Auditoría | AdminPro',
        data: { miga: 'Auditoría' },
        canActivate: [permisoGuard('AUDITORIA_LEER')],
        loadComponent: () => import('./features/auditoria/auditoria').then((m) => m.Auditoria),
      },
      {
        path: 'sistema-diseno',
        title: 'Sistema de diseño | AdminPro',
        data: { miga: 'Sistema de diseño' },
        loadComponent: () => import('./features/sistema-diseno/sistema-diseno').then((m) => m.SistemaDiseno),
      },
      {
        path: 'acceso-denegado',
        title: 'Sin acceso | AdminPro',
        data: { miga: 'Sin acceso' },
        loadComponent: () => import('./features/errores/acceso-denegado').then((m) => m.AccesoDenegado),
      },
      {
        path: '**',
        title: 'Página no encontrada | AdminPro',
        data: { miga: 'Página no encontrada' },
        loadComponent: () => import('./features/errores/no-encontrada').then((m) => m.NoEncontrada),
      },
    ],
  },
];
