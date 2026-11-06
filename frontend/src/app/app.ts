import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Confirmacion } from './shared/components/confirmacion';
import { Toasts } from './shared/components/toasts';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, Confirmacion, Toasts],
  template: `
    <router-outlet />
    <ui-toasts />
    <ui-confirmacion />
  `,
})
export class App {}
