import { HttpParams } from '@angular/common/http';
import { DestroyRef, computed, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, catchError, of, switchMap, tap } from 'rxjs';

import { PageResponse } from '../../models/api';
import { OrdenTabla } from '../components/tabla';

export type Filtros = Record<string, string | number | boolean | null | undefined>;

const TAMANO = 20;

/**
 * Estado de un listado con paginación, orden y filtros en el servidor. switchMap cancela la
 * consulta anterior: si el usuario escribe rápido, una respuesta vieja no pisa a la nueva.
 */
export class Listado<T> {
  readonly pagina = signal(0);
  readonly orden = signal<OrdenTabla | null>(null);
  readonly cargando = signal(true);
  readonly fallo = signal(false);
  readonly tamano = TAMANO;

  private readonly filtros = signal<Filtros>({});
  private readonly datos = signal<PageResponse<T> | null>(null);
  private readonly pedir = new Subject<void>();

  readonly filas = computed(() => this.datos()?.content ?? []);
  readonly total = computed(() => this.datos()?.totalElements ?? 0);
  readonly totalPaginas = computed(() => this.datos()?.totalPages ?? 0);

  constructor(
    consultar: (parametros: HttpParams) => Observable<PageResponse<T>>,
    destruir: DestroyRef,
    orden: OrdenTabla | null = null,
    filtros: Filtros = {},
  ) {
    this.orden.set(orden);
    this.filtros.set(filtros);
    this.pedir
      .pipe(
        tap(() => {
          this.cargando.set(true);
          this.fallo.set(false);
        }),
        switchMap(() =>
          consultar(this.parametros()).pipe(
            catchError(() => {
              this.fallo.set(true);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(destruir),
      )
      .subscribe((respuesta) => {
        this.datos.set(respuesta);
        this.cargando.set(false);
      });
    queueMicrotask(() => this.recargar());
  }

  recargar(): void {
    this.pedir.next();
  }

  /** Un filtro nuevo vuelve a la primera página, porque la actual podría ya no existir. */
  filtrar(cambios: Filtros): void {
    this.filtros.update((actuales) => ({ ...actuales, ...cambios }));
    this.pagina.set(0);
    this.recargar();
  }

  irA(pagina: number): void {
    this.pagina.set(pagina);
    this.recargar();
  }

  ordenar(orden: OrdenTabla): void {
    this.orden.set(orden);
    this.pagina.set(0);
    this.recargar();
  }

  private parametros(): HttpParams {
    let parametros = new HttpParams().set('page', this.pagina()).set('size', this.tamano);
    const orden = this.orden();
    if (orden) {
      parametros = parametros.set('sort', `${orden.campo},${orden.direccion}`);
    }
    for (const [clave, valor] of Object.entries(this.filtros())) {
      if (valor !== null && valor !== undefined && valor !== '') {
        parametros = parametros.set(clave, valor);
      }
    }
    return parametros;
  }
}
