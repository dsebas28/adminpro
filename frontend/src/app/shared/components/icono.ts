import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Trazos de 24x24. Son pocos, así que no justifican una librería de iconos. */
const TRAZOS = {
  inicio: ['M3 11.5 12 4l9 7.5', 'M5.5 10v9.5h13V10', 'M10 19.5v-5.5h4v5.5'],
  personas: [
    'M16 20v-1.5a3.5 3.5 0 0 0-3.5-3.5h-6A3.5 3.5 0 0 0 3 18.5V20',
    'M9.5 11.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z',
    'M21 20v-1.5a3.5 3.5 0 0 0-2.6-3.4',
    'M15.5 4.7a3.5 3.5 0 0 1 0 6.6',
  ],
  documento: ['M6 3h8l5 5v13H6V3Z', 'M14 3v5h5', 'M9 13h7', 'M9 17h7'],
  usuario: [
    'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z',
    'M4.5 20.5a7.5 7.5 0 0 1 15 0',
  ],
  historial: ['M3.5 12a8.5 8.5 0 1 0 2.6-6.1L3.5 8.5', 'M3.5 3.5v5h5', 'M12 7.5V12l3 2'],
  sol: [
    'M12 16.5a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9Z',
    'M12 2.5v2',
    'M12 19.5v2',
    'M2.5 12h2',
    'M19.5 12h2',
    'm5.3 5.3 1.4 1.4',
    'm17.3 17.3 1.4 1.4',
    'm5.3 18.7 1.4-1.4',
    'm17.3 6.7 1.4-1.4',
  ],
  luna: ['M20.5 14.2A8.5 8.5 0 1 1 9.8 3.5a6.8 6.8 0 0 0 10.7 10.7Z'],
  menu: ['M4 6.5h16', 'M4 12h16', 'M4 17.5h16'],
  panel: ['M4 4h16v16H4V4Z', 'M9.5 4v16'],
  salir: ['M9.5 20.5H4.5v-17h5', 'M15.5 16.5 20 12l-4.5-4.5', 'M20 12H9.5'],
  llave: ['M7.5 21a5.5 5.5 0 1 0 0-11 5.5 5.5 0 0 0 0 11Z', 'm21 3-9.4 9.4', 'm15.5 8.5 3 3 3-3-3-3'],
  cerrar: ['m6 6 12 12', 'M18 6 6 18'],
  ojo: ['M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z', 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z'],
  ojoTachado: ['M4 4l16 16', 'M9.9 5.8a9.5 9.5 0 0 1 2.1-.3c6 0 9.5 6.5 9.5 6.5a16 16 0 0 1-2.9 3.6', 'M6.3 7.5A15.5 15.5 0 0 0 2.5 12S6 18.5 12 18.5a9.3 9.3 0 0 0 4.6-1.2', 'M10 10a3 3 0 0 0 4.1 4.1'],
  flechaArriba: ['M12 19V5', 'm6 11 6-6 6 6'],
  flechaAbajo: ['M12 5v14', 'm6 13 6 6 6-6'],
  anterior: ['m14.5 6-6 6 6 6'],
  siguiente: ['m9.5 6 6 6-6 6'],
  buscar: ['M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14Z', 'm20 20-4-4'],
  bandeja: ['M3.5 13.5 6.5 5h11l3 8.5V19h-17v-5.5Z', 'M3.5 13.5h5.5l1 2.5h4l1-2.5h5.5'],
  alerta: ['M12 4 2.5 20h19L12 4Z', 'M12 10v4.5', 'M12 17.2v.3'],
  check: ['m5 12.5 4.5 4.5L19 7.5'],
  candado: ['M6 11h12v9.5H6V11Z', 'M8.5 11V7.5a3.5 3.5 0 0 1 7 0V11'],
} as const;

export type NombreIcono = keyof typeof TRAZOS;

@Component({
  selector: 'ui-icono',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-flex shrink-0', 'aria-hidden': 'true' },
  template: `
    <svg
      [attr.width]="tamano()"
      [attr.height]="tamano()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.7"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      @for (trazo of trazos(); track trazo) {
        <path [attr.d]="trazo" />
      }
    </svg>
  `,
})
export class Icono {
  readonly nombre = input.required<NombreIcono>();
  readonly tamano = input(20);

  protected readonly trazos = computed<readonly string[]>(() => TRAZOS[this.nombre()]);
}
