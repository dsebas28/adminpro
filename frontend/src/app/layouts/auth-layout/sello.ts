import { ChangeDetectionStrategy, Component } from '@angular/core';

import { FechaBogotaPipe } from '../../shared/pipes/fecha-bogota.pipe';

const CONSECUTIVO = '000019';

/**
 * La pieza de identidad de AdminPro: un sello de radicado con el consecutivo de los
 * documentos. Los dígitos ruedan una vez al cargar, como los de una numeradora. El
 * filtro SVG da el borde irregular de la tinta sobre papel.
 */
@Component({
  selector: 'app-sello',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FechaBogotaPipe],
  styles: `
    .digito {
      display: inline-block;
      height: 1em;
      overflow: hidden;
      line-height: 1;
      vertical-align: top;
    }

    .rodillo {
      display: block;
      animation: rodar 1.5s cubic-bezier(0.2, 0.75, 0.2, 1) both;
      animation-delay: var(--retraso);
    }

    .rodillo span {
      display: block;
      height: 1em;
    }

    @keyframes rodar {
      from {
        transform: translateY(0);
      }
      to {
        transform: translateY(calc(var(--destino) * -1em));
      }
    }
  `,
  template: `
    <svg class="absolute size-0" aria-hidden="true">
      <filter id="tinta">
        <feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" seed="7" result="ruido" />
        <feDisplacementMap in="SourceGraphic" in2="ruido" scale="1.8" result="borde" />
        <feColorMatrix
          in="ruido"
          type="matrix"
          values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 -2.6 2.05"
          result="manchas"
        />
        <feComposite in="borde" in2="manchas" operator="in" />
      </filter>
    </svg>

    <div
      class="-rotate-3 border-[3px] border-tinta p-1.5 text-tinta [filter:url(#tinta)]"
      role="img"
      [attr.aria-label]="'Sello de radicado ' + numero"
    >
      <div class="border border-tinta px-8 py-6">
        <p class="text-3xl font-extrabold tracking-tight">Radicado</p>
        <p class="mt-3 font-numerador text-[clamp(2rem,4.4vw,4rem)] leading-none font-semibold whitespace-nowrap">
          <span>DOC-{{ anio }}-</span>
          @for (digito of digitos; track $index) {
            <span class="digito" aria-hidden="true">
              <span class="rodillo" [style.--destino]="digito + 10" [style.--retraso]="$index * 90 + 'ms'">
                @for (n of tira; track $index) {
                  <span>{{ n }}</span>
                }
              </span>
            </span>
          }
        </p>
        <p class="mt-4 text-xl font-bold">{{ hoy | fechaBogota }}</p>
      </div>
    </div>
  `,
})
export class Sello {
  protected readonly hoy = new Date().toISOString();
  protected readonly anio = new Date().getFullYear();
  protected readonly numero = `DOC-${this.anio}-${CONSECUTIVO}`;
  protected readonly digitos = [...CONSECUTIVO].map(Number);

  /** Dos vueltas de 0 a 9, para que cada rodillo gire aunque su destino sea 0. */
  protected readonly tira = [...Array(20).keys()].map((n) => n % 10);
}
