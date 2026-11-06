import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ToastService } from '../../core/services/toast.service';
import { Boton } from './boton';
import { Icono } from './icono';

@Component({
  selector: 'ui-toasts',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, Icono],
  host: {
    class: 'fixed right-4 bottom-4 left-4 z-50 flex flex-col items-end gap-2 sm:left-auto',
    'aria-live': 'polite',
  },
  template: `
    @for (toast of servicio.toasts(); track toast.id) {
      <div
        class="flex w-full items-start gap-2.5 rounded-control border border-linea bg-superficie py-2 pr-1.5 pl-3.5 text-sm shadow-lg sm:w-96"
        [class.border-l-4]="true"
        [class.border-l-exito]="toast.tipo === 'exito'"
        [class.border-l-peligro]="toast.tipo === 'error'"
        [attr.role]="toast.tipo === 'error' ? 'alert' : 'status'"
      >
        <span class="mt-1.5" [class.text-exito]="toast.tipo === 'exito'" [class.text-peligro]="toast.tipo === 'error'">
          <ui-icono [nombre]="toast.tipo === 'exito' ? 'check' : 'alerta'" [tamano]="18" />
        </span>
        <p class="flex-1 py-1">{{ toast.mensaje }}</p>
        <button uiBoton type="button" variante="fantasma" tamano="pequeno" aria-label="Cerrar aviso" (click)="servicio.cerrar(toast.id)">
          <ui-icono nombre="cerrar" [tamano]="16" />
        </button>
      </div>
    }
  `,
})
export class Toasts {
  protected readonly servicio = inject(ToastService);
}
