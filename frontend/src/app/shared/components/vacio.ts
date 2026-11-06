import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Icono, NombreIcono } from './icono';

@Component({
  selector: 'ui-vacio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { class: 'flex flex-col items-center px-6 py-12 text-center' },
  template: `
    <span class="text-tenue"><ui-icono [nombre]="icono()" [tamano]="32" /></span>
    <h3 class="mt-3 text-base font-semibold">{{ titulo() }}</h3>
    @if (mensaje()) {
      <p class="mt-1 max-w-md text-sm text-tenue">{{ mensaje() }}</p>
    }
    <div class="mt-4 empty:hidden"><ng-content /></div>
  `,
})
export class Vacio {
  readonly titulo = input.required<string>();
  readonly mensaje = input<string>();
  readonly icono = input<NombreIcono>('bandeja');
}
