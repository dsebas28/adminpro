import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Marcador mientras cargan los datos. Se oculta a los lectores de pantalla. */
@Component({
  selector: 'ui-esqueleto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'block animate-pulse rounded-control bg-superficie-2',
    'aria-hidden': 'true',
    '[style.height]': 'alto()',
    '[style.width]': 'ancho()',
  },
  template: ``,
})
export class Esqueleto {
  readonly alto = input('1rem');
  readonly ancho = input('100%');
}
