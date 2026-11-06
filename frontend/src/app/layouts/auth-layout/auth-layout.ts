import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { TemaService } from '../../core/services/tema.service';
import { Boton } from '../../shared/components/boton';
import { Icono } from '../../shared/components/icono';
import { Sello } from './sello';

@Component({
  selector: 'app-auth-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, Boton, Icono, Sello],
  template: `
    <div class="grid min-h-dvh lg:grid-cols-[minmax(26rem,32rem)_1fr]">
      <main class="flex flex-col bg-superficie px-6 py-6 sm:px-12">
        <div class="flex items-center justify-between">
          <span class="text-xl font-bold tracking-tight text-tinta">AdminPro</span>
          <button
            uiBoton
            type="button"
            variante="fantasma"
            tamano="icono"
            [attr.aria-label]="tema.tema() === 'dark' ? 'Usar tema claro' : 'Usar tema oscuro'"
            (click)="tema.alternar()"
          >
            <ui-icono [nombre]="tema.tema() === 'dark' ? 'sol' : 'luna'" />
          </button>
        </div>
        <div class="my-auto w-full max-w-sm self-center py-10">
          <router-outlet />
        </div>
      </main>
      <aside class="hidden items-center justify-center overflow-hidden bg-fondo p-12 lg:flex">
        <app-sello />
      </aside>
    </div>
  `,
})
export class AuthLayout {
  protected readonly tema = inject(TemaService);
}
