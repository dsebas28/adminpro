import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

import { Icono } from './icono';

/** Campo de búsqueda. Espera a que el usuario deje de escribir antes de consultar. */
@Component({
  selector: 'ui-busqueda',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { class: 'relative block' },
  template: `
    <span class="pointer-events-none absolute inset-y-0 left-3 flex items-center text-tenue">
      <ui-icono nombre="buscar" [tamano]="16" />
    </span>
    <input
      type="search"
      class="block h-10 w-full rounded-control border border-linea-fuerte bg-superficie pr-3 pl-9 text-sm text-texto placeholder:text-tenue"
      [attr.aria-label]="etiqueta()"
      [placeholder]="etiqueta()"
      (input)="escrito.next($any($event.target).value)"
    />
  `,
})
export class Busqueda {
  readonly etiqueta = input.required<string>();
  readonly buscar = output<string>();

  protected readonly escrito = new Subject<string>();

  constructor() {
    this.escrito
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe((texto) => this.buscar.emit(texto.trim()));
  }
}
