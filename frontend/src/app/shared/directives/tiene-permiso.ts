import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';
import { Permiso } from '../../models/auth';

/** Oculta un elemento si el usuario no tiene el permiso. No reemplaza la validación del backend. */
@Directive({ selector: '[tienePermiso]' })
export class TienePermiso {
  private readonly auth = inject(AuthService);
  private readonly plantilla = inject(TemplateRef);
  private readonly contenedor = inject(ViewContainerRef);

  readonly tienePermiso = input.required<Permiso>();

  constructor() {
    effect(() => {
      this.contenedor.clear();
      if (this.auth.usuario()?.permisos.includes(this.tienePermiso())) {
        this.contenedor.createEmbeddedView(this.plantilla);
      }
    });
  }
}
