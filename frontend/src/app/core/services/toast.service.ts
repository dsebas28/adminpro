import { Injectable, signal } from '@angular/core';

export type TipoToast = 'exito' | 'error';

export interface Toast {
  id: number;
  tipo: TipoToast;
  mensaje: string;
}

const DURACION_MS = 5000;

@Injectable({ providedIn: 'root' })
export class ToastService {
  private siguienteId = 0;

  readonly toasts = signal<Toast[]>([]);

  exito(mensaje: string): void {
    this.agregar('exito', mensaje);
  }

  error(mensaje: string): void {
    this.agregar('error', mensaje);
  }

  cerrar(id: number): void {
    this.toasts.update((lista) => lista.filter((toast) => toast.id !== id));
  }

  private agregar(tipo: TipoToast, mensaje: string): void {
    if (this.toasts().some((toast) => toast.mensaje === mensaje)) {
      return; // Varias peticiones fallidas a la vez no deben apilar el mismo aviso.
    }
    const id = ++this.siguienteId;
    this.toasts.update((lista) => [...lista, { id, tipo, mensaje }]);
    setTimeout(() => this.cerrar(id), DURACION_MS);
  }
}
