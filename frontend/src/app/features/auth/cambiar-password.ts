import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Boton } from '../../shared/components/boton';
import { Campo, CampoInput } from '../../shared/components/campo';
import { aplicarErroresServidor, coincideCon, passwordValida } from '../../shared/utils/formularios';

@Component({
  selector: 'app-cambiar-password',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, Boton, Campo, CampoInput],
  template: `
    <h1 class="text-3xl font-bold">Cambia tu contraseña</h1>
    <p class="mt-2 text-tenue">
      @if (obligatorio()) {
        Tu contraseña es temporal. Crea una nueva para continuar.
      } @else {
        Las demás sesiones abiertas se cerrarán.
      }
    </p>

    @if (error(); as mensaje) {
      <p role="alert" class="mt-6 rounded-control bg-peligro-suave px-3 py-2.5 text-sm text-peligro">
        {{ mensaje }}
      </p>
    }

    <form class="mt-6 space-y-4" [formGroup]="formulario" (ngSubmit)="enviar()" novalidate>
      <ui-campo etiqueta="Contraseña actual">
        <input uiInput type="password" autocomplete="current-password" formControlName="passwordActual" />
      </ui-campo>
      <ui-campo etiqueta="Contraseña nueva" ayuda="Mínimo 10 caracteres.">
        <input uiInput type="password" autocomplete="new-password" formControlName="passwordNueva" />
      </ui-campo>
      <ui-campo etiqueta="Repite la contraseña nueva">
        <input uiInput type="password" autocomplete="new-password" formControlName="confirmacion" />
      </ui-campo>

      <button uiBoton type="submit" class="w-full" [cargando]="enviando()">Cambiar contraseña</button>
      @if (!obligatorio()) {
        <a uiBoton routerLink="/" variante="fantasma" class="w-full">Cancelar</a>
      } @else {
        <button uiBoton type="button" variante="fantasma" class="w-full" (click)="auth.logout()">
          Cerrar sesión
        </button>
      }
    </form>
  `,
})
export class CambiarPassword {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    passwordActual: ['', Validators.required],
    passwordNueva: ['', [Validators.required, passwordValida]],
    confirmacion: ['', [Validators.required, coincideCon('passwordNueva')]],
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly obligatorio = computed(() => this.auth.usuario()?.debeCambiarPassword ?? false);

  constructor() {
    // La confirmación depende de otro campo: se revalida cuando ese campo cambia.
    this.formulario.controls.passwordNueva.valueChanges.subscribe(() =>
      this.formulario.controls.confirmacion.updateValueAndValidity(),
    );
  }

  protected enviar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const { passwordActual, passwordNueva } = this.formulario.getRawValue();

    this.auth.cambiarPassword({ passwordActual, passwordNueva }).subscribe({
      next: () => {
        this.toasts.exito('Contraseña cambiada');
        void this.router.navigateByUrl('/');
      },
      error: (fallo: unknown) => {
        this.enviando.set(false);
        this.error.set(aplicarErroresServidor(this.formulario, fallo));
      },
    });
  }
}
