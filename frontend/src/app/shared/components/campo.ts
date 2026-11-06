import {
  ChangeDetectionStrategy,
  Component,
  Directive,
  DestroyRef,
  OnInit,
  computed,
  contentChild,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgControl } from '@angular/forms';

import { mensajeDeError } from '../utils/formularios';

let contador = 0;

/** Marca el input, select o textarea de un formulario. Lo conecta con su etiqueta y su error. */
@Directive({
  selector: 'input[uiInput], select[uiInput], textarea[uiInput]',
  host: {
    '[id]': 'id',
    '[attr.aria-invalid]': 'error() ? "true" : null',
    '[attr.aria-describedby]': 'error() ? id + "-error" : null',
    '[class]': 'clases()',
  },
})
export class CampoInput implements OnInit {
  private readonly destruir = inject(DestroyRef);
  readonly control = inject(NgControl, { optional: true, self: true });

  readonly id = `campo-${++contador}`;
  readonly error = signal<string | null>(null);

  protected readonly clases = computed(
    () =>
      'block w-full rounded-control border bg-superficie px-3 h-10 text-sm text-texto ' +
      'placeholder:text-tenue disabled:bg-superficie-2 disabled:text-tenue ' +
      (this.error() ? 'border-peligro' : 'border-linea-fuerte'),
  );

  ngOnInit(): void {
    const control = this.control?.control;
    if (!control) {
      return;
    }
    const actualizar = (): void => {
      const mostrar = control.invalid && (control.touched || control.dirty);
      this.error.set(mostrar ? mensajeDeError(control.errors) : null);
    };
    actualizar();
    control.events.pipe(takeUntilDestroyed(this.destruir)).subscribe(actualizar);
  }
}

/** Etiqueta, control y mensaje de validación visible. */
@Component({
  selector: 'ui-campo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <label [attr.for]="entrada()?.id" class="mb-1.5 block text-sm font-medium text-texto">
      {{ etiqueta() }}
      @if (!obligatorio()) {
        <span class="font-normal text-tenue">(opcional)</span>
      }
    </label>
    <ng-content />
    @if (entrada()?.error(); as error) {
      <p [id]="entrada()?.id + '-error'" class="mt-1.5 text-[0.8125rem] text-peligro">{{ error }}</p>
    } @else if (ayuda()) {
      <p class="mt-1.5 text-[0.8125rem] text-tenue">{{ ayuda() }}</p>
    }
  `,
})
export class Campo {
  readonly etiqueta = input.required<string>();
  readonly ayuda = input<string>();
  readonly obligatorio = input(true);

  protected readonly entrada = contentChild(CampoInput);
}
