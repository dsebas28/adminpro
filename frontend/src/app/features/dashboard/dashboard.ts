import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';
import { Vacio } from '../../shared/components/vacio';

@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Vacio],
  template: `
    <h1 class="text-2xl font-bold">Hola, {{ nombre() }}</h1>
    <ui-vacio
      class="mt-6 rounded-panel border border-linea bg-superficie"
      titulo="Tus indicadores aparecerán aquí"
      mensaje="Verás empleados, documentos por estado y próximos vencimientos."
    />
  `,
})
export class Dashboard {
  private readonly auth = inject(AuthService);
  protected readonly nombre = computed(() => this.auth.usuario()?.nombreVisible.split(' ')[0] ?? '');
}
