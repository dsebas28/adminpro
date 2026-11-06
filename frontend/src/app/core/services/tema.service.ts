import { DOCUMENT, Injectable, inject, signal } from '@angular/core';

export type Tema = 'light' | 'dark';

const CLAVE = 'adminpro.tema';

@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly documento = inject(DOCUMENT);

  /** tema-inicial.js ya fijó el atributo antes de que Angular arrancara. */
  readonly tema = signal<Tema>(
    this.documento.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light',
  );

  alternar(): void {
    this.aplicar(this.tema() === 'dark' ? 'light' : 'dark');
  }

  aplicar(tema: Tema): void {
    this.tema.set(tema);
    this.documento.documentElement.setAttribute('data-theme', tema);
    try {
      localStorage.setItem(CLAVE, tema);
    } catch {
      // Sin almacenamiento, el tema elegido dura hasta recargar la página.
    }
  }
}
