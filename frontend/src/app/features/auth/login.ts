import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { rutaInternaSegura } from '../../core/guards/guards';
import { Boton } from '../../shared/components/boton';
import { Campo, CampoInput } from '../../shared/components/campo';
import { Icono } from '../../shared/components/icono';
import { PATRON_CORREO, problemaDe } from '../../shared/utils/formularios';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Boton, Campo, CampoInput, Icono],
  template: `
    <h1 class="text-3xl font-bold">Inicia sesión</h1>
    <p class="mt-2 text-tenue">Entra con tu correo de trabajo.</p>

    @if (error(); as mensaje) {
      <p role="alert" class="mt-6 rounded-control bg-peligro-suave px-3 py-2.5 text-sm text-peligro">
        {{ mensaje }}
      </p>
    }

    <form class="mt-6 space-y-4" [formGroup]="formulario" (ngSubmit)="enviar()" novalidate>
      <ui-campo etiqueta="Correo">
        <input uiInput type="email" autocomplete="username" formControlName="correo" />
      </ui-campo>

      <ui-campo etiqueta="Contraseña">
        <div class="relative">
          <input
            uiInput
            class="pr-11"
            [type]="verPassword() ? 'text' : 'password'"
            autocomplete="current-password"
            formControlName="password"
          />
          <button
            type="button"
            class="absolute inset-y-0 right-0 flex w-10 items-center justify-center rounded-control text-tenue hover:text-texto"
            [attr.aria-label]="verPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
            [attr.aria-pressed]="verPassword()"
            (click)="verPassword.set(!verPassword())"
          >
            <ui-icono [nombre]="verPassword() ? 'ojoTachado' : 'ojo'" [tamano]="18" />
          </button>
        </div>
      </ui-campo>

      <button uiBoton type="submit" class="w-full" [cargando]="enviando()">Iniciar sesión</button>
    </form>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    correo: ['', [Validators.required, Validators.pattern(PATRON_CORREO)]],
    password: ['', Validators.required],
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verPassword = signal(false);

  protected enviar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);

    this.auth.login(this.formulario.getRawValue()).subscribe({
      next: (usuario) => {
        const volver = this.ruta.snapshot.queryParamMap.get('volver');
        void this.router.navigateByUrl(
          usuario.debeCambiarPassword ? '/cambiar-password' : rutaInternaSegura(volver),
        );
      },
      error: (fallo: unknown) => {
        this.enviando.set(false);
        this.error.set(problemaDe(fallo)?.detail ?? 'No se pudo iniciar sesión. Inténtalo de nuevo.');
      },
    });
  }
}
