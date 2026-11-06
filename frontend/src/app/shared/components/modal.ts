import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  effect,
  input,
  output,
  viewChild,
} from '@angular/core';

import { Boton } from './boton';
import { Icono } from './icono';

/**
 * Usa el elemento nativo dialog: el navegador ya resuelve mantener el foco adentro,
 * cerrar con Escape y devolver el foco al salir, sin código propio que mantener.
 */
@Component({
  selector: 'ui-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, Icono],
  template: `
    <dialog
      #dialogo
      class="m-auto w-[calc(100%-2rem)] max-w-lg rounded-panel border border-linea bg-superficie p-0 text-texto"
      [attr.aria-labelledby]="idTitulo"
      (close)="cerrado.emit()"
    >
      <header class="flex items-center justify-between gap-4 border-b border-linea px-5 py-3.5">
        <h2 [id]="idTitulo" class="text-base font-semibold">{{ titulo() }}</h2>
        <button
          uiBoton
          type="button"
          variante="fantasma"
          tamano="icono"
          aria-label="Cerrar"
          (click)="dialogo.close()"
        >
          <ui-icono nombre="cerrar" />
        </button>
      </header>
      <div class="px-5 py-4"><ng-content /></div>
      <footer class="flex justify-end gap-2 px-5 pb-4 empty:hidden">
        <ng-content select="[acciones]" />
      </footer>
    </dialog>
  `,
})
export class Modal {
  readonly titulo = input.required<string>();
  readonly abierto = input(false);
  readonly cerrado = output<void>();

  protected readonly idTitulo = `modal-${Math.random().toString(36).slice(2)}`;
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  constructor() {
    effect(() => {
      const elemento = this.dialogo().nativeElement;
      if (this.abierto() && !elemento.open) {
        elemento.showModal();
      } else if (!this.abierto() && elemento.open) {
        elemento.close();
      }
    });
  }
}
