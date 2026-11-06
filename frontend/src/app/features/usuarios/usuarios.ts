import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../core/auth/auth.service';
import { ApiService } from '../../core/services/api.service';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { ToastService } from '../../core/services/toast.service';
import { Empleado, Rol, Usuario } from '../../models/negocio';
import { Boton } from '../../shared/components/boton';
import { Busqueda } from '../../shared/components/busqueda';
import { Campo, CampoInput } from '../../shared/components/campo';
import { Estado } from '../../shared/components/estado';
import { Modal } from '../../shared/components/modal';
import { Paginador } from '../../shared/components/paginador';
import { ColumnaTabla, FilaTabla, Tabla } from '../../shared/components/tabla';
import { Vacio } from '../../shared/components/vacio';
import { FechaBogotaPipe } from '../../shared/pipes/fecha-bogota.pipe';
import { PATRON_CORREO, aplicarErroresServidor, mensajeDeFallo, passwordValida } from '../../shared/utils/formularios';
import { Listado } from '../../shared/utils/listado';

@Component({
  selector: 'app-usuarios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, Boton, Busqueda, Campo, CampoInput, Estado, FechaBogotaPipe, FilaTabla, Modal, Paginador, Tabla, Vacio],
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-bold">Usuarios</h1>
      <button uiBoton type="button" (click)="abrir(null)">Crear usuario</button>
    </div>

    <div class="mt-4 max-w-sm"><ui-busqueda etiqueta="Buscar por nombre o correo" (buscar)="listado.filtrar({ q: $event })" /></div>

    <div class="mt-4 rounded-panel border border-linea bg-superficie">
      <ui-tabla
        descripcion="Usuarios"
        [columnas]="columnas"
        [filas]="listado.filas()"
        [cargando]="listado.cargando()"
        [orden]="listado.orden()"
        (ordenar)="listado.ordenar($event)"
      >
        <ng-template uiFila let-u>
          <td class="px-3 py-2.5 font-medium">{{ u.nombreVisible }}</td>
          <td class="px-3 py-2.5">{{ u.correo }}</td>
          <td class="px-3 py-2.5">{{ u.rol.nombre }}</td>
          <td class="px-3 py-2.5 whitespace-nowrap">
            <ui-estado [tono]="u.activo ? 'exito' : 'neutro'">{{ u.activo ? 'Activo' : 'Inactivo' }}</ui-estado>
            @if (u.bloqueadoHasta) {
              <ui-estado class="ml-1" tono="peligro">Bloqueado</ui-estado>
            }
          </td>
          <td class="px-3 py-2.5 whitespace-nowrap text-tenue">
            {{ u.ultimoAccesoAt ? (u.ultimoAccesoAt | fechaBogota: true) : 'Nunca' }}
          </td>
          <td class="px-3 py-1.5 text-right whitespace-nowrap">
            <button uiBoton type="button" variante="fantasma" tamano="pequeno" (click)="abrir(u)">Editar</button>
            <button uiBoton type="button" variante="fantasma" tamano="pequeno" (click)="abrirRestablecer(u)">Restablecer</button>
            @if (u.id !== idActual()) {
              <button uiBoton type="button" variante="fantasma" tamano="pequeno" (click)="alternar(u)">
                {{ u.activo ? 'Desactivar' : 'Activar' }}
              </button>
            }
          </td>
        </ng-template>
        <ui-vacio titulo="No hay usuarios con esta búsqueda" />
      </ui-tabla>
      <ui-paginador
        [pagina]="listado.pagina()"
        [tamano]="listado.tamano"
        [total]="listado.total()"
        [totalPaginas]="listado.totalPaginas()"
        (cambio)="listado.irA($event)"
      />
    </div>

    <ui-modal [titulo]="editando() ? 'Editar usuario' : 'Crear usuario'" [abierto]="modalAbierto()" (cerrado)="modalAbierto.set(false)">
      @if (error(); as mensaje) {
        <p role="alert" class="mb-3 rounded-control bg-peligro-suave px-3 py-2 text-sm text-peligro">{{ mensaje }}</p>
      }
      <form id="form-usuario" class="space-y-4" [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
        @if (!editando()) {
          <ui-campo etiqueta="Correo"><input uiInput type="email" formControlName="correo" autocomplete="off" /></ui-campo>
        }
        <ui-campo etiqueta="Nombre"><input uiInput formControlName="nombreVisible" autocomplete="off" /></ui-campo>
        <ui-campo etiqueta="Rol">
          <select uiInput formControlName="rolId">
            <option [ngValue]="null" disabled>Selecciona uno</option>
            @for (r of roles(); track r.id) {
              <option [ngValue]="r.id">{{ r.nombre }}</option>
            }
          </select>
        </ui-campo>
        <ui-campo etiqueta="Empleado vinculado" [obligatorio]="false">
          <select uiInput formControlName="empleadoId">
            <option [ngValue]="null">Sin vincular</option>
            @for (e of empleados(); track e.id) {
              <option [ngValue]="e.id">{{ e.nombres }} {{ e.apellidos }}</option>
            }
          </select>
        </ui-campo>
        @if (!editando()) {
          <ui-campo etiqueta="Contraseña temporal" ayuda="Mínimo 10 caracteres. El usuario deberá cambiarla al entrar.">
            <input uiInput type="password" formControlName="passwordTemporal" autocomplete="new-password" />
          </ui-campo>
        }
      </form>
      <ng-container acciones>
        <button uiBoton type="button" variante="secundario" (click)="modalAbierto.set(false)">Cancelar</button>
        <button uiBoton type="submit" form="form-usuario" [cargando]="guardando()">Guardar</button>
      </ng-container>
    </ui-modal>

    <ui-modal titulo="Restablecer contraseña" [abierto]="restableciendo() !== null" (cerrado)="restableciendo.set(null)">
      <p class="mb-3 text-sm text-tenue">
        Se cerrarán las sesiones de {{ restableciendo()?.nombreVisible }}, se desbloqueará la cuenta y deberá cambiar la contraseña al entrar.
      </p>
      <ui-campo etiqueta="Contraseña temporal" ayuda="Mínimo 10 caracteres.">
        <input uiInput type="password" [formControl]="temporal" autocomplete="new-password" />
      </ui-campo>
      <ng-container acciones>
        <button uiBoton type="button" variante="secundario" (click)="restableciendo.set(null)">Cancelar</button>
        <button uiBoton type="button" (click)="restablecer()">Restablecer</button>
      </ng-container>
    </ui-modal>
  `,
})
export class Usuarios {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  private readonly confirmacion = inject(ConfirmacionService);

  protected readonly columnas: ColumnaTabla[] = [
    { clave: 'nombreVisible', titulo: 'Nombre', ordenable: true },
    { clave: 'correo', titulo: 'Correo', ordenable: true },
    { clave: 'rol', titulo: 'Rol' },
    { clave: 'activo', titulo: 'Estado', ordenable: true },
    { clave: 'ultimoAccesoAt', titulo: 'Último acceso', ordenable: true },
    { clave: 'acciones', titulo: 'Acciones', derecha: true },
  ];
  protected readonly listado = new Listado<Usuario>(
    (parametros) => this.api.usuarios(parametros),
    inject(DestroyRef),
    { campo: 'nombreVisible', direccion: 'asc' },
  );
  protected readonly idActual = computed(() => this.auth.usuario()?.id);
  protected readonly roles = signal<Rol[]>([]);
  protected readonly empleados = signal<Empleado[]>([]);

  protected readonly formulario = inject(FormBuilder).group({
    correo: ['', [Validators.required, Validators.maxLength(150), Validators.pattern(PATRON_CORREO)]],
    nombreVisible: ['', [Validators.required, Validators.maxLength(120)]],
    rolId: [null as number | null, Validators.required],
    empleadoId: [null as number | null],
    passwordTemporal: ['', [Validators.required, passwordValida]],
  });
  protected readonly temporal = new FormControl('', { nonNullable: true, validators: [Validators.required, passwordValida] });

  protected readonly modalAbierto = signal(false);
  protected readonly editando = signal<Usuario | null>(null);
  protected readonly restableciendo = signal<Usuario | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.api.roles().subscribe((lista) => this.roles.set(lista));
    this.api.empleadosActivos().subscribe((lista) => this.empleados.set(lista));
  }

  protected abrir(usuario: Usuario | null): void {
    this.editando.set(usuario);
    this.error.set(null);
    this.formulario.reset({
      correo: usuario?.correo ?? '',
      nombreVisible: usuario?.nombreVisible ?? '',
      rolId: usuario?.rol.id ?? null,
      empleadoId: usuario?.empleado?.id ?? null,
      passwordTemporal: '',
    });
    const { correo, passwordTemporal } = this.formulario.controls;
    if (usuario) {
      correo.disable();
      passwordTemporal.disable();
    } else {
      correo.enable();
      passwordTemporal.enable();
    }
    this.modalAbierto.set(true);
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    const v = this.formulario.getRawValue();
    const editando = this.editando();
    const peticion = editando
      ? this.api.editarUsuario(editando.id, {
          nombreVisible: v.nombreVisible,
          rolId: v.rolId,
          empleadoId: v.empleadoId,
          version: editando.version,
        })
      : this.api.crearUsuario(v);

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.modalAbierto.set(false);
        this.toasts.exito(editando ? 'Cambios guardados' : 'Usuario creado');
        this.listado.recargar();
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        this.error.set(aplicarErroresServidor(this.formulario, fallo) ?? mensajeDeFallo(fallo));
      },
    });
  }

  protected abrirRestablecer(usuario: Usuario): void {
    this.temporal.reset();
    this.restableciendo.set(usuario);
  }

  protected restablecer(): void {
    const usuario = this.restableciendo();
    if (!usuario || this.temporal.invalid) {
      this.temporal.markAsTouched();
      return;
    }
    this.api.restablecerPassword(usuario.id, this.temporal.value).subscribe({
      next: () => {
        this.restableciendo.set(null);
        this.toasts.exito('Contraseña restablecida');
        this.listado.recargar();
      },
      error: (fallo: unknown) => this.toasts.error(mensajeDeFallo(fallo)),
    });
  }

  protected async alternar(usuario: Usuario): Promise<void> {
    if (usuario.activo) {
      const confirmado = await this.confirmacion.pedir({
        titulo: 'Desactivar usuario',
        mensaje: `${usuario.nombreVisible} perderá el acceso y se cerrarán sus sesiones.`,
        accion: 'Desactivar',
        peligro: true,
      });
      if (!confirmado) {
        return;
      }
    }
    this.api.cambiarUsuario(usuario.id, !usuario.activo).subscribe({
      next: () => {
        this.toasts.exito(usuario.activo ? 'Usuario desactivado' : 'Usuario activado');
        this.listado.recargar();
      },
      error: (fallo: unknown) => this.toasts.error(mensajeDeFallo(fallo)),
    });
  }
}
