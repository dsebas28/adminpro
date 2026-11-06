import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/services/api.service';
import { ConfirmacionService } from '../../core/services/confirmacion.service';
import { ToastService } from '../../core/services/toast.service';
import { Adjunto, Documento, HistorialDocumento } from '../../models/negocio';
import { Boton } from '../../shared/components/boton';
import { Campo, CampoInput } from '../../shared/components/campo';
import { Esqueleto } from '../../shared/components/esqueleto';
import { Estado } from '../../shared/components/estado';
import { EventoTiempo, LineaTiempo } from '../../shared/components/linea-tiempo';
import { Modal } from '../../shared/components/modal';
import { Vacio } from '../../shared/components/vacio';
import { TienePermiso } from '../../shared/directives/tiene-permiso';
import { FechaBogotaPipe } from '../../shared/pipes/fecha-bogota.pipe';
import { ESTADOS, tamanoLegible, textoHistorial } from '../../shared/utils/etiquetas';
import { mensajeDeFallo } from '../../shared/utils/formularios';

const MAX_BYTES = 10 * 1024 * 1024;

interface BotonAccion {
  accion: string;
  ruta: string;
  texto: string;
  exito: string;
  primario: boolean;
}

const TRANSICIONES: BotonAccion[] = [
  { accion: 'ENVIAR', ruta: 'enviar', texto: 'Enviar a revisión', exito: 'Documento enviado', primario: true },
  { accion: 'INICIAR_REVISION', ruta: 'iniciar-revision', texto: 'Iniciar revisión', exito: 'Revisión iniciada', primario: true },
  { accion: 'APROBAR', ruta: 'aprobar', texto: 'Aprobar', exito: 'Documento aprobado', primario: true },
  { accion: 'FINALIZAR', ruta: 'finalizar', texto: 'Finalizar', exito: 'Documento finalizado', primario: true },
];

@Component({
  selector: 'app-documento-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, Boton, Campo, CampoInput, Esqueleto, Estado, FechaBogotaPipe, LineaTiempo, Modal, TienePermiso, Vacio],
  template: `
    @if (documento(); as d) {
      <p class="font-numerador text-sm font-medium text-tinta">{{ d.consecutivo }}</p>
      <div class="mt-1 flex flex-wrap items-start justify-between gap-3">
        <div class="min-w-0">
          <h1 class="text-2xl font-bold">{{ d.titulo }}</h1>
          <p class="mt-1.5 flex flex-wrap items-center gap-1.5">
            <ui-estado [tono]="estados[d.estado].tono">{{ estados[d.estado].texto }}</ui-estado>
            @if (d.vencido) {
              <ui-estado tono="peligro">Vencido</ui-estado>
            }
            @if (!d.activo) {
              <ui-estado tono="neutro">Desactivado</ui-estado>
            }
          </p>
        </div>
        <div class="flex flex-wrap gap-2">
          @for (b of botones(); track b.accion) {
            <button uiBoton type="button" [cargando]="ocupado()" (click)="transitar(d, b)">{{ b.texto }}</button>
          }
          @if (puede('DEVOLVER')) {
            <button uiBoton type="button" variante="secundario" (click)="abrirDevolver()">Devolver</button>
          }
          @if (puede('EDITAR')) {
            <a uiBoton variante="secundario" [routerLink]="['/documentos', d.id, 'editar']">Editar</a>
          }
          <ng-container *tienePermiso="'DOCUMENTOS_DESACTIVAR'">
            @if (d.activo) {
              <button uiBoton type="button" variante="fantasma" (click)="desactivar(d)">Desactivar</button>
            } @else {
              <button uiBoton type="button" variante="secundario" (click)="cambiarActivo(d, true)">Activar</button>
            }
          </ng-container>
        </div>
      </div>

      <div class="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[1fr_24rem]">
        <div class="space-y-6">
          <section class="rounded-panel border border-linea bg-superficie p-5">
            <h2 class="font-semibold">Datos</h2>
            <dl class="mt-3 grid gap-x-6 gap-y-3 text-sm sm:grid-cols-2">
              <div><dt class="text-tenue">Categoría</dt><dd class="font-medium">{{ d.categoria.nombre }}</dd></div>
              <div><dt class="text-tenue">Responsable</dt><dd class="font-medium">{{ d.responsable.nombre }}</dd></div>
              <div><dt class="text-tenue">Fecha del documento</dt><dd class="font-medium">{{ d.fechaDocumento | fechaBogota }}</dd></div>
              <div>
                <dt class="text-tenue">Vencimiento</dt>
                <dd class="font-medium">{{ d.fechaVencimiento ? (d.fechaVencimiento | fechaBogota) : 'No vence' }}</dd>
              </div>
            </dl>
            @if (d.descripcion) {
              <p class="mt-3 border-t border-linea pt-3 text-sm whitespace-pre-line">{{ d.descripcion }}</p>
            }
          </section>

          <section class="rounded-panel border border-linea bg-superficie p-5">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <h2 class="font-semibold">Adjuntos</h2>
              @if (puede('EDITAR')) {
                <label
                  class="inline-flex h-8 cursor-pointer items-center rounded-control border border-linea-fuerte px-3 text-[0.8125rem] font-semibold hover:bg-superficie-2 has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-tinta"
                >
                  {{ subiendo() ? 'Subiendo...' : 'Agregar archivo' }}
                  <input
                    type="file"
                    class="sr-only"
                    accept=".pdf,.docx,.xlsx,.png,.jpg,.jpeg,.webp"
                    [disabled]="subiendo()"
                    (change)="subir(d, $event)"
                  />
                </label>
              }
            </div>
            @if (d.adjuntos.length) {
              <ul class="mt-2 divide-y divide-linea">
                @for (a of d.adjuntos; track a.id) {
                  <li class="flex items-center justify-between gap-3 py-2 text-sm">
                    <span class="min-w-0">
                      <span class="block truncate font-medium">{{ a.nombreOriginal }}</span>
                      <span class="text-xs text-tenue">{{ tamano(a.tamanoBytes) }}</span>
                    </span>
                    <span class="flex shrink-0 gap-1">
                      <button uiBoton type="button" variante="fantasma" tamano="pequeno" (click)="descargar(d, a)">Descargar</button>
                      @if (puede('EDITAR')) {
                        <button uiBoton type="button" variante="fantasma" tamano="pequeno" (click)="eliminar(d, a)">Eliminar</button>
                      }
                    </span>
                  </li>
                }
              </ul>
            } @else {
              <p class="mt-2 text-sm text-tenue">
                Sin adjuntos. Se necesita al menos uno para enviar el documento a revisión.
              </p>
            }
            @if (puede('EDITAR')) {
              <p class="mt-2 text-xs text-tenue">PDF, DOCX, XLSX o imágenes. Máximo 10 MB y 10 archivos.</p>
            }
          </section>
        </div>

        <section class="rounded-panel border border-linea bg-superficie p-5">
          <h2 class="font-semibold">Historial</h2>
          <div class="mt-3"><ui-linea-tiempo [eventos]="eventos()" /></div>
        </section>
      </div>

      <ui-modal titulo="Devolver documento" [abierto]="devolviendo()" (cerrado)="devolviendo.set(false)">
        <ui-campo etiqueta="Motivo de la devolución" ayuda="Quien lo registró verá este comentario.">
          <textarea uiInput class="h-24 py-2" [formControl]="comentario"></textarea>
        </ui-campo>
        <ng-container acciones>
          <button uiBoton type="button" variante="secundario" (click)="devolviendo.set(false)">Cancelar</button>
          <button uiBoton type="button" [cargando]="ocupado()" (click)="devolver(d)">Devolver</button>
        </ng-container>
      </ui-modal>
    } @else if (noExiste()) {
      <ui-vacio titulo="Este documento no existe" mensaje="Puede que el enlace esté mal escrito.">
        <a uiBoton routerLink="/documentos" variante="secundario">Volver a documentos</a>
      </ui-vacio>
    } @else {
      <ui-esqueleto alto="2rem" ancho="50%" />
      <ui-esqueleto class="mt-6" alto="16rem" />
    }
  `,
})
export class DocumentoDetalle implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toasts = inject(ToastService);
  private readonly confirmacion = inject(ConfirmacionService);

  readonly id = input.required({ transform: numberAttribute });

  protected readonly estados = ESTADOS;
  protected readonly tamano = tamanoLegible;
  protected readonly documento = signal<Documento | null>(null);
  protected readonly noExiste = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly subiendo = signal(false);
  protected readonly devolviendo = signal(false);
  protected readonly comentario = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(1000)],
  });
  private readonly historial = signal<HistorialDocumento[]>([]);

  protected readonly botones = computed(() =>
    TRANSICIONES.filter((b) => this.documento()?.accionesDisponibles.includes(b.accion as never)),
  );
  protected readonly eventos = computed<EventoTiempo[]>(() =>
    this.historial().map((h) => ({
      id: h.id,
      titulo: textoHistorial(h.accion, h.estadoAnterior, h.estadoNuevo),
      detalle: h.comentario,
      autor: h.usuario?.nombre,
      fecha: h.createdAt,
    })),
  );

  ngOnInit(): void {
    this.api.documento(this.id()).subscribe({
      next: (d) => this.documento.set(d),
      error: () => this.noExiste.set(true),
    });
    this.cargarHistorial();
  }

  protected puede(accion: string): boolean {
    return this.documento()?.accionesDisponibles.includes(accion as never) ?? false;
  }

  protected transitar(d: Documento, boton: BotonAccion): void {
    this.ejecutarTransicion(d, boton.ruta, boton.exito);
  }

  protected abrirDevolver(): void {
    this.comentario.reset();
    this.devolviendo.set(true);
  }

  protected devolver(d: Documento): void {
    if (this.comentario.invalid || !this.comentario.value.trim()) {
      this.comentario.setErrors({ required: true });
      this.comentario.markAsTouched();
      return;
    }
    this.ejecutarTransicion(d, 'devolver', 'Documento devuelto', this.comentario.value.trim());
  }

  protected async desactivar(d: Documento): Promise<void> {
    const confirmado = await this.confirmacion.pedir({
      titulo: 'Desactivar documento',
      mensaje: `${d.consecutivo} dejará de aparecer en los listados y su flujo quedará detenido.`,
      accion: 'Desactivar',
      peligro: true,
    });
    if (confirmado) {
      this.cambiarActivo(d, false);
    }
  }

  protected cambiarActivo(d: Documento, activar: boolean): void {
    this.api.cambiarDocumento(d.id, activar).subscribe({
      next: (actualizado) => {
        this.actualizar(actualizado);
        this.toasts.exito(activar ? 'Documento activado' : 'Documento desactivado');
      },
      error: (fallo: unknown) => this.toasts.error(mensajeDeFallo(fallo)),
    });
  }

  protected subir(d: Documento, evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = ''; // Permite volver a elegir el mismo archivo.
    if (!archivo) {
      return;
    }
    if (archivo.size > MAX_BYTES) {
      this.toasts.error('El archivo supera el máximo de 10 MB.');
      return;
    }
    this.subiendo.set(true);
    this.api.agregarAdjunto(d.id, archivo).subscribe({
      next: () => {
        this.subiendo.set(false);
        this.recargar();
        this.toasts.exito('Archivo agregado');
      },
      error: (fallo: unknown) => {
        this.subiendo.set(false);
        this.toasts.error(mensajeDeFallo(fallo));
      },
    });
  }

  protected descargar(d: Documento, adjunto: Adjunto): void {
    this.api.descargarAdjunto(d.id, adjunto.id).subscribe({
      next: (contenido) => {
        const url = URL.createObjectURL(contenido);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = adjunto.nombreOriginal;
        enlace.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.toasts.error('No se pudo descargar el archivo.'),
    });
  }

  protected async eliminar(d: Documento, adjunto: Adjunto): Promise<void> {
    const confirmado = await this.confirmacion.pedir({
      titulo: 'Eliminar adjunto',
      mensaje: `${adjunto.nombreOriginal} dejará de estar disponible en este documento.`,
      accion: 'Eliminar',
      peligro: true,
    });
    if (!confirmado) {
      return;
    }
    this.api.eliminarAdjunto(d.id, adjunto.id).subscribe({
      next: () => {
        this.recargar();
        this.toasts.exito('Adjunto eliminado');
      },
      error: (fallo: unknown) => this.toasts.error(mensajeDeFallo(fallo)),
    });
  }

  private ejecutarTransicion(d: Documento, ruta: string, exito: string, comentario?: string): void {
    this.ocupado.set(true);
    this.api.transitar(d.id, ruta, d.version, comentario).subscribe({
      next: (actualizado) => {
        this.ocupado.set(false);
        this.devolviendo.set(false);
        this.actualizar(actualizado);
        this.toasts.exito(exito);
      },
      error: (fallo: unknown) => {
        this.ocupado.set(false);
        this.toasts.error(mensajeDeFallo(fallo));
        this.recargar(); // Otra persona pudo cambiarlo: se muestra el estado real.
      },
    });
  }

  private actualizar(documento: Documento): void {
    this.documento.set(documento);
    this.cargarHistorial();
  }

  private recargar(): void {
    this.api.documento(this.id()).subscribe((d) => this.actualizar(d));
  }

  private cargarHistorial(): void {
    this.api.historialDocumento(this.id()).subscribe({
      next: (pagina) => this.historial.set(pagina.content),
      error: () => this.historial.set([]),
    });
  }
}
