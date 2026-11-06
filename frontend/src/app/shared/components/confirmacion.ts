import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { Boton } from './boton';
import { Modal } from './modal';

@Component({
  selector: 'ui-confirmacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, Modal],
  template: `
    <ui-modal
      [titulo]="solicitud()?.titulo ?? ''"
      [abierto]="solicitud() !== null"
      (cerrado)="servicio.responder(false)"
    >
      <p class="text-sm text-tenue">{{ solicitud()?.mensaje }}</p>
      <ng-container acciones>
        <button uiBoton type="button" variante="secundario" (click)="servicio.responder(false)">
          Cancelar
        </button>
        <button
          uiBoton
          type="button"
          [variante]="solicitud()?.peligro ? 'peligro' : 'primario'"
          (click)="servicio.responder(true)"
        >
          {{ solicitud()?.accion }}
        </button>
      </ng-container>
    </ui-modal>
  `,
})
export class Confirmacion {
  protected readonly servicio = inject(ConfirmacionService);
  protected readonly solicitud = computed(() => this.servicio.pendiente());
}
