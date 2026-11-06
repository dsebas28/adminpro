import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { Boton } from './boton';
import { Icono } from './icono';

@Component({
  selector: 'ui-paginador',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, Icono],
  host: { class: 'flex flex-wrap items-center justify-between gap-3 px-3 py-2.5 text-sm text-tenue' },
  template: `
    <p aria-live="polite">{{ resumen() }}</p>
    <nav class="flex items-center gap-1" aria-label="Paginación">
      <button
        uiBoton
        type="button"
        variante="fantasma"
        tamano="icono"
        aria-label="Página anterior"
        [deshabilitado]="pagina() === 0"
        (click)="cambio.emit(pagina() - 1)"
      >
        <ui-icono nombre="anterior" />
      </button>
      <span class="px-2 text-texto">Página {{ pagina() + 1 }} de {{ totalPaginas() || 1 }}</span>
      <button
        uiBoton
        type="button"
        variante="fantasma"
        tamano="icono"
        aria-label="Página siguiente"
        [deshabilitado]="pagina() + 1 >= totalPaginas()"
        (click)="cambio.emit(pagina() + 1)"
      >
        <ui-icono nombre="siguiente" />
      </button>
    </nav>
  `,
})
export class Paginador {
  readonly pagina = input.required<number>();
  readonly tamano = input.required<number>();
  readonly total = input.required<number>();
  readonly totalPaginas = input.required<number>();
  readonly cambio = output<number>();

  protected readonly resumen = computed(() => {
    if (this.total() === 0) {
      return 'Sin resultados';
    }
    const desde = this.pagina() * this.tamano() + 1;
    const hasta = Math.min(desde + this.tamano() - 1, this.total());
    return `${desde} a ${hasta} de ${this.total()}`;
  });
}
