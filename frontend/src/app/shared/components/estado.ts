import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type TonoEstado = 'neutro' | 'info' | 'aviso' | 'exito' | 'peligro';

const TONOS: Record<TonoEstado, string> = {
  neutro: 'bg-neutro-suave text-neutro',
  info: 'bg-tinta-suave text-tinta',
  aviso: 'bg-aviso-suave text-aviso',
  exito: 'bg-exito-suave text-exito',
  peligro: 'bg-peligro-suave text-peligro',
};

/** Badge de estado. Es rectangular, como el recuadro de un sello, no una píldora. */
@Component({
  selector: 'ui-estado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': 'clases()' },
  template: `<ng-content />`,
})
export class Estado {
  readonly tono = input<TonoEstado>('neutro');

  protected readonly clases = computed(
    () =>
      `inline-flex items-center rounded-[3px] px-2 py-0.5 text-xs font-semibold ${TONOS[this.tono()]}`,
  );
}
