import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';

/** Estas rutas usan la cookie. Renovar ante su propio 401 crearía un ciclo infinito. */
const RUTAS_DE_SESION = ['/auth/login', '/auth/refresh', '/auth/logout'];

function conToken<T>(peticion: HttpRequest<T>, token: string | null): HttpRequest<T> {
  return token
    ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : peticion;
}

/** Agrega el token y, si venció, lo renueva y repite la petición una sola vez. */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const auth = inject(AuthService);

  const esApi = peticion.url.startsWith('/api/');
  if (!esApi || RUTAS_DE_SESION.some((ruta) => peticion.url.endsWith(ruta))) {
    return siguiente(peticion);
  }

  return siguiente(conToken(peticion, auth.token())).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return auth.renovar().pipe(
        switchMap(() => siguiente(conToken(peticion, auth.token()))),
        catchError((fallo: unknown) => {
          auth.cerrarSesionLocal();
          return throwError(() => fallo);
        }),
      );
    }),
  );
};
