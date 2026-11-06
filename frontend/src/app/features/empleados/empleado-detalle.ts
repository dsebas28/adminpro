import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/services/api.service';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { ToastService } from '../../core/services/toast.service';
import { Empleado, RegistroAuditoria } from '../../models/negocio';
import { Boton } from '../../shared/components/boton';
import { Esqueleto } from '../../shared/components/esqueleto';
import { Estado } from '../../shared/components/estado';
import { EventoTiempo, LineaTiempo } from '../../shared/components/linea-tiempo';
import { Vacio } from '../../shared/components/vacio';
import { TienePermiso } from '../../shared/directives/tiene-permiso';
import { FechaBogotaPipe } from '../../shared/pipes/fecha-bogota.pipe';
import { textoAccion } from '../../shared/utils/etiquetas';
import { mensajeDeFallo } from '../../shared/utils/formularios';

const CAMPOS: Record<string, string> = {
  cedula: 'cédula',
  nombres: 'nombres',
  apellidos: 'apellidos',
  correo: 'correo',
  telefono: 'teléfono',
  fechaIngreso: 'fecha de ingreso',
  departamento: 'departamento',
  cargo: 'cargo',
};

function cambios(registro: RegistroAuditoria): string | null {
  if (!registro.valorAnterior || !registro.valorNuevo) {
    return null;
  }
  const lineas = Object.keys(CAMPOS)
    .filter((campo) => registro.valorAnterior![campo] !== registro.valorNuevo![campo])
    .map((campo) => `${CAMPOS[campo]}: ${registro.valorAnterior![campo] ?? 'vacío'} por ${registro.valorNuevo![campo] ?? 'vacío'}`);
  return lineas.join('\n') || null;
}

@Component({
  selector: 'app-empleado-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Boton, Esqueleto, Estado, FechaBogotaPipe, LineaTiempo, TienePermiso, Vacio],
  template: `
    @if (empleado(); as e) {
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 class="text-2xl font-bold">{{ e.nombres }} {{ e.apellidos }}</h1>
          <p class="mt-1 flex items-center gap-2 text-tenue">
            {{ e.cargo.nombre }}
            <ui-estado [tono]="e.activo ? 'exito' : 'neutro'">{{ e.activo ? 'Activo' : 'Inactivo' }}</ui-estado>
          </p>
        </div>
        <div class="flex gap-2">
          <a *tienePermiso="'EMPLEADOS_ESCRIBIR'" uiBoton variante="secundario" [routerLink]="['/empleados', e.id, 'editar']">Editar</a>
          <ng-container *tienePermiso="'EMPLEADOS_DESACTIVAR'">
            @if (e.activo) {
              <button uiBoton type="button" variante="peligro" (click)="desactivar(e)">Desactivar</button>
            } @else {
              <button uiBoton type="button" variante="secundario" (click)="activar(e)">Activar</button>
            }
          </ng-container>
        </div>
      </div>

      <div class="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[1fr_24rem]">
        <section class="rounded-panel border border-linea bg-superficie p-5">
          <h2 class="font-semibold">Datos</h2>
          <dl class="mt-3 grid gap-x-6 gap-y-3 text-sm sm:grid-cols-2">
            <div><dt class="text-tenue">Cédula</dt><dd class="font-medium">{{ e.cedula }}</dd></div>
            <div><dt class="text-tenue">Correo</dt><dd class="font-medium break-all">{{ e.correo }}</dd></div>
            <div><dt class="text-tenue">Teléfono</dt><dd class="font-medium">{{ e.telefono ?? 'Sin registrar' }}</dd></div>
            <div><dt class="text-tenue">Fecha de ingreso</dt><dd class="font-medium">{{ e.fechaIngreso | fechaBogota }}</dd></div>
            <div><dt class="text-tenue">Departamento</dt><dd class="font-medium">{{ e.departamento.nombre }}</dd></div>
            <div><dt class="text-tenue">Cargo</dt><dd class="font-medium">{{ e.cargo.nombre }}</dd></div>
          </dl>
        </section>
        <section class="rounded-panel border border-linea bg-superficie p-5">
          <h2 class="font-semibold">Historial de cambios</h2>
          <div class="mt-3">
            @if (eventos().length) {
              <ui-linea-tiempo [eventos]="eventos()" />
            } @else {
              <p class="text-sm text-tenue">Este empleado no tiene cambios registrados.</p>
            }
          </div>
        </section>
      </div>
    } @else if (noExiste()) {
      <ui-vacio titulo="Este empleado no existe" mensaje="Puede que el enlace esté mal escrito.">
        <a uiBoton routerLink="/empleados" variante="secundario">Volver a empleados</a>
      </ui-vacio>
    } @else {
      <ui-esqueleto alto="2rem" ancho="40%" />
      <ui-esqueleto class="mt-6" alto="14rem" />
    }
  `,
})
export class EmpleadoDetalle implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toasts = inject(ToastService);
  private readonly confirmacion = inject(ConfirmacionService);

  readonly id = input.required({ transform: numberAttribute });

  protected readonly empleado = signal<Empleado | null>(null);
  protected readonly noExiste = signal(false);
  private readonly historial = signal<RegistroAuditoria[]>([]);
  protected readonly eventos = computed<EventoTiempo[]>(() =>
    this.historial().map((r) => ({
      id: r.id,
      titulo: `${textoAccion(r.accion)} el empleado`,
      detalle: cambios(r),
      autor: r.usuario?.nombre,
      fecha: r.createdAt,
    })),
  );

  ngOnInit(): void {
    this.api.empleado(this.id()).subscribe({
      next: (e) => this.empleado.set(e),
      error: () => this.noExiste.set(true),
    });
    this.cargarHistorial();
  }

  protected async desactivar(e: Empleado): Promise<void> {
    const confirmado = await this.confirmacion.pedir({
      titulo: 'Desactivar empleado',
      mensaje: `${e.nombres} ${e.apellidos} dejará de aparecer en los listados. Si tiene usuario, perderá el acceso.`,
      accion: 'Desactivar',
      peligro: true,
    });
    if (confirmado) {
      this.cambiar(e, false, 'Empleado desactivado');
    }
  }

  protected activar(e: Empleado): void {
    this.cambiar(e, true, 'Empleado activado');
  }

  private cambiar(e: Empleado, activar: boolean, mensaje: string): void {
    this.api.cambiarEmpleado(e.id, activar).subscribe({
      next: (actualizado) => {
        this.empleado.set(actualizado);
        this.cargarHistorial();
        this.toasts.exito(mensaje);
      },
      error: (fallo: unknown) => this.toasts.error(mensajeDeFallo(fallo)),
    });
  }

  private cargarHistorial(): void {
    this.api.historialEmpleado(this.id()).subscribe({
      next: (pagina) => this.historial.set(pagina.content),
      error: () => this.historial.set([]),
    });
  }
}
