import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../core/auth/auth.service';
import { ApiService, TipoCatalogo } from '../../core/services/api.service';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { ToastService } from '../../core/services/toast.service';
import { Catalogo } from '../../models/negocio';
import { Boton } from '../../shared/components/boton';
import { Busqueda } from '../../shared/components/busqueda';
import { Campo, CampoInput } from '../../shared/components/campo';
import { Estado } from '../../shared/components/estado';
import { Modal } from '../../shared/components/modal';
import { Paginador } from '../../shared/components/paginador';
import { ColumnaTabla, FilaTabla, Tabla } from '../../shared/components/tabla';
import { Vacio } from '../../shared/components/vacio';
import { TienePermiso } from '../../shared/directives/tiene-permiso';
import { aplicarErroresServidor, mensajeDeFallo } from '../../shared/utils/formularios';
import { Listado } from '../../shared/utils/listado';

const PESTANAS: { tipo: TipoCatalogo; texto: string; singular: string }[] = [
  { tipo: 'departamentos', texto: 'Departamentos', singular: 'departamento' },
  { tipo: 'cargos', texto: 'Cargos', singular: 'cargo' },
  { tipo: 'categorias-documento', texto: 'Categorías de documento', singular: 'categoría' },
];

@Component({
  selector: 'app-catalogos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Boton, Busqueda, Campo, CampoInput, Estado, FilaTabla, Modal, Paginador, Tabla, TienePermiso, Vacio],
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-bold">Catálogos</h1>
      <button *tienePermiso="'CATALOGOS_ESCRIBIR'" uiBoton type="button" (click)="abrir(null)">
        Agregar {{ actual().singular }}
      </button>
    </div>

    <div class="mt-4 flex gap-1 overflow-x-auto border-b border-linea" role="tablist" aria-label="Tipo de catálogo">
      @for (p of pestanas; track p.tipo) {
        <button
          type="button"
          role="tab"
          class="-mb-px border-b-2 px-3 py-2 text-sm font-medium whitespace-nowrap"
          [class.border-tinta]="p.tipo === actual().tipo"
          [class.text-tinta]="p.tipo === actual().tipo"
          [class.border-transparent]="p.tipo !== actual().tipo"
          [class.text-tenue]="p.tipo !== actual().tipo"
          [attr.aria-selected]="p.tipo === actual().tipo"
          (click)="cambiar(p)"
        >
          {{ p.texto }}
        </button>
      }
    </div>

    <div class="mt-4 max-w-sm"><ui-busqueda etiqueta="Buscar por nombre" (buscar)="listado.filtrar({ q: $event })" /></div>

    <div class="mt-4 rounded-panel border border-linea bg-superficie">
      <ui-tabla
        [descripcion]="actual().texto"
        [columnas]="columnas()"
        [filas]="listado.filas()"
        [cargando]="listado.cargando()"
        [orden]="listado.orden()"
        (ordenar)="listado.ordenar($event)"
      >
        <ng-template uiFila let-c>
          <td class="px-3 py-2.5 font-medium">{{ c.nombre }}</td>
          <td class="px-3 py-2.5 text-tenue">{{ c.descripcion }}</td>
          <td class="px-3 py-2.5">
            <ui-estado [tono]="c.activo ? 'exito' : 'neutro'">{{ c.activo ? 'Activo' : 'Inactivo' }}</ui-estado>
          </td>
          @if (puedeEscribir()) {
            <td class="px-3 py-1.5 text-right whitespace-nowrap">
              <button uiBoton type="button" variante="fantasma" tamano="pequeno" (click)="abrir(c)">Editar</button>
              <button uiBoton type="button" variante="fantasma" tamano="pequeno" (click)="alternar(c)">
                {{ c.activo ? 'Desactivar' : 'Activar' }}
              </button>
            </td>
          }
        </ng-template>
        <ui-vacio titulo="No hay registros" mensaje="Quita la búsqueda o agrega uno nuevo." />
      </ui-tabla>
      <ui-paginador
        [pagina]="listado.pagina()"
        [tamano]="listado.tamano"
        [total]="listado.total()"
        [totalPaginas]="listado.totalPaginas()"
        (cambio)="listado.irA($event)"
      />
    </div>

    <ui-modal
      [titulo]="(editando() ? 'Editar ' : 'Agregar ') + actual().singular"
      [abierto]="modalAbierto()"
      (cerrado)="modalAbierto.set(false)"
    >
      @if (error(); as mensaje) {
        <p role="alert" class="mb-3 rounded-control bg-peligro-suave px-3 py-2 text-sm text-peligro">{{ mensaje }}</p>
      }
      <form id="form-catalogo" class="space-y-4" [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
        <ui-campo etiqueta="Nombre"><input uiInput formControlName="nombre" /></ui-campo>
        <ui-campo etiqueta="Descripción" [obligatorio]="false"><input uiInput formControlName="descripcion" /></ui-campo>
      </form>
      <ng-container acciones>
        <button uiBoton type="button" variante="secundario" (click)="modalAbierto.set(false)">Cancelar</button>
        <button uiBoton type="submit" form="form-catalogo" [cargando]="guardando()">Guardar</button>
      </ng-container>
    </ui-modal>
  `,
})
export class Catalogos {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  private readonly confirmacion = inject(ConfirmacionService);

  protected readonly pestanas = PESTANAS;
  protected readonly actual = signal(PESTANAS[0]);
  protected readonly puedeEscribir = computed(() => this.auth.tienePermiso('CATALOGOS_ESCRIBIR'));
  protected readonly columnas = computed<ColumnaTabla[]>(() => [
    { clave: 'nombre', titulo: 'Nombre', ordenable: true },
    { clave: 'descripcion', titulo: 'Descripción' },
    { clave: 'activo', titulo: 'Estado', ordenable: true },
    ...(this.puedeEscribir() ? [{ clave: 'acciones', titulo: 'Acciones', derecha: true }] : []),
  ]);
  protected readonly listado = new Listado<Catalogo>(
    (parametros) => this.api.catalogos(this.actual().tipo, parametros),
    inject(DestroyRef),
    { campo: 'nombre', direccion: 'asc' },
  );

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    descripcion: ['', Validators.maxLength(255)],
  });
  protected readonly modalAbierto = signal(false);
  protected readonly editando = signal<Catalogo | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected cambiar(pestana: (typeof PESTANAS)[number]): void {
    this.actual.set(pestana);
    this.listado.filtrar({ q: '' });
  }

  protected abrir(catalogo: Catalogo | null): void {
    this.editando.set(catalogo);
    this.error.set(null);
    this.formulario.reset({ nombre: catalogo?.nombre ?? '', descripcion: catalogo?.descripcion ?? '' });
    this.modalAbierto.set(true);
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    const { nombre, descripcion } = this.formulario.getRawValue();
    const editando = this.editando();
    const tipo = this.actual().tipo;
    const datos = { nombre: nombre.trim(), descripcion: descripcion.trim() || null, version: editando?.version };

    (editando ? this.api.editarCatalogo(tipo, editando.id, datos) : this.api.crearCatalogo(tipo, datos)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.modalAbierto.set(false);
        this.toasts.exito('Cambios guardados');
        this.listado.recargar();
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        this.error.set(aplicarErroresServidor(this.formulario, fallo) ?? mensajeDeFallo(fallo));
      },
    });
  }

  protected async alternar(catalogo: Catalogo): Promise<void> {
    if (catalogo.activo) {
      const confirmado = await this.confirmacion.pedir({
        titulo: `Desactivar ${this.actual().singular}`,
        mensaje: `"${catalogo.nombre}" dejará de aparecer en los formularios.`,
        accion: 'Desactivar',
        peligro: true,
      });
      if (!confirmado) {
        return;
      }
    }
    this.api.cambiarCatalogo(this.actual().tipo, catalogo.id, !catalogo.activo).subscribe({
      next: () => {
        this.toasts.exito(catalogo.activo ? 'Desactivado' : 'Activado');
        this.listado.recargar();
      },
      error: (fallo: unknown) => this.toasts.error(mensajeDeFallo(fallo)),
    });
  }
}
