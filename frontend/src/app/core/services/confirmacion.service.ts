import { Injectable, signal } from '@angular/core';

export interface SolicitudConfirmacion {
  titulo: string;
  mensaje: string;
  accion: string;
  peligro?: boolean;
}

interface Pendiente extends SolicitudConfirmacion {
  responder: (confirmado: boolean) => void;
}

/** Pide confirmación antes de una acción. El diálogo vive en el componente raíz. */
@Injectable({ providedIn: 'root' })
export class ConfirmacionService {
  readonly pendiente = signal<Pendiente | null>(null);

  pedir(solicitud: SolicitudConfirmacion): Promise<boolean> {
    return new Promise((responder) => this.pendiente.set({ ...solicitud, responder }));
  }

  responder(confirmado: boolean): void {
    this.pendiente()?.responder(confirmado);
    this.pendiente.set(null);
  }
}
