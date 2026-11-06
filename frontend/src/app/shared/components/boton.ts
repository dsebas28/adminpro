import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type VarianteBoton = 'primario' | 'secundario' | 'fantasma' | 'peligro';

const BASE =
  'inline-flex items-center justify-center gap-2 rounded-control font-semibold transition-colors ' +
  'disabled:opacity-55 whitespace-nowrap';

const VARIANTES: Record<VarianteBoton, string> = {
  primario: 'bg-tinta text-sobre-tinta hover:enabled:bg-tinta-hover',
  secundario: 'border border-linea-fuerte bg-superficie text-texto hover:enabled:bg-superficie-2',
  fantasma: 'text-texto hover:enabled:bg-superficie-2',
  peligro: 'bg-peligro text-superficie hover:enabled:bg-peligro-hover',
};

const TAMANOS = {
  normal: 'h-10 px-4 text-sm',
  pequeno: 'h-8 px-3 text-[0.8125rem]',
  icono: 'size-10',
};

@Component({
  selector: 'button[uiBoton], a[uiBoton]',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': 'clases()',
    '[attr.aria-busy]': 'cargando() || null',
    '[attr.disabled]': 'cargando() || deshabilitado() ? "" : null',
  },
  template: `
    @if (cargando()) {
      <span
        class="size-4 animate-spin rounded-full border-2 border-current border-t-transparent"
        aria-hidden="true"
      ></span>
    }
    <ng-content />
  `,
})
export class Boton {
  readonly variante = input<VarianteBoton>('primario');
  readonly tamano = input<keyof typeof TAMANOS>('normal');
  readonly cargando = input(false);
  readonly deshabilitado = input(false);

  protected readonly clases = computed(
    () => `${BASE} ${VARIANTES[this.variante()]} ${TAMANOS[this.tamano()]}`,
  );
}
