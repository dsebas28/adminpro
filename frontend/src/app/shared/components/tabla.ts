import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  Directive,
  TemplateRef,
  computed,
  contentChild,
  inject,
  input,
  output,
} from '@angular/core';

import { Esqueleto } from './esqueleto';
import { Icono } from './icono';

export interface ColumnaTabla {
  clave: string;
  titulo: string;
  ordenable?: boolean;
  derecha?: boolean;
}

export interface OrdenTabla {
  campo: string;
  direccion: 'asc' | 'desc';
}

/** Plantilla de las celdas de una fila: <ng-template uiFila let-fila>...</ng-template> */
@Directive({ selector: 'ng-template[uiFila]' })
export class FilaTabla {
  readonly plantilla = inject(TemplateRef);
}

/** La tabla solo emite eventos: el orden y la paginación los resuelve el servidor. */
@Component({
  selector: 'ui-tabla',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgTemplateOutlet, Esqueleto, Icono],
  host: { class: 'block overflow-x-auto' },
  template: `
    <table class="w-full border-collapse text-sm">
      <caption class="sr-only">{{ descripcion() }}</caption>
      <thead>
        <tr class="border-b border-linea-fuerte text-left">
          @for (columna of columnas(); track columna.clave) {
            <th
              scope="col"
              class="px-3 py-2.5 font-semibold whitespace-nowrap text-tenue"
              [class.text-right]="columna.derecha"
              [attr.aria-sort]="ariaOrden(columna)"
            >
              @if (columna.ordenable) {
                <button
                  type="button"
                  class="inline-flex items-center gap-1 rounded-control hover:text-texto"
                  (click)="alternarOrden(columna)"
                >
                  {{ columna.titulo }}
                  @if (orden()?.campo === columna.clave) {
                    <ui-icono
                      [nombre]="orden()?.direccion === 'asc' ? 'flechaArriba' : 'flechaAbajo'"
                      [tamano]="14"
                    />
                  }
                </button>
              } @else {
                {{ columna.titulo }}
              }
            </th>
          }
        </tr>
      </thead>
      <tbody>
        @if (cargando()) {
          @for (fila of filasEsqueleto; track fila) {
            <tr class="border-b border-linea">
              @for (columna of columnas(); track columna.clave) {
                <td class="px-3 py-3"><ui-esqueleto ancho="70%" /></td>
              }
            </tr>
          }
        } @else {
          @for (fila of filas(); track $index) {
            <tr class="border-b border-linea hover:bg-superficie-2">
              <ng-container [ngTemplateOutlet]="plantilla()" [ngTemplateOutletContext]="{ $implicit: fila }" />
            </tr>
          }
        }
      </tbody>
    </table>
    @if (sinDatos()) {
      <ng-content />
    }
  `,
})
export class Tabla<T> {
  readonly descripcion = input.required<string>();
  readonly columnas = input.required<ColumnaTabla[]>();
  readonly filas = input.required<T[]>();
  readonly cargando = input(false);
  readonly orden = input<OrdenTabla | null>(null);
  readonly ordenar = output<OrdenTabla>();

  protected readonly filasEsqueleto = [1, 2, 3, 4, 5];
  private readonly fila = contentChild.required(FilaTabla);
  protected readonly plantilla = computed(() => this.fila().plantilla);
  protected readonly sinDatos = computed(() => !this.cargando() && this.filas().length === 0);

  protected alternarOrden(columna: ColumnaTabla): void {
    const actual = this.orden();
    const ascendente = actual?.campo !== columna.clave || actual.direccion === 'desc';
    this.ordenar.emit({ campo: columna.clave, direccion: ascendente ? 'asc' : 'desc' });
  }

  protected ariaOrden(columna: ColumnaTabla): string | null {
    if (!columna.ordenable) {
      return null;
    }
    const actual = this.orden();
    if (actual?.campo !== columna.clave) {
      return 'none';
    }
    return actual.direccion === 'asc' ? 'ascending' : 'descending';
  }
}
