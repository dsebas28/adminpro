import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Boton } from '../../shared/components/boton';
import { Vacio } from '../../shared/components/vacio';

@Component({
  selector: 'app-acceso-denegado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Boton, Vacio],
  template: `
    <ui-vacio
      icono="candado"
      titulo="No tienes acceso a esta sección"
      mensaje="Pídele a un Administrador que revise el rol de tu usuario."
    >
      <a uiBoton routerLink="/" variante="secundario">Volver al inicio</a>
    </ui-vacio>
  `,
})
export class AccesoDenegado {}
