import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';

import { Permiso, UsuarioActual } from '../../models/auth';
import { AuthService } from '../auth/auth.service';
import {
  authGuard,
  invitadoGuard,
  permisoGuard,
  rutaInternaSegura,
  sesionGuard,
} from './guards';

describe('guards', () => {
  const usuario = signal<UsuarioActual | null>(null);

  const conUsuario = (permisos: Permiso[] = [], debeCambiarPassword = false): void =>
    usuario.set({ id: 1, correo: 'a', nombreVisible: 'A', rol: 'X', permisos, debeCambiarPassword });

  const ejecutar = (guard: CanActivateFn, url = '/empleados') =>
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );

  const destino = (resultado: unknown): string =>
    TestBed.inject(Router).serializeUrl(resultado as UrlTree);

  beforeEach(() => {
    usuario.set(null);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            autenticado: () => usuario() !== null,
            usuario,
            tienePermiso: (p: Permiso) => usuario()?.permisos.includes(p) ?? false,
          },
        },
      ],
    });
  });

  it('authGuard lleva al login recordando a dónde iba', () => {
    expect(destino(ejecutar(authGuard))).toBe('/login?volver=%2Fempleados');
  });

  it('authGuard deja pasar con sesión', () => {
    conUsuario();
    expect(ejecutar(authGuard)).toBe(true);
  });

  it('authGuard obliga a cambiar la contraseña temporal', () => {
    conUsuario([], true);
    expect(destino(ejecutar(authGuard))).toBe('/cambiar-password');
  });

  it('sesionGuard permite la pantalla de cambio con contraseña temporal', () => {
    conUsuario([], true);
    expect(ejecutar(sesionGuard)).toBe(true);
  });

  it('sesionGuard exige sesión', () => {
    expect(destino(ejecutar(sesionGuard))).toBe('/login');
  });

  it('invitadoGuard saca del login a quien ya tiene sesión', () => {
    conUsuario();
    expect(destino(ejecutar(invitadoGuard))).toBe('/');
  });

  it('invitadoGuard deja entrar sin sesión', () => {
    expect(ejecutar(invitadoGuard)).toBe(true);
  });

  it('permisoGuard deja pasar con el permiso', () => {
    conUsuario(['USUARIOS_LEER']);
    expect(ejecutar(permisoGuard('USUARIOS_LEER'))).toBe(true);
  });

  it('permisoGuard rechaza sin el permiso', () => {
    conUsuario(['EMPLEADOS_LEER']);
    expect(destino(ejecutar(permisoGuard('USUARIOS_LEER')))).toBe('/acceso-denegado');
  });

  describe('rutaInternaSegura', () => {
    it('acepta rutas internas', () => {
      expect(rutaInternaSegura('/empleados?page=2')).toBe('/empleados?page=2');
    });

    it.each([
      ['https://malo.example'],
      ['//malo.example'],
      ['/\\malo.example'],
      ['javascript:alert(1)'],
      [''],
      [null],
    ])('rechaza %s', (ruta) => {
      expect(rutaInternaSegura(ruta)).toBe('/');
    });
  });
});
