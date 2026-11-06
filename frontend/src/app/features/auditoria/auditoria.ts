import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';

import { ApiService } from '../../core/services/api.service';
import { RegistroAuditoria } from '../../models/negocio';
import { Boton } from '../../shared/components/boton';
import { Campo, CampoInput } from '../../shared/components/campo';
import { Modal } from '../../shared/components/modal';
import { Paginador } from '../../shared/components/paginador';
import { ColumnaTabla, FilaTabla, Tabla } from '../../shared/components/tabla';
import { Vacio } from '../../shared/components/vacio';
import { FechaBogotaPipe } from '../../shared/pipes/fecha-bogota.pipe';
import { LISTA_ACCIONES_AUDITORIA, LISTA_ENTIDADES, textoAccion, textoEntidad } from '../../shared/utils/etiquetas';
import { Listado } from '../../shared/utils/listado';

interface CambioCampo {
  campo: string;
  anterior: string;
  nuevo: string;
}

const texto = (valor: unknown): string => (valor === null || valor === undefined ? '' : String(valor));

/** Convierte una fecha del selector en el inicio o fin de ese día en Bogotá. */
const limite = (fecha: string, fin: boolean): string =>
  fecha ? `${fecha}T${fin ? '23:59:59' : '00:00:00'}-05:00` : '';

@Component({
  selector: 'app-auditoria',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, Campo, CampoInput, FechaBogotaPipe, FilaTabla, Modal, Paginador, Tabla, Vacio],
  template: `
    <h1 class="text-2xl font-bold">Auditoría</h1>

    <div class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <ui-campo etiqueta="Tipo de registro">
        <select uiInput (change)="listado.filtrar({ entidad: $any($event.target).value })">
          <option value="">Todos</option>
          @for (e of entidades; track e) {
            <option [value]="e">{{ entidad(e) }}</option>
          }
        </select>
      </ui-campo>
      <ui-campo etiqueta="Acción">
        <select uiInput (change)="listado.filtrar({ accion: $any($event.target).value })">
          <option value="">Todas</option>
          @for (a of acciones; track a) {
            <option [value]="a">{{ accion(a) }}</option>
          }
        </select>
      </ui-campo>
      <ui-campo etiqueta="Desde">
        <input uiInput type="date" (change)="listado.filtrar({ desde: inicio($any($event.target).value) })" />
      </ui-campo>
      <ui-campo etiqueta="Hasta">
        <input uiInput type="date" (change)="listado.filtrar({ hasta: fin($any($event.target).value) })" />
      </ui-campo>
    </div>

    <div class="mt-4 rounded-panel border border-linea bg-superficie">
      <ui-tabla
        descripcion="Registro de auditoría"
        [columnas]="columnas"
        [filas]="listado.filas()"
        [cargando]="listado.cargando()"
        [orden]="listado.orden()"
        (ordenar)="listado.ordenar($event)"
      >
        <ng-template uiFila let-r>
          <td class="px-3 py-2.5 whitespace-nowrap">{{ r.createdAt | fechaBogota: true }}</td>
          <td class="px-3 py-2.5">{{ r.usuario?.nombre ?? 'Sistema' }}</td>
          <td class="px-3 py-2.5">{{ accion(r.accion) }}</td>
          <td class="px-3 py-2.5">{{ entidad(r.entidad) }} {{ r.entidadId }}</td>
          <td class="px-3 py-1.5 text-right">
            @if (r.valorAnterior || r.valorNuevo) {
              <button uiBoton type="button" variante="fantasma" tamano="pequeno" (click)="seleccionado.set(r)">Ver cambios</button>
            }
          </td>
        </ng-template>
        <ui-vacio titulo="No hay registros con estos filtros" icono="historial" />
      </ui-tabla>
      <ui-paginador
        [pagina]="listado.pagina()"
        [tamano]="listado.tamano"
        [total]="listado.total()"
        [totalPaginas]="listado.totalPaginas()"
        (cambio)="listado.irA($event)"
      />
    </div>

    <ui-modal titulo="Detalle del cambio" [abierto]="seleccionado() !== null" (cerrado)="seleccionado.set(null)">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-linea-fuerte text-left text-tenue">
              <th scope="col" class="py-1.5 pr-3 font-semibold">Campo</th>
              <th scope="col" class="py-1.5 pr-3 font-semibold">Antes</th>
              <th scope="col" class="py-1.5 font-semibold">Después</th>
            </tr>
          </thead>
          <tbody>
            @for (c of cambios(); track c.campo) {
              <tr class="border-b border-linea">
                <th scope="row" class="py-1.5 pr-3 text-left font-medium">{{ c.campo }}</th>
                <td class="py-1.5 pr-3 break-all text-tenue">{{ c.anterior }}</td>
                <td class="py-1.5 break-all">{{ c.nuevo }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </ui-modal>
  `,
})
export class Auditoria {
  private readonly api = inject(ApiService);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'createdAt', titulo: 'Fecha', ordenable: true },
    { clave: 'usuario', titulo: 'Quién' },
    { clave: 'accion', titulo: 'Acción', ordenable: true },
    { clave: 'entidad', titulo: 'Registro', ordenable: true },
    { clave: 'detalle', titulo: 'Detalle', derecha: true },
  ];
  protected readonly entidades = LISTA_ENTIDADES;
  protected readonly acciones = LISTA_ACCIONES_AUDITORIA;
  protected readonly accion = textoAccion;
  protected readonly entidad = textoEntidad;
  protected readonly listado = new Listado<RegistroAuditoria>(
    (parametros) => this.api.auditoria(parametros),
    inject(DestroyRef),
    { campo: 'createdAt', direccion: 'desc' },
  );
  protected readonly seleccionado = signal<RegistroAuditoria | null>(null);

  protected readonly cambios = computed<CambioCampo[]>(() => {
    const r = this.seleccionado();
    if (!r) {
      return [];
    }
    const campos = new Set([...Object.keys(r.valorAnterior ?? {}), ...Object.keys(r.valorNuevo ?? {})]);
    return [...campos]
      .map((campo) => ({ campo, anterior: texto(r.valorAnterior?.[campo]), nuevo: texto(r.valorNuevo?.[campo]) }))
      .filter((c) => c.anterior !== c.nuevo);
  });

  protected inicio(fecha: string): string {
    return limite(fecha, false);
  }

  protected fin(fecha: string): string {
    return limite(fecha, true);
  }
}
