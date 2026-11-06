import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { ApiService } from '../../core/services/api.service';
import { Actividad, Alerta, ConteoEstado, DocumentoItem, Resumen } from '../../models/negocio';
import { Esqueleto } from '../../shared/components/esqueleto';
import { Estado } from '../../shared/components/estado';
import { Icono } from '../../shared/components/icono';
import { Vacio } from '../../shared/components/vacio';
import { FechaBogotaPipe } from '../../shared/pipes/fecha-bogota.pipe';
import { ESTADOS, textoAccion } from '../../shared/utils/etiquetas';
import { GraficoEstados } from './grafico-estados';

interface Datos {
  resumen: Resumen;
  estados: ConteoEstado[];
  porVencer: DocumentoItem[];
  actividad: Actividad[];
  alertas: Alerta[];
}

const SEVERIDAD: Record<Alerta['severidad'], string> = { ALTA: 'Urgente', MEDIA: 'Revisar', BAJA: 'Próximo' };

@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Esqueleto, Estado, FechaBogotaPipe, GraficoEstados, Icono, Vacio],
  template: `
    <h1 class="text-2xl font-bold">Hola, {{ nombre() }}</h1>

    @if (fallo()) {
      <ui-vacio
        class="mt-6 rounded-panel border border-linea bg-superficie"
        icono="alerta"
        titulo="No se pudieron cargar los indicadores"
        mensaje="Recarga la página para intentarlo de nuevo."
      />
    } @else if (datos(); as d) {
      <dl class="mt-5 grid grid-cols-2 divide-linea overflow-hidden rounded-panel border border-linea bg-superficie lg:grid-cols-4 lg:divide-x">
        <div class="p-4">
          <dt class="text-sm text-tenue">Empleados activos</dt>
          <dd class="mt-1 text-3xl font-bold">{{ d.resumen.empleadosActivos }}</dd>
        </div>
        <div class="p-4">
          <dt class="text-sm text-tenue">Documentos activos</dt>
          <dd class="mt-1 text-3xl font-bold">{{ d.resumen.documentosActivos }}</dd>
        </div>
        <div class="p-4">
          <dt class="text-sm text-tenue">Vencen en 30 días</dt>
          <dd class="mt-1 text-3xl font-bold">{{ d.resumen.documentosPorVencer }}</dd>
        </div>
        <div class="p-4">
          <dt class="text-sm text-tenue">Vencidos</dt>
          <dd class="mt-1 flex items-center gap-2 text-3xl font-bold">
            {{ d.resumen.documentosVencidos }}
            @if (d.resumen.documentosVencidos > 0) {
              <span class="text-peligro"><ui-icono nombre="alerta" [tamano]="22" /></span>
            }
          </dd>
        </div>
      </dl>

      <div class="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section class="rounded-panel border border-linea bg-superficie p-4">
          <h2 class="font-semibold">Documentos por estado</h2>
          <div class="mt-3"><app-grafico-estados [datos]="d.estados" /></div>
        </section>

        <section class="rounded-panel border border-linea bg-superficie p-4">
          <h2 class="font-semibold">Próximos a vencer</h2>
          @if (d.porVencer.length) {
            <ul class="mt-2 divide-y divide-linea">
              @for (doc of d.porVencer; track doc.id) {
                <li class="flex items-center justify-between gap-3 py-2 text-sm">
                  <a [routerLink]="['/documentos', doc.id]" class="min-w-0 hover:underline">
                    <span class="block truncate font-medium">{{ doc.titulo }}</span>
                    <span class="block font-numerador text-xs text-tenue">{{ doc.consecutivo }}</span>
                  </a>
                  <span class="shrink-0 text-right">
                    <span class="block">{{ doc.fechaVencimiento | fechaBogota }}</span>
                    <ui-estado [tono]="estados[doc.estado].tono">{{ estados[doc.estado].texto }}</ui-estado>
                  </span>
                </li>
              }
            </ul>
          } @else {
            <ui-vacio titulo="Nada vence pronto" mensaje="Ningún documento vence en los próximos 30 días." />
          }
        </section>

        <section class="rounded-panel border border-linea bg-superficie p-4">
          <h2 class="font-semibold">Alertas</h2>
          @if (d.alertas.length) {
            <ul class="mt-2 max-h-80 divide-y divide-linea overflow-y-auto">
              @for (alerta of d.alertas; track alerta.tipo + alerta.entidadId) {
                <li class="flex items-start gap-2.5 py-2 text-sm">
                  <ui-estado class="mt-0.5 shrink-0" [tono]="tono(alerta)">{{ severidad[alerta.severidad] }}</ui-estado>
                  <a [routerLink]="['/documentos', alerta.entidadId]" class="hover:underline">{{ alerta.mensaje }}</a>
                </li>
              }
            </ul>
          } @else {
            <ui-vacio titulo="Sin alertas" mensaje="Todos los documentos están al día." />
          }
        </section>

        <section class="rounded-panel border border-linea bg-superficie p-4">
          <h2 class="font-semibold">Actividad reciente</h2>
          @if (d.actividad.length) {
            <ul class="mt-2 divide-y divide-linea">
              @for (evento of d.actividad; track $index) {
                <li class="py-2 text-sm">
                  <p>
                    <span class="font-medium">{{ evento.usuario ?? 'Sistema' }}</span>
                    {{ accion(evento.accion).toLowerCase() }} {{ evento.etiqueta }}
                  </p>
                  <p class="text-xs text-tenue">{{ evento.fecha | fechaBogota: true }}</p>
                </li>
              }
            </ul>
          } @else {
            <ui-vacio titulo="Todavía no hay actividad" />
          }
        </section>
      </div>
    } @else {
      <div class="mt-5 space-y-4" aria-busy="true">
        <ui-esqueleto alto="6rem" />
        <div class="grid gap-6 lg:grid-cols-2">
          <ui-esqueleto alto="18rem" />
          <ui-esqueleto alto="18rem" />
        </div>
      </div>
    }
  `,
})
export class Dashboard {
  private readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);

  protected readonly estados = ESTADOS;
  protected readonly severidad = SEVERIDAD;
  protected readonly datos = signal<Datos | null>(null);
  protected readonly fallo = signal(false);
  protected readonly nombre = computed(() => this.auth.usuario()?.nombreVisible.split(' ')[0] ?? '');
  protected readonly accion = textoAccion;

  constructor() {
    forkJoin({
      resumen: this.api.resumen(),
      estados: this.api.documentosPorEstado(),
      porVencer: this.api.documentosPorVencer(),
      actividad: this.api.actividadReciente(),
      alertas: this.api.alertas(),
    }).subscribe({
      next: (r) => this.datos.set({ ...r, porVencer: r.porVencer.content }),
      error: () => this.fallo.set(true),
    });
  }

  protected tono(alerta: Alerta): 'peligro' | 'aviso' | 'neutro' {
    return alerta.severidad === 'ALTA' ? 'peligro' : alerta.severidad === 'MEDIA' ? 'aviso' : 'neutro';
  }
}
