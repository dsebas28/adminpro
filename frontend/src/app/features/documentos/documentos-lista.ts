import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/services/api.service';
import { Catalogo, DocumentoItem, EstadoDocumento } from '../../models/negocio';
import { Boton } from '../../shared/components/boton';
import { Busqueda } from '../../shared/components/busqueda';
import { Campo, CampoInput } from '../../shared/components/campo';
import { Estado } from '../../shared/components/estado';
import { Paginador } from '../../shared/components/paginador';
import { ColumnaTabla, FilaTabla, Tabla } from '../../shared/components/tabla';
import { Vacio } from '../../shared/components/vacio';
import { TienePermiso } from '../../shared/directives/tiene-permiso';
import { FechaBogotaPipe } from '../../shared/pipes/fecha-bogota.pipe';
import { ESTADOS, LISTA_ESTADOS } from '../../shared/utils/etiquetas';
import { Listado } from '../../shared/utils/listado';

@Component({
  selector: 'app-documentos-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Boton, Busqueda, Campo, CampoInput, Estado, FechaBogotaPipe, FilaTabla, Paginador, Tabla, TienePermiso, Vacio],
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-bold">Documentos</h1>
      <a *tienePermiso="'DOCUMENTOS_ESCRIBIR'" uiBoton routerLink="/documentos/nuevo">Registrar documento</a>
    </div>

    <div class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
      <div class="self-end sm:col-span-2 lg:col-span-1">
        <ui-busqueda etiqueta="Número o título" (buscar)="listado.filtrar({ q: $event })" />
      </div>
      <ui-campo etiqueta="Categoría">
        <select uiInput (change)="listado.filtrar({ categoriaId: $any($event.target).value })">
          <option value="">Todas</option>
          @for (c of categorias(); track c.id) {
            <option [value]="c.id">{{ c.nombre }}</option>
          }
        </select>
      </ui-campo>
      <ui-campo etiqueta="Estado">
        <select uiInput (change)="listado.filtrar({ estado: $any($event.target).value })">
          <option value="">Todos</option>
          @for (e of listaEstados; track e) {
            <option [value]="e">{{ estados[e].texto }}</option>
          }
        </select>
      </ui-campo>
      <ui-campo etiqueta="Desde">
        <input uiInput type="date" (change)="listado.filtrar({ fechaDesde: $any($event.target).value })" />
      </ui-campo>
      <ui-campo etiqueta="Hasta">
        <input uiInput type="date" (change)="listado.filtrar({ fechaHasta: $any($event.target).value })" />
      </ui-campo>
    </div>

    <div class="mt-4 rounded-panel border border-linea bg-superficie">
      <ui-tabla
        descripcion="Documentos"
        [columnas]="columnas"
        [filas]="listado.filas()"
        [cargando]="listado.cargando()"
        [orden]="listado.orden()"
        (ordenar)="listado.ordenar($event)"
      >
        <ng-template uiFila let-d>
          <td class="px-3 py-2.5 whitespace-nowrap">
            <a [routerLink]="['/documentos', d.id]" class="font-numerador text-[0.8125rem] text-tinta hover:underline">{{ d.consecutivo }}</a>
          </td>
          <td class="px-3 py-2.5">{{ d.titulo }}</td>
          <td class="px-3 py-2.5">{{ d.categoria.nombre }}</td>
          <td class="px-3 py-2.5">{{ d.responsable.nombre }}</td>
          <td class="px-3 py-2.5 whitespace-nowrap">
            <ui-estado [tono]="info(d.estado).tono">{{ info(d.estado).texto }}</ui-estado>
            @if (!d.activo) {
              <ui-estado class="ml-1" tono="neutro">Desactivado</ui-estado>
            }
          </td>
          <td class="px-3 py-2.5 text-right whitespace-nowrap">
            @if (d.vencido) {
              <ui-estado tono="peligro">Vencido</ui-estado>
            }
            {{ d.fechaVencimiento | fechaBogota }}
          </td>
        </ng-template>
        <ui-vacio
          [icono]="listado.fallo() ? 'alerta' : 'documento'"
          [titulo]="listado.fallo() ? 'No se pudo cargar el listado' : 'No hay documentos con estos filtros'"
          [mensaje]="listado.fallo() ? 'Recarga la página para intentarlo de nuevo.' : 'Quita un filtro o registra un documento.'"
        />
      </ui-tabla>
      <ui-paginador
        [pagina]="listado.pagina()"
        [tamano]="listado.tamano"
        [total]="listado.total()"
        [totalPaginas]="listado.totalPaginas()"
        (cambio)="listado.irA($event)"
      />
    </div>
  `,
})
export class DocumentosLista {
  private readonly api = inject(ApiService);

  protected readonly estados = ESTADOS;
  protected readonly info = (estado: EstadoDocumento) => ESTADOS[estado];
  protected readonly listaEstados = LISTA_ESTADOS;
  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'consecutivo', titulo: 'Número', ordenable: true },
    { clave: 'titulo', titulo: 'Título', ordenable: true },
    { clave: 'categoria', titulo: 'Categoría' },
    { clave: 'responsable', titulo: 'Responsable' },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
    { clave: 'fechaVencimiento', titulo: 'Vence', ordenable: true, derecha: true },
  ];
  protected readonly categorias = signal<Catalogo[]>([]);
  protected readonly listado = new Listado<DocumentoItem>(
    (parametros) => this.api.documentos(parametros),
    inject(DestroyRef),
    { campo: 'consecutivo', direccion: 'desc' },
    { activo: true },
  );

  constructor() {
    this.api.opciones('categorias-documento').subscribe((lista) => this.categorias.set(lista));
  }
}
