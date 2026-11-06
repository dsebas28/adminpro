import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  Observable,
  catchError,
  finalize,
  firstValueFrom,
  map,
  of,
  shareReplay,
  switchMap,
  tap,
} from 'rxjs';

import {
  CambiarPasswordRequest,
  LoginRequest,
  Permiso,
  TokenResponse,
  UsuarioActual,
} from '../../models/auth';

const API = '/api/v1/auth';

/**
 * Sesión del usuario. El access token vive solo en memoria y el refresh token en una
 * cookie HttpOnly que JavaScript no puede leer: un ataque XSS no puede robar la sesión
 * para usarla después (D-08). Por eso, al recargar la página, se renueva con la cookie.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sesion = signal<TokenResponse | null>(null);
  private renovacionEnCurso: Observable<TokenResponse> | null = null;

  readonly usuario = computed<UsuarioActual | null>(() => this.sesion()?.usuario ?? null);
  readonly token = computed(() => this.sesion()?.accessToken ?? null);
  readonly autenticado = computed(() => this.sesion() !== null);

  login(credenciales: LoginRequest): Observable<UsuarioActual> {
    return this.http.post<TokenResponse>(`${API}/login`, credenciales).pipe(
      tap((respuesta) => this.sesion.set(respuesta)),
      map((respuesta) => respuesta.usuario),
    );
  }

  /**
   * Todas las peticiones que reciban 401 al mismo tiempo comparten una sola renovación.
   * Si cada una renovara por su cuenta, el backend vería un token reutilizado.
   */
  renovar(): Observable<TokenResponse> {
    this.renovacionEnCurso ??= this.http.post<TokenResponse>(`${API}/refresh`, null).pipe(
      tap((respuesta) => this.sesion.set(respuesta)),
      finalize(() => (this.renovacionEnCurso = null)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.renovacionEnCurso;
  }

  /** Se ejecuta al arrancar. No tener sesión es un caso normal, no un error. */
  restaurarSesion(): Promise<void> {
    return firstValueFrom(
      this.renovar().pipe(
        map(() => undefined),
        catchError(() => of(undefined)),
      ),
    );
  }

  cambiarPassword(solicitud: CambiarPasswordRequest): Observable<TokenResponse> {
    return this.http
      .post<void>(`${API}/cambiar-password`, solicitud)
      .pipe(switchMap(() => this.renovar()));
  }

  logout(): void {
    this.http
      .post<void>(`${API}/logout`, null)
      .pipe(catchError(() => of(undefined)))
      .subscribe(() => this.cerrarSesionLocal());
  }

  cerrarSesionLocal(): void {
    this.sesion.set(null);
    void this.router.navigateByUrl('/login');
  }

  tienePermiso(permiso: Permiso): boolean {
    return this.usuario()?.permisos.includes(permiso) ?? false;
  }
}
