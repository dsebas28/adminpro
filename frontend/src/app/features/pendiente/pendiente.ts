import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Vacio } from '../../shared/components/vacio';

/** Sección cuyo módulo se construye en la Fase 6. La ruta y su permiso ya están activos. */
@Component({
  selector: 'app-pendiente',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Vacio],
  template: `
    <h1 class="text-2xl font-bold">{{ miga() }}</h1>
    <ui-vacio
      class="mt-6 rounded-panel border border-linea bg-superficie"
      titulo="Esta sección todavía no está disponible"
      mensaje="Tu usuario ya tiene acceso. La pantalla se habilita en la siguiente entrega."
    />
  `,
})
export class Pendiente {
  readonly miga = input.required<string>();
}
