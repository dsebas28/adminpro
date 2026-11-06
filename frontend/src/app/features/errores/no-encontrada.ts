import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Boton } from '../../shared/components/boton';
import { Vacio } from '../../shared/components/vacio';

@Component({
  selector: 'app-no-encontrada',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Boton, Vacio],
  template: `
    <ui-vacio titulo="Esta página no existe" mensaje="Revisa la dirección o vuelve al inicio.">
      <a uiBoton routerLink="/" variante="secundario">Volver al inicio</a>
    </ui-vacio>
  `,
})
export class NoEncontrada {}
