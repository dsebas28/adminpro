import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { BarController, BarElement, CategoryScale, Chart, ChartConfiguration, LinearScale, Plugin, Tooltip } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

import { TemaService } from '../../core/services/tema.service';
import { ConteoEstado } from '../../models/negocio';
import { ESTADOS } from '../../shared/utils/etiquetas';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip);

/** Escribe el total al final de cada barra, para leer el dato sin pasar el cursor. */
const valores: Plugin<'bar'> = {
  id: 'valores',
  afterDatasetsDraw(grafico) {
    const { ctx } = grafico;
    const estilo = getComputedStyle(grafico.canvas);
    ctx.save();
    ctx.fillStyle = estilo.getPropertyValue('--texto').trim();
    ctx.font = "600 13px 'Hanken Grotesk Variable', sans-serif";
    ctx.textBaseline = 'middle';
    grafico.getDatasetMeta(0).data.forEach((barra, indice) => {
      ctx.fillText(String(grafico.data.datasets[0].data[indice]), barra.x + 8, barra.y);
    });
    ctx.restore();
  },
};

/**
 * Una sola serie en el orden fijo del flujo: barras de un solo color con su valor directo.
 * Una dona dificultaría comparar los estados y perdería ese orden.
 */
@Component({
  selector: 'app-grafico-estados',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BaseChartDirective],
  template: `
    <div class="h-56">
      @if (config(); as grafico) {
        <canvas
          baseChart
          type="bar"
          role="img"
          [attr.aria-label]="resumenAccesible()"
          [data]="grafico.data"
          [options]="grafico.options"
          [plugins]="plugins"
        ></canvas>
      }
    </div>
    <table class="sr-only">
      <caption>Documentos activos por estado</caption>
      <tbody>
        @for (fila of datos(); track fila.estado) {
          <tr>
            <th scope="row">{{ etiqueta(fila) }}</th>
            <td>{{ fila.total }}</td>
          </tr>
        }
      </tbody>
    </table>
  `,
})
export class GraficoEstados {
  private readonly tema = inject(TemaService);

  readonly datos = input.required<ConteoEstado[]>();

  protected readonly plugins = [valores];

  protected readonly resumenAccesible = computed(
    () => 'Documentos por estado: ' + this.datos().map((f) => `${this.etiqueta(f)} ${f.total}`).join(', '),
  );

  /** Depende del tema: los colores del lienzo no reaccionan a CSS y hay que redibujarlos. */
  protected readonly config = computed<Pick<ChartConfiguration<'bar'>, 'data' | 'options'>>(() => {
    this.tema.tema();
    const css = getComputedStyle(document.documentElement);
    const color = (nombre: string): string => css.getPropertyValue(nombre).trim();
    const fuente = { family: "'Hanken Grotesk Variable', sans-serif", size: 13 };

    return {
      data: {
        labels: this.datos().map((fila) => this.etiqueta(fila)),
        datasets: [
          {
            data: this.datos().map((fila) => fila.total),
            backgroundColor: color('--tinta'),
            hoverBackgroundColor: color('--tinta-hover'),
            borderRadius: 4,
            barThickness: 16,
          },
        ],
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        layout: { padding: { right: 28 } },
        plugins: {
          legend: { display: false },
          tooltip: {
            displayColors: false,
            callbacks: { label: (item) => `${item.parsed.x} documentos` },
          },
        },
        scales: {
          x: { display: false, beginAtZero: true, grid: { display: false } },
          y: {
            grid: { display: false },
            border: { display: false },
            ticks: { color: color('--tenue'), font: fuente },
          },
        },
      },
    };
  });

  protected etiqueta(fila: ConteoEstado): string {
    return ESTADOS[fila.estado].texto;
  }
}
