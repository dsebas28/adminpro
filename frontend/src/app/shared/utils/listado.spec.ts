import { HttpParams } from '@angular/common/http';
import { DestroyRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { PageResponse } from '../../models/api';
import { Listado } from './listado';
import { tamanoLegible, textoHistorial } from './etiquetas';

const pagina = (content: string[], totalElements = content.length): PageResponse<string> => ({
  content,
  page: 0,
  size: 20,
  totalElements,
  totalPages: 1,
});

const destruir = () => TestBed.inject(DestroyRef);
const microtarea = () => new Promise<void>((resolver) => queueMicrotask(resolver));

describe('Listado', () => {
  it('consulta al crearse con el orden y los filtros iniciales', async () => {
    const consultar = vi.fn((_: HttpParams) => of(pagina(['a'])));

    const listado = new Listado<string>(consultar, destruir(), { campo: 'nombre', direccion: 'asc' }, { activo: true });
    await microtarea();

    const parametros = consultar.mock.calls[0][0];
    expect(parametros.get('sort')).toBe('nombre,asc');
    expect(parametros.get('activo')).toBe('true');
    expect(parametros.get('page')).toBe('0');
    expect(listado.filas()).toEqual(['a']);
    expect(listado.cargando()).toBe(false);
  });

  it('un filtro vuelve a la primera página y omite los valores vacíos', async () => {
    const consultar = vi.fn((_: HttpParams) => of(pagina([])));
    const listado = new Listado<string>(consultar, destruir());
    await microtarea();

    listado.irA(3);
    listado.filtrar({ q: 'ana', estado: '' });

    const parametros = consultar.mock.lastCall![0];
    expect(parametros.get('page')).toBe('0');
    expect(parametros.get('q')).toBe('ana');
    expect(parametros.has('estado')).toBe(false);
  });

  it('una respuesta vieja no pisa a la más reciente', async () => {
    const lenta = new Subject<PageResponse<string>>();
    const consultar = vi.fn().mockReturnValueOnce(lenta).mockReturnValueOnce(of(pagina(['nueva'])));
    const listado = new Listado<string>(consultar, destruir());
    await microtarea();

    listado.recargar();
    lenta.next(pagina(['vieja']));

    expect(listado.filas()).toEqual(['nueva']);
  });

  it('un fallo no rompe el listado y se puede reintentar', async () => {
    const consultar = vi.fn().mockReturnValueOnce(throwError(() => new Error('red'))).mockReturnValueOnce(of(pagina(['a'])));
    const listado = new Listado<string>(consultar, destruir());
    await microtarea();

    expect(listado.fallo()).toBe(true);
    expect(listado.cargando()).toBe(false);

    listado.recargar();
    expect(listado.fallo()).toBe(false);
    expect(listado.filas()).toEqual(['a']);
  });
});

describe('etiquetas', () => {
  it('una devolución se describe como tal', () => {
    expect(textoHistorial('CAMBIO_ESTADO', 'EN_REVISION', 'PENDIENTE')).toBe('Devolvió el documento');
    expect(textoHistorial('CAMBIO_ESTADO', 'PENDIENTE', 'EN_REVISION')).toBe('Pasó a En revisión');
  });

  it('muestra los tamaños de forma legible', () => {
    expect(tamanoLegible(512)).toBe('512 B');
    expect(tamanoLegible(2048)).toBe('2 KB');
    expect(tamanoLegible(1572864)).toBe('1,5 MB');
  });
});
