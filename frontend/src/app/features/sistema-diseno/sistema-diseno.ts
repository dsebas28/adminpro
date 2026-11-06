import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { ToastService } from '../../core/services/toast.service';
import { Boton } from '../../shared/components/boton';
import { Campo, CampoInput } from '../../shared/components/campo';
import { Esqueleto } from '../../shared/components/esqueleto';
import { Estado } from '../../shared/components/estado';
import { Modal } from '../../shared/components/modal';
import { Paginador } from '../../shared/components/paginador';
import { ColumnaTabla, FilaTabla, OrdenTabla, Tabla } from '../../shared/components/tabla';
import { Vacio } from '../../shared/components/vacio';
import { FechaBogotaPipe } from '../../shared/pipes/fecha-bogota.pipe';

interface DocumentoEjemplo {
  consecutivo: string;
  titulo: string;
  estado: string;
  tono: 'neutro' | 'info' | 'aviso' | 'exito';
  fecha: string;
}

/** Catálogo vivo de los componentes base, en los dos temas. */
@Component({
  selector: 'app-sistema-diseno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    Boton,
    Campo,
    CampoInput,
    Esqueleto,
    Estado,
    FechaBogotaPipe,
    FilaTabla,
    Modal,
    Paginador,
    Tabla,
    Vacio,
  ],
  template: `
    <h1 class="text-2xl font-bold">Sistema de diseño</h1>
    <p class="mt-1 max-w-2xl text-tenue">Los componentes con los que se construyen todas las pantallas.</p>

    <div class="mt-8 space-y-10">
      <section>
        <h2 class="text-lg font-semibold">Color</h2>
        <div class="mt-3 grid grid-cols-2 gap-px overflow-hidden rounded-panel border border-linea bg-linea sm:grid-cols-4 lg:grid-cols-8">
          @for (color of colores; track color.nombre) {
            <div class="bg-superficie p-3">
              <div class="h-12 rounded-control border border-linea" [style.background]="'var(--' + color.variable + ')'"></div>
              <p class="mt-2 text-sm font-medium">{{ color.nombre }}</p>
            </div>
          }
        </div>
      </section>

      <section>
        <h2 class="text-lg font-semibold">Tipografía</h2>
        <div class="mt-3 space-y-2">
          <p class="text-3xl font-bold">Hanken Grotesk para la interfaz</p>
          <p class="text-tenue">Los números de las tablas tienen ancho fijo para que se alineen.</p>
          <p class="font-numerador text-xl font-semibold text-tinta">DOC-2026-000019</p>
        </div>
      </section>

      <section>
        <h2 class="text-lg font-semibold">Botones</h2>
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <button uiBoton type="button">Guardar cambios</button>
          <button uiBoton type="button" variante="secundario">Cancelar</button>
          <button uiBoton type="button" variante="fantasma">Ver historial</button>
          <button uiBoton type="button" variante="peligro">Desactivar</button>
          <button uiBoton type="button" [cargando]="true">Guardando</button>
          <button uiBoton type="button" [deshabilitado]="true">No disponible</button>
        </div>
      </section>

      <section>
        <h2 class="text-lg font-semibold">Estados</h2>
        <div class="mt-3 flex flex-wrap gap-2">
          <ui-estado tono="neutro">Registrado</ui-estado>
          <ui-estado tono="aviso">Pendiente</ui-estado>
          <ui-estado tono="info">En revisión</ui-estado>
          <ui-estado tono="exito">Aprobado</ui-estado>
          <ui-estado tono="neutro">Finalizado</ui-estado>
          <ui-estado tono="peligro">Vencido</ui-estado>
        </div>
      </section>

      <section class="max-w-md">
        <h2 class="text-lg font-semibold">Formularios</h2>
        <div class="mt-3 space-y-4">
          <ui-campo etiqueta="Nombre del departamento" ayuda="Debe ser único en la empresa.">
            <input uiInput [formControl]="valido" />
          </ui-campo>
          <ui-campo etiqueta="Cédula">
            <input uiInput [formControl]="invalido" inputmode="numeric" />
          </ui-campo>
          <ui-campo etiqueta="Teléfono" [obligatorio]="false">
            <input uiInput [formControl]="opcional" inputmode="tel" />
          </ui-campo>
        </div>
      </section>

      <section>
        <h2 class="text-lg font-semibold">Tabla</h2>
        <div class="mt-3 rounded-panel border border-linea bg-superficie">
          <ui-tabla
            descripcion="Documentos de ejemplo"
            [columnas]="columnas"
            [filas]="filas()"
            [orden]="orden()"
            (ordenar)="ordenar($event)"
          >
            <ng-template uiFila let-fila>
              <td class="px-3 py-2.5 font-numerador text-[0.8125rem] whitespace-nowrap">{{ fila.consecutivo }}</td>
              <td class="px-3 py-2.5">{{ fila.titulo }}</td>
              <td class="px-3 py-2.5"><ui-estado [tono]="fila.tono">{{ fila.estado }}</ui-estado></td>
              <td class="px-3 py-2.5 text-right whitespace-nowrap">{{ fila.fecha | fechaBogota }}</td>
            </ng-template>
          </ui-tabla>
          <ui-paginador [pagina]="pagina()" [tamano]="3" [total]="9" [totalPaginas]="3" (cambio)="pagina.set($event)" />
        </div>
      </section>

      <section class="grid gap-6 md:grid-cols-2">
        <div>
          <h2 class="text-lg font-semibold">Estado vacío</h2>
          <ui-vacio
            class="mt-3 rounded-panel border border-linea bg-superficie"
            titulo="No hay documentos con estos filtros"
            mensaje="Quita un filtro o registra un documento."
          />
        </div>
        <div>
          <h2 class="text-lg font-semibold">Carga</h2>
          <div class="mt-3 space-y-3 rounded-panel border border-linea bg-superficie p-5">
            <ui-esqueleto alto="1.5rem" ancho="55%" />
            <ui-esqueleto />
            <ui-esqueleto ancho="85%" />
            <ui-esqueleto ancho="70%" />
          </div>
        </div>
      </section>

      <section>
        <h2 class="text-lg font-semibold">Avisos y diálogos</h2>
        <div class="mt-3 flex flex-wrap gap-2">
          <button uiBoton type="button" variante="secundario" (click)="toasts.exito('Documento aprobado')">
            Mostrar aviso de éxito
          </button>
          <button
            uiBoton
            type="button"
            variante="secundario"
            (click)="toasts.error('No se pudo guardar. El documento cambió mientras lo editabas.')"
          >
            Mostrar aviso de error
          </button>
          <button uiBoton type="button" variante="secundario" (click)="modalAbierto.set(true)">Abrir modal</button>
          <button uiBoton type="button" variante="secundario" (click)="confirmar()">Pedir confirmación</button>
        </div>
      </section>
    </div>

    <ui-modal titulo="Editar categoría" [abierto]="modalAbierto()" (cerrado)="modalAbierto.set(false)">
      <p class="text-sm text-tenue">El foco queda dentro del diálogo y Escape lo cierra.</p>
      <ng-container acciones>
        <button uiBoton type="button" (click)="modalAbierto.set(false)">Entendido</button>
      </ng-container>
    </ui-modal>
  `,
})
export class SistemaDiseno {
  protected readonly toasts = inject(ToastService);
  private readonly confirmacion = inject(ConfirmacionService);

  protected readonly colores = [
    { nombre: 'Fondo', variable: 'fondo' },
    { nombre: 'Superficie', variable: 'superficie' },
    { nombre: 'Línea', variable: 'linea' },
    { nombre: 'Texto', variable: 'texto' },
    { nombre: 'Tinta', variable: 'tinta' },
    { nombre: 'Éxito', variable: 'exito' },
    { nombre: 'Aviso', variable: 'aviso' },
    { nombre: 'Peligro', variable: 'peligro' },
  ];

  protected readonly valido = new FormControl('Talento Humano');
  protected readonly invalido = new FormControl('12A', [Validators.pattern(/^[0-9]{6,10}$/)]);
  protected readonly opcional = new FormControl('');

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'consecutivo', titulo: 'Número', ordenable: true },
    { clave: 'titulo', titulo: 'Título', ordenable: true },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'fecha', titulo: 'Fecha', ordenable: true, derecha: true },
  ];
  protected readonly filas = signal<DocumentoEjemplo[]>([
    { consecutivo: 'DOC-2026-000017', titulo: 'Contrato de aprendizaje SENA', estado: 'Registrado', tono: 'neutro', fecha: '2026-10-06' },
    { consecutivo: 'DOC-2026-000012', titulo: 'Manual de seguridad de la información', estado: 'En revisión', tono: 'info', fecha: '2026-07-21' },
    { consecutivo: 'DOC-2026-000001', titulo: 'Certificado de existencia y representación legal', estado: 'Aprobado', tono: 'exito', fecha: '2026-01-15' },
  ]);
  protected readonly orden = signal<OrdenTabla | null>({ campo: 'consecutivo', direccion: 'desc' });
  protected readonly pagina = signal(0);
  protected readonly modalAbierto = signal(false);

  constructor() {
    this.invalido.markAsTouched();
  }

  protected ordenar(orden: OrdenTabla): void {
    this.orden.set(orden);
  }

  protected async confirmar(): Promise<void> {
    const confirmado = await this.confirmacion.pedir({
      titulo: 'Desactivar empleado',
      mensaje: 'Laura Peña dejará de aparecer en los listados y su usuario perderá el acceso.',
      accion: 'Desactivar',
      peligro: true,
    });
    if (confirmado) {
      this.toasts.exito('Empleado desactivado');
    }
  }
}
