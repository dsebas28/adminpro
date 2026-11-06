import { ChangeDetectionStrategy, Component, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { Catalogo, Empleado, EmpleadoRequest } from '../../models/negocio';
import { Boton } from '../../shared/components/boton';
import { Campo, CampoInput } from '../../shared/components/campo';
import { PATRON_CORREO, aplicarErroresServidor } from '../../shared/utils/formularios';

/** Sirve para crear y para editar: con id en la ruta, carga el empleado. */
@Component({
  selector: 'app-empleado-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, Boton, Campo, CampoInput],
  template: `
    <h1 class="text-2xl font-bold">{{ id() ? 'Editar empleado' : 'Nuevo empleado' }}</h1>

    @if (error(); as mensaje) {
      <p role="alert" class="mt-4 max-w-2xl rounded-control bg-peligro-suave px-3 py-2.5 text-sm text-peligro">{{ mensaje }}</p>
    }

    <form class="mt-5 grid max-w-2xl gap-4 sm:grid-cols-2" [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <ui-campo etiqueta="Nombres"><input uiInput formControlName="nombres" autocomplete="off" /></ui-campo>
      <ui-campo etiqueta="Apellidos"><input uiInput formControlName="apellidos" autocomplete="off" /></ui-campo>
      <ui-campo etiqueta="Cédula" ayuda="Entre 6 y 10 dígitos.">
        <input uiInput formControlName="cedula" inputmode="numeric" autocomplete="off" />
      </ui-campo>
      <ui-campo etiqueta="Correo"><input uiInput type="email" formControlName="correo" autocomplete="off" /></ui-campo>
      <ui-campo etiqueta="Teléfono" [obligatorio]="false" ayuda="10 dígitos.">
        <input uiInput formControlName="telefono" inputmode="tel" autocomplete="off" />
      </ui-campo>
      <ui-campo etiqueta="Fecha de ingreso"><input uiInput type="date" formControlName="fechaIngreso" /></ui-campo>
      <ui-campo etiqueta="Departamento">
        <select uiInput formControlName="departamentoId">
          <option [ngValue]="null" disabled>Selecciona uno</option>
          @for (d of departamentos(); track d.id) {
            <option [ngValue]="d.id">{{ d.nombre }}</option>
          }
        </select>
      </ui-campo>
      <ui-campo etiqueta="Cargo">
        <select uiInput formControlName="cargoId">
          <option [ngValue]="null" disabled>Selecciona uno</option>
          @for (c of cargos(); track c.id) {
            <option [ngValue]="c.id">{{ c.nombre }}</option>
          }
        </select>
      </ui-campo>

      <div class="flex gap-2 sm:col-span-2">
        <button uiBoton type="submit" [cargando]="guardando()">{{ id() ? 'Guardar cambios' : 'Crear empleado' }}</button>
        <a uiBoton variante="secundario" [routerLink]="id() ? ['/empleados', id()] : ['/empleados']">Cancelar</a>
      </div>
    </form>
  `,
})
export class EmpleadoForm implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);

  /** Viene de la ruta gracias a withComponentInputBinding. */
  readonly id = input(undefined, { transform: (v: unknown) => (v === undefined ? undefined : numberAttribute(v)) });

  protected readonly formulario = inject(FormBuilder).group({
    nombres: ['', [Validators.required, Validators.maxLength(100)]],
    apellidos: ['', [Validators.required, Validators.maxLength(100)]],
    cedula: ['', [Validators.required, Validators.pattern(/^[0-9]{6,10}$/)]],
    correo: ['', [Validators.required, Validators.maxLength(150), Validators.pattern(PATRON_CORREO)]],
    telefono: ['', Validators.pattern(/^[0-9]{10}$/)],
    fechaIngreso: ['', Validators.required],
    departamentoId: [null as number | null, Validators.required],
    cargoId: [null as number | null, Validators.required],
  });
  protected readonly departamentos = signal<Catalogo[]>([]);
  protected readonly cargos = signal<Catalogo[]>([]);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  private version: number | undefined;

  ngOnInit(): void {
    this.api.opciones('departamentos').subscribe((lista) => this.departamentos.update((a) => unir(a, lista)));
    this.api.opciones('cargos').subscribe((lista) => this.cargos.update((a) => unir(a, lista)));

    const id = this.id();
    if (id) {
      this.api.empleado(id).subscribe((e) => this.cargar(e));
    }
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    const valor = this.formulario.getRawValue();
    const datos: EmpleadoRequest = {
      nombres: valor.nombres!.trim(),
      apellidos: valor.apellidos!.trim(),
      cedula: valor.cedula!,
      correo: valor.correo!.trim(),
      telefono: valor.telefono || null,
      fechaIngreso: valor.fechaIngreso!,
      departamentoId: valor.departamentoId!,
      cargoId: valor.cargoId!,
      version: this.version,
    };
    const id = this.id();
    const peticion: Observable<Empleado> = id ? this.api.editarEmpleado(id, datos) : this.api.crearEmpleado(datos);

    peticion.subscribe({
      next: (empleado) => {
        this.toasts.exito(id ? 'Cambios guardados' : 'Empleado creado');
        void this.router.navigate(['/empleados', empleado.id]);
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        this.error.set(aplicarErroresServidor(this.formulario, fallo));
      },
    });
  }

  private cargar(e: Empleado): void {
    this.version = e.version;
    // Un catálogo ya desactivado no llega en las opciones, pero el empleado puede conservarlo.
    this.departamentos.update((a) => unir(a, [{ ...e.departamento, descripcion: null, activo: true, version: 0 }]));
    this.cargos.update((a) => unir(a, [{ ...e.cargo, descripcion: null, activo: true, version: 0 }]));
    this.formulario.patchValue({
      nombres: e.nombres,
      apellidos: e.apellidos,
      cedula: e.cedula,
      correo: e.correo,
      telefono: e.telefono ?? '',
      fechaIngreso: e.fechaIngreso,
      departamentoId: e.departamento.id,
      cargoId: e.cargo.id,
    });
  }
}

function unir(actual: Catalogo[], nuevos: Catalogo[]): Catalogo[] {
  const porId = new Map([...actual, ...nuevos].map((c) => [c.id, c]));
  return [...porId.values()].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
}
