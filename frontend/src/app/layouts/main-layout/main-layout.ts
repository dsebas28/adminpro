import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ActivatedRouteSnapshot,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { filter } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { TemaService } from '../../core/services/tema.service';
import { Permiso } from '../../models/auth';
import { Boton } from '../../shared/components/boton';
import { Icono, NombreIcono } from '../../shared/components/icono';

interface EnlaceMenu {
  ruta: string;
  texto: string;
  icono: NombreIcono;
  permiso?: Permiso;
}

interface Miga {
  texto: string;
  ruta: string;
}

const CLAVE_COLAPSADO = 'adminpro.barra-colapsada';

const ENLACES: EnlaceMenu[] = [
  { ruta: '/', texto: 'Inicio', icono: 'inicio', permiso: 'DASHBOARD_LEER' },
  { ruta: '/empleados', texto: 'Empleados', icono: 'personas', permiso: 'EMPLEADOS_LEER' },
  { ruta: '/documentos', texto: 'Documentos', icono: 'documento', permiso: 'DOCUMENTOS_LEER' },
  { ruta: '/usuarios', texto: 'Usuarios', icono: 'usuario', permiso: 'USUARIOS_LEER' },
  { ruta: '/auditoria', texto: 'Auditoría', icono: 'historial', permiso: 'AUDITORIA_LEER' },
];

const NOMBRES_ROL: Record<string, string> = {
  ADMINISTRADOR: 'Administrador',
  SUPERVISOR: 'Supervisor',
  AUXILIAR: 'Auxiliar',
  CONSULTA: 'Consulta',
};

function leerColapsado(): boolean {
  try {
    return localStorage.getItem(CLAVE_COLAPSADO) === 'true';
  } catch {
    return false;
  }
}

@Component({
  selector: 'app-main-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Boton, Icono],
  template: `
    <a
      href="#contenido"
      class="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:rounded-control focus:bg-superficie focus:px-3 focus:py-2"
    >
      Ir al contenido
    </a>

    <div class="flex min-h-dvh">
      @if (menuMovil()) {
        <button
          type="button"
          class="fixed inset-0 z-30 bg-black/50 lg:hidden"
          aria-label="Cerrar menú"
          (click)="menuMovil.set(false)"
        ></button>
      }

      <aside
        class="fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-barra text-barra-texto transition-transform lg:sticky lg:top-0 lg:h-dvh lg:translate-x-0"
        [class.-translate-x-full]="!menuMovil()"
        [class.lg:w-16]="colapsado()"
      >
        <div class="flex h-14 items-center px-4">
          <a routerLink="/" class="text-lg font-bold tracking-tight text-white" aria-label="AdminPro, inicio">
            {{ colapsado() ? 'A' : 'AdminPro' }}
          </a>
        </div>

        <nav class="flex-1 space-y-0.5 overflow-y-auto px-2 py-2" aria-label="Principal">
          @for (enlace of enlaces(); track enlace.ruta) {
            <a
              [routerLink]="enlace.ruta"
              routerLinkActive="bg-barra-activo text-white"
              ariaCurrentWhenActive="page"
              [routerLinkActiveOptions]="{ exact: enlace.ruta === '/' }"
              class="flex h-10 items-center gap-3 rounded-control px-3 text-sm font-medium hover:bg-barra-activo hover:text-white focus-visible:outline-white"
              [attr.title]="colapsado() ? enlace.texto : null"
              (click)="menuMovil.set(false)"
            >
              <ui-icono [nombre]="enlace.icono" />
              <span [class.lg:sr-only]="colapsado()">{{ enlace.texto }}</span>
            </a>
          }
        </nav>

        <div class="space-y-0.5 px-2 py-2">
          <a
            routerLink="/sistema-diseno"
            routerLinkActive="bg-barra-activo text-white"
            class="flex h-10 items-center gap-3 rounded-control px-3 text-sm hover:bg-barra-activo hover:text-white focus-visible:outline-white"
            [attr.title]="colapsado() ? 'Sistema de diseño' : null"
            (click)="menuMovil.set(false)"
          >
            <ui-icono nombre="panel" />
            <span [class.lg:sr-only]="colapsado()">Sistema de diseño</span>
          </a>
          <button
            type="button"
            class="hidden h-10 w-full items-center gap-3 rounded-control px-3 text-sm hover:bg-barra-activo hover:text-white focus-visible:outline-white lg:flex"
            [attr.aria-expanded]="!colapsado()"
            [attr.aria-label]="colapsado() ? 'Expandir menú' : 'Colapsar menú'"
            (click)="alternarColapsado()"
          >
            <ui-icono [nombre]="colapsado() ? 'siguiente' : 'anterior'" />
            @if (!colapsado()) {
              <span>Colapsar</span>
            }
          </button>
        </div>
      </aside>

      <div class="flex min-w-0 flex-1 flex-col">
        <header class="sticky top-0 z-20 flex h-14 items-center gap-3 border-b border-linea bg-superficie px-3 sm:px-5">
          <button
            uiBoton
            type="button"
            variante="fantasma"
            tamano="icono"
            class="lg:hidden"
            aria-label="Abrir menú"
            (click)="menuMovil.set(true)"
          >
            <ui-icono nombre="menu" />
          </button>

          <nav class="min-w-0 flex-1" aria-label="Ruta de navegación">
            <ol class="flex items-center gap-1.5 text-sm">
              @for (miga of migas(); track miga.ruta; let ultima = $last) {
                <li class="flex min-w-0 items-center gap-1.5">
                  @if (ultima) {
                    <span class="truncate font-semibold" aria-current="page">{{ miga.texto }}</span>
                  } @else {
                    <a [routerLink]="miga.ruta" class="truncate text-tenue hover:text-texto">{{ miga.texto }}</a>
                    <span class="text-tenue" aria-hidden="true"><ui-icono nombre="siguiente" [tamano]="14" /></span>
                  }
                </li>
              }
            </ol>
          </nav>

          <div class="hidden text-right leading-tight sm:block">
            <p class="text-sm font-semibold">{{ auth.usuario()?.nombreVisible }}</p>
            <p class="text-xs text-tenue">{{ rol() }}</p>
          </div>

          <div class="flex items-center">
            <button
              uiBoton
              type="button"
              variante="fantasma"
              tamano="icono"
              [attr.aria-label]="tema.tema() === 'dark' ? 'Usar tema claro' : 'Usar tema oscuro'"
              [attr.title]="tema.tema() === 'dark' ? 'Usar tema claro' : 'Usar tema oscuro'"
              (click)="tema.alternar()"
            >
              <ui-icono [nombre]="tema.tema() === 'dark' ? 'sol' : 'luna'" />
            </button>
            <a
              uiBoton
              routerLink="/cambiar-password"
              variante="fantasma"
              tamano="icono"
              aria-label="Cambiar contraseña"
              title="Cambiar contraseña"
            >
              <ui-icono nombre="llave" />
            </a>
            <button
              uiBoton
              type="button"
              variante="fantasma"
              tamano="icono"
              aria-label="Cerrar sesión"
              title="Cerrar sesión"
              (click)="auth.logout()"
            >
              <ui-icono nombre="salir" />
            </button>
          </div>
        </header>

        <main id="contenido" tabindex="-1" class="flex-1 px-4 py-6 outline-none sm:px-6 lg:px-8">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class MainLayout {
  protected readonly auth = inject(AuthService);
  protected readonly tema = inject(TemaService);
  private readonly router = inject(Router);
  private readonly destruir = inject(DestroyRef);

  protected readonly colapsado = signal(leerColapsado());
  protected readonly menuMovil = signal(false);
  protected readonly migas = signal<Miga[]>([]);

  protected readonly enlaces = computed(() =>
    ENLACES.filter((enlace) => !enlace.permiso || this.auth.usuario()?.permisos.includes(enlace.permiso)),
  );
  protected readonly rol = computed(() => NOMBRES_ROL[this.auth.usuario()?.rol ?? ''] ?? '');

  constructor() {
    this.actualizarMigas();
    this.router.events
      .pipe(
        filter((evento) => evento instanceof NavigationEnd),
        takeUntilDestroyed(this.destruir),
      )
      .subscribe(() => this.actualizarMigas());
  }

  protected alternarColapsado(): void {
    const valor = !this.colapsado();
    this.colapsado.set(valor);
    try {
      localStorage.setItem(CLAVE_COLAPSADO, String(valor));
    } catch {
      // Sin almacenamiento, la preferencia dura hasta recargar la página.
    }
  }

  private actualizarMigas(): void {
    const migas: Miga[] = [];
    let ruta = '';
    let actual: ActivatedRouteSnapshot | null = this.router.routerState.snapshot.root;
    while (actual) {
      const segmentos = actual.url.map((s) => s.path).join('/');
      ruta = segmentos ? `${ruta}/${segmentos}` : ruta;
      const texto = actual.data['miga'] as string | undefined;
      if (texto) {
        migas.push({ texto, ruta: ruta || '/' });
      }
      actual = actual.firstChild;
    }
    this.migas.set(migas);
  }
}
