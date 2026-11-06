import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { FechaBogotaPipe } from '../pipes/fecha-bogota.pipe';

export interface EventoTiempo {
  id: number;
  titulo: string;
  detalle?: string | null;
  autor?: string | null;
  fecha: string;
}

@Component({
  selector: 'ui-linea-tiempo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FechaBogotaPipe],
  template: `
    <ol class="border-l border-linea-fuerte">
      @for (evento of eventos(); track evento.id) {
        <li class="relative pb-5 pl-5 last:pb-0">
          <span class="absolute top-1.5 -left-[5px] size-[9px] rounded-full bg-tinta" aria-hidden="true"></span>
          <p class="text-sm font-medium">{{ evento.titulo }}</p>
          @if (evento.detalle) {
            <p class="mt-0.5 text-sm whitespace-pre-line text-tenue">{{ evento.detalle }}</p>
          }
          <p class="mt-0.5 text-xs text-tenue">
            {{ evento.autor ?? 'Sistema' }}, {{ evento.fecha | fechaBogota: true }}
          </p>
        </li>
      }
    </ol>
  `,
})
export class LineaTiempo {
  readonly eventos = input.required<EventoTiempo[]>();
}
