import { Routes } from '@angular/router';

import { authGuard, invitadoGuard, permisoGuard, sesionGuard } from './core/guards/guards';

const authLayout = () => import('./layouts/auth-layout/auth-layout').then((m) => m.AuthLayout);
const pendiente = () => import('./features/pendiente/pendiente').then((m) => m.Pendiente);

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
        title: 'Empleados | AdminPro',
        data: { miga: 'Empleados' },
        canActivate: [permisoGuard('EMPLEADOS_LEER')],
        loadComponent: pendiente,
      },
      {
        path: 'documentos',
        title: 'Documentos | AdminPro',
        data: { miga: 'Documentos' },
        canActivate: [permisoGuard('DOCUMENTOS_LEER')],
        loadComponent: pendiente,
      },
      {
        path: 'usuarios',
        title: 'Usuarios | AdminPro',
        data: { miga: 'Usuarios' },
        canActivate: [permisoGuard('USUARIOS_LEER')],
        loadComponent: pendiente,
      },
      {
        path: 'auditoria',
        title: 'Auditoría | AdminPro',
        data: { miga: 'Auditoría' },
        canActivate: [permisoGuard('AUDITORIA_LEER')],
        loadComponent: pendiente,
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
