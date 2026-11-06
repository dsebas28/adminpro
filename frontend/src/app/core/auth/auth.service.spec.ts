import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';

import { TokenResponse } from '../../models/auth';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { AuthService } from './auth.service';

const sesion = (token = 'token-1', debeCambiarPassword = false): TokenResponse => ({
  accessToken: token,
  expiresIn: 900,
  usuario: {
    id: 1,
    correo: 'a@adminpro.test',
    nombreVisible: 'Ana',
    rol: 'AUXILIAR',
    permisos: ['EMPLEADOS_LEER'],
    debeCambiarPassword,
  },
});

describe('AuthService e interceptor', () => {
  let auth: AuthService;
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  function iniciarSesion(token = 'token-1'): void {
    auth.login({ correo: 'a@adminpro.test', password: 'x' }).subscribe();
    backend.expectOne('/api/v1/auth/login').flush(sesion(token));
  }

  it('guarda la sesión en memoria y expone los permisos', () => {
    iniciarSesion();

    expect(auth.autenticado()).toBe(true);
    expect(auth.tienePermiso('EMPLEADOS_LEER')).toBe(true);
    expect(auth.tienePermiso('USUARIOS_LEER')).toBe(false);
  });

  it('nunca guarda el token en el almacenamiento del navegador', () => {
    iniciarSesion('token-secreto');

    expect(JSON.stringify({ ...localStorage })).not.toContain('token-secreto');
    expect(JSON.stringify({ ...sessionStorage })).not.toContain('token-secreto');
  });

  it('restaurar sesión sin cookie no es un error', async () => {
    const restaurada = auth.restaurarSesion();
    backend
      .expectOne('/api/v1/auth/refresh')
      .flush({ code: 'NO_AUTENTICADO' }, { status: 401, statusText: 'Unauthorized' });

    await expect(restaurada).resolves.toBeUndefined();
    expect(auth.autenticado()).toBe(false);
  });

  it('el interceptor agrega el token a la API', () => {
    iniciarSesion('token-1');

    http.get('/api/v1/usuarios').subscribe();

    const peticion = backend.expectOne('/api/v1/usuarios');
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer token-1');
    peticion.flush({});
  });

  it('el interceptor no envía el token a otros dominios', () => {
    iniciarSesion();

    http.get('https://otro.example/datos').subscribe();

    const peticion = backend.expectOne('https://otro.example/datos');
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });

  it('ante un 401 renueva y repite la petición con el token nuevo', () => {
    iniciarSesion('viejo');
    let respuesta: unknown;

    http.get('/api/v1/usuarios').subscribe((r) => (respuesta = r));
    backend.expectOne('/api/v1/usuarios').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/v1/auth/refresh').flush(sesion('nuevo'));

    const repetida = backend.expectOne('/api/v1/usuarios');
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer nuevo');
    repetida.flush({ ok: true });
    expect(respuesta).toEqual({ ok: true });
  });

  it('varios 401 simultáneos comparten una sola renovación', () => {
    iniciarSesion('viejo');

    http.get('/api/v1/usuarios').subscribe();
    http.get('/api/v1/roles').subscribe();
    backend.expectOne('/api/v1/usuarios').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/v1/roles').flush({}, { status: 401, statusText: 'Unauthorized' });

    backend.expectOne('/api/v1/auth/refresh').flush(sesion('nuevo'));
    backend.expectOne('/api/v1/usuarios').flush({});
    backend.expectOne('/api/v1/roles').flush({});
  });

  it('si la renovación falla, cierra la sesión y lleva al login', () => {
    iniciarSesion('viejo');
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    let fallo: unknown;

    http.get('/api/v1/usuarios').subscribe({ error: (e) => (fallo = e) });
    backend.expectOne('/api/v1/usuarios').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(fallo).toBeTruthy();
    expect(auth.autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith('/login');
  });

  it('un 401 en el login no intenta renovar', () => {
    let fallo: unknown;

    auth.login({ correo: 'a@adminpro.test', password: 'mala' }).subscribe({ error: (e) => (fallo = e) });
    backend.expectOne('/api/v1/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(fallo).toBeTruthy();
    backend.expectNone('/api/v1/auth/refresh');
  });

  it('cambiar la contraseña renueva el token para quitar la restricción', () => {
    auth.login({ correo: 'a@adminpro.test', password: 'x' }).subscribe();
    backend.expectOne('/api/v1/auth/login').flush(sesion('temporal', true));
    expect(auth.usuario()?.debeCambiarPassword).toBe(true);

    auth.cambiarPassword({ passwordActual: 'x', passwordNueva: 'NuevaClave.2026' }).subscribe();
    backend.expectOne('/api/v1/auth/cambiar-password').flush(null);
    backend.expectOne('/api/v1/auth/refresh').flush(sesion('definitivo', false));

    expect(auth.usuario()?.debeCambiarPassword).toBe(false);
    expect(auth.token()).toBe('definitivo');
  });

  it('el logout limpia la sesión aunque el servidor falle', () => {
    iniciarSesion();
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    auth.logout();
    backend.expectOne('/api/v1/auth/logout').flush({}, { status: 500, statusText: 'Error' });

    expect(auth.autenticado()).toBe(false);
  });
});
