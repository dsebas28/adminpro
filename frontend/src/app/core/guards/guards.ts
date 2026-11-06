import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Permiso } from '../../models/auth';
import { AuthService } from '../auth/auth.service';

/*
 * Los guards solo mejoran la experiencia: evitan mostrar pantallas que van a fallar.
 * La seguridad real está en el backend, que valida cada petición con @PreAuthorize.
 */

export const authGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.autenticado()) {
    return router.createUrlTree(['/login'], { queryParams: { volver: estado.url } });
  }
  if (auth.usuario()?.debeCambiarPassword) {
    return router.createUrlTree(['/cambiar-password']);
  }
  return true;
};

export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.autenticado() ? inject(Router).createUrlTree(['/']) : true;
};

/** Para pantallas accesibles con contraseña temporal: solo exige tener sesión. */
export const sesionGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.autenticado() ? true : inject(Router).createUrlTree(['/login']);
};

export function permisoGuard(permiso: Permiso): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.tienePermiso(permiso) ? true : inject(Router).createUrlTree(['/acceso-denegado']);
  };
}

/** Evita que ?volver= lleve a otro sitio: solo se aceptan rutas internas. */
export function rutaInternaSegura(ruta: string | null): string {
  return ruta && ruta.startsWith('/') && !ruta.startsWith('//') && !ruta.includes('\\') ? ruta : '/';
}
