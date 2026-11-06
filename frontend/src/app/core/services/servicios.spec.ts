import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { FechaBogotaPipe } from '../../shared/pipes/fecha-bogota.pipe';
import { ConfirmacionService } from './confirmacion.service';
import { TemaService } from './tema.service';
import { ToastService } from './toast.service';

describe('TemaService', () => {
  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme');
    localStorage.clear();
  });

  it('alterna el tema, lo aplica al documento y lo recuerda', () => {
    const tema = TestBed.inject(TemaService);
    expect(tema.tema()).toBe('light');

    tema.alternar();

    expect(tema.tema()).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem('adminpro.tema')).toBe('dark');
  });

  it('funciona aunque el navegador bloquee el almacenamiento', () => {
    const tema = TestBed.inject(TemaService);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });

    expect(() => tema.aplicar('dark')).not.toThrow();
    expect(tema.tema()).toBe('dark');
    vi.restoreAllMocks();
  });
});

describe('ToastService', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('agrega un aviso y lo retira solo', () => {
    const toasts = TestBed.inject(ToastService);

    toasts.exito('Guardado');
    expect(toasts.toasts()).toHaveLength(1);

    vi.advanceTimersByTime(5000);
    expect(toasts.toasts()).toHaveLength(0);
  });

  it('no repite el mismo aviso', () => {
    const toasts = TestBed.inject(ToastService);

    toasts.error('Sin conexión');
    toasts.error('Sin conexión');

    expect(toasts.toasts()).toHaveLength(1);
  });
});

describe('ConfirmacionService', () => {
  it('resuelve con la respuesta del usuario', async () => {
    const servicio = TestBed.inject(ConfirmacionService);

    const respuesta = servicio.pedir({ titulo: 't', mensaje: 'm', accion: 'a' });
    expect(servicio.pendiente()).not.toBeNull();
    servicio.responder(true);

    await expect(respuesta).resolves.toBe(true);
    expect(servicio.pendiente()).toBeNull();
  });
});

describe('FechaBogotaPipe', () => {
  const pipe = new FechaBogotaPipe();

  it('una fecha de calendario no se corre al día anterior', () => {
    expect(pipe.transform('2026-01-01')).toBe('1/01/2026');
  });

  it('convierte una hora UTC a la hora de Bogotá', () => {
    expect(pipe.transform('2026-01-01T03:00:00Z')).toBe('31/12/2025');
  });

  it('un valor vacío o inválido no se muestra', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform('no-es-fecha')).toBe('');
  });
});
