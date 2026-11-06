import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/services/api.service';
import { Catalogo, Empleado } from '../../models/negocio';
import { Boton } from '../../shared/components/boton';
import { Busqueda } from '../../shared/components/busqueda';
import { Campo, CampoInput } from '../../shared/components/campo';
import { Estado } from '../../shared/components/estado';
import { Paginador } from '../../shared/components/paginador';
import { ColumnaTabla, FilaTabla, Tabla } from '../../shared/components/tabla';
import { Vacio } from '../../shared/components/vacio';
import { TienePermiso } from '../../shared/directives/tiene-permiso';
import { Listado } from '../../shared/utils/listado';

@Component({
  selector: 'app-empleados-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Boton, Busqueda, Campo, CampoInput, Estado, FilaTabla, Paginador, Tabla, TienePermiso, Vacio],
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-bold">Empleados</h1>
      <a *tienePermiso="'EMPLEADOS_ESCRIBIR'" uiBoton routerLink="/empleados/nuevo">Nuevo empleado</a>
    </div>

    <div class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <div class="self-end"><ui-busqueda etiqueta="Buscar por nombre, cédula o correo" (buscar)="listado.filtrar({ q: $event })" /></div>
      <ui-campo etiqueta="Departamento">
        <select uiInput (change)="listado.filtrar({ departamentoId: $any($event.target).value })">
          <option value="">Todos</option>
          @for (d of departamentos(); track d.id) {
            <option [value]="d.id">{{ d.nombre }}</option>
          }
        </select>
      </ui-campo>
      <ui-campo etiqueta="Cargo">
        <select uiInput (change)="listado.filtrar({ cargoId: $any($event.target).value })">
          <option value="">Todos</option>
          @for (c of cargos(); track c.id) {
            <option [value]="c.id">{{ c.nombre }}</option>
          }
        </select>
      </ui-campo>
      <ui-campo etiqueta="Estado">
        <select uiInput (change)="listado.filtrar({ activo: $any($event.target).value })">
          <option value="true">Activos</option>
          <option value="false">Inactivos</option>
          <option value="">Todos</option>
        </select>
      </ui-campo>
    </div>

    <div class="mt-4 rounded-panel border border-linea bg-superficie">
      <ui-tabla
        descripcion="Empleados"
        [columnas]="columnas"
        [filas]="listado.filas()"
        [cargando]="listado.cargando()"
        [orden]="listado.orden()"
        (ordenar)="listado.ordenar($event)"
      >
        <ng-template uiFila let-e>
          <td class="px-3 py-2.5">
            <a [routerLink]="['/empleados', e.id]" class="font-medium text-tinta hover:underline">
              {{ e.apellidos }}, {{ e.nombres }}
            </a>
          </td>
          <td class="px-3 py-2.5 whitespace-nowrap">{{ e.cedula }}</td>
          <td class="px-3 py-2.5">{{ e.departamento.nombre }}</td>
          <td class="px-3 py-2.5">{{ e.cargo.nombre }}</td>
          <td class="px-3 py-2.5">
            <ui-estado [tono]="e.activo ? 'exito' : 'neutro'">{{ e.activo ? 'Activo' : 'Inactivo' }}</ui-estado>
          </td>
        </ng-template>
        <ui-vacio
          [icono]="listado.fallo() ? 'alerta' : 'personas'"
          [titulo]="listado.fallo() ? 'No se pudo cargar el listado' : 'No hay empleados con estos filtros'"
          [mensaje]="listado.fallo() ? 'Recarga la página para intentarlo de nuevo.' : 'Quita un filtro o registra un empleado.'"
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
export class EmpleadosLista {
  private readonly api = inject(ApiService);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'apellidos', titulo: 'Nombre', ordenable: true },
    { clave: 'cedula', titulo: 'Cédula', ordenable: true },
    { clave: 'departamento', titulo: 'Departamento' },
    { clave: 'cargo', titulo: 'Cargo' },
    { clave: 'activo', titulo: 'Estado', ordenable: true },
  ];
  protected readonly departamentos = signal<Catalogo[]>([]);
  protected readonly cargos = signal<Catalogo[]>([]);
  protected readonly listado = new Listado<Empleado>(
    (parametros) => this.api.empleados(parametros),
    inject(DestroyRef),
    { campo: 'apellidos', direccion: 'asc' },
    { activo: true },
  );

  constructor() {
    this.api.opciones('departamentos').subscribe((lista) => this.departamentos.set(lista));
    this.api.opciones('cargos').subscribe((lista) => this.cargos.set(lista));
  }
}
