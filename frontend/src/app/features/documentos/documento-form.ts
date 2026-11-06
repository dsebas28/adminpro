import { ChangeDetectionStrategy, Component, OnInit, inject, input, numberAttribute, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { Documento, DocumentoRequest } from '../../models/negocio';
import { Boton } from '../../shared/components/boton';
import { Campo, CampoInput } from '../../shared/components/campo';
import { aplicarErroresServidor } from '../../shared/utils/formularios';

interface Opcion {
  id: number;
  nombre: string;
}

function noAntesDeDocumento(control: AbstractControl): ValidationErrors | null {
  const fecha = control.parent?.get('fechaDocumento')?.value as string | undefined;
  return control.value && fecha && control.value < fecha
    ? { servidor: 'No puede ser anterior a la fecha del documento' }
    : null;
}

@Component({
  selector: 'app-documento-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, Boton, Campo, CampoInput],
  template: `
    <h1 class="text-2xl font-bold">{{ id() ? 'Editar documento' : 'Registrar documento' }}</h1>
    @if (!id()) {
      <p class="mt-1 text-tenue">El número se asigna al guardar.</p>
    }

    @if (error(); as mensaje) {
      <p role="alert" class="mt-4 max-w-2xl rounded-control bg-peligro-suave px-3 py-2.5 text-sm text-peligro">{{ mensaje }}</p>
    }

    <form class="mt-5 grid max-w-2xl gap-4 sm:grid-cols-2" [formGroup]="formulario" (ngSubmit)="guardar()" novalidate>
      <div class="sm:col-span-2">
        <ui-campo etiqueta="Título"><input uiInput formControlName="titulo" autocomplete="off" /></ui-campo>
      </div>
      <ui-campo etiqueta="Categoría">
        <select uiInput formControlName="categoriaId">
          <option [ngValue]="null" disabled>Selecciona una</option>
          @for (c of categorias(); track c.id) {
            <option [ngValue]="c.id">{{ c.nombre }}</option>
          }
        </select>
      </ui-campo>
      <ui-campo etiqueta="Responsable">
        <select uiInput formControlName="responsableId">
          <option [ngValue]="null" disabled>Selecciona uno</option>
          @for (r of responsables(); track r.id) {
            <option [ngValue]="r.id">{{ r.nombre }}</option>
          }
        </select>
      </ui-campo>
      <ui-campo etiqueta="Fecha del documento"><input uiInput type="date" formControlName="fechaDocumento" /></ui-campo>
      <ui-campo etiqueta="Fecha de vencimiento" [obligatorio]="false">
        <input uiInput type="date" formControlName="fechaVencimiento" />
      </ui-campo>
      <div class="sm:col-span-2">
        <ui-campo etiqueta="Descripción" [obligatorio]="false">
          <textarea uiInput class="h-24 py-2" formControlName="descripcion"></textarea>
        </ui-campo>
      </div>
      <div class="flex gap-2 sm:col-span-2">
        <button uiBoton type="submit" [cargando]="guardando()">{{ id() ? 'Guardar cambios' : 'Registrar documento' }}</button>
        <a uiBoton variante="secundario" [routerLink]="id() ? ['/documentos', id()] : ['/documentos']">Cancelar</a>
      </div>
    </form>
  `,
})
export class DocumentoForm implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);

  readonly id = input(undefined, { transform: (v: unknown) => (v === undefined ? undefined : numberAttribute(v)) });

  protected readonly formulario = inject(FormBuilder).group({
    titulo: ['', [Validators.required, Validators.maxLength(200)]],
    categoriaId: [null as number | null, Validators.required],
    responsableId: [null as number | null, Validators.required],
    fechaDocumento: ['', Validators.required],
    fechaVencimiento: ['', noAntesDeDocumento],
    descripcion: ['', Validators.maxLength(2000)],
  });
  protected readonly categorias = signal<Opcion[]>([]);
  protected readonly responsables = signal<Opcion[]>([]);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  private version: number | undefined;

  ngOnInit(): void {
    this.formulario.controls.fechaDocumento.valueChanges.subscribe(() =>
      this.formulario.controls.fechaVencimiento.updateValueAndValidity(),
    );
    this.api.opciones('categorias-documento').subscribe((lista) => this.categorias.update((a) => unir(a, lista)));
    this.api.empleadosActivos().subscribe((lista) =>
      this.responsables.update((a) => unir(a, lista.map((e) => ({ id: e.id, nombre: `${e.nombres} ${e.apellidos}` })))),
    );
    const id = this.id();
    if (id) {
      this.api.documento(id).subscribe((d) => this.cargar(d));
    }
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    const v = this.formulario.getRawValue();
    const datos: DocumentoRequest = {
      titulo: v.titulo!.trim(),
      descripcion: v.descripcion?.trim() || null,
      categoriaId: v.categoriaId!,
      responsableId: v.responsableId!,
      fechaDocumento: v.fechaDocumento!,
      fechaVencimiento: v.fechaVencimiento || null,
      version: this.version,
    };
    const id = this.id();
    (id ? this.api.editarDocumento(id, datos) : this.api.crearDocumento(datos)).subscribe({
      next: (documento) => {
        this.toasts.exito(id ? 'Cambios guardados' : `Documento ${documento.consecutivo} registrado`);
        void this.router.navigate(['/documentos', documento.id]);
      },
      error: (fallo: unknown) => {
        this.guardando.set(false);
        this.error.set(aplicarErroresServidor(this.formulario, fallo));
      },
    });
  }

  private cargar(d: Documento): void {
    this.version = d.version;
    // La categoría o el responsable pueden estar inactivos y aun así conservarse.
    this.categorias.update((a) => unir(a, [d.categoria]));
    this.responsables.update((a) => unir(a, [d.responsable]));
    this.formulario.patchValue({
      titulo: d.titulo,
      categoriaId: d.categoria.id,
      responsableId: d.responsable.id,
      fechaDocumento: d.fechaDocumento,
      fechaVencimiento: d.fechaVencimiento ?? '',
      descripcion: d.descripcion ?? '',
    });
  }
}

function unir(actual: Opcion[], nuevos: Opcion[]): Opcion[] {
  const porId = new Map([...actual, ...nuevos].map((o) => [o.id, o]));
  return [...porId.values()].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
}
