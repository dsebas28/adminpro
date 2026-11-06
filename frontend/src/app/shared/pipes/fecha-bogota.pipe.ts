import { Pipe, PipeTransform } from '@angular/core';

const FECHA = new Intl.DateTimeFormat('es-CO', {
  timeZone: 'America/Bogota',
  dateStyle: 'medium',
});

const FECHA_HORA = new Intl.DateTimeFormat('es-CO', {
  timeZone: 'America/Bogota',
  dateStyle: 'medium',
  timeStyle: 'short',
});

/**
 * Muestra la hora de Colombia sin importar la zona del navegador. Las fechas de calendario
 * (YYYY-MM-DD) se interpretan al mediodía de Bogotá, porque como UTC caerían el día anterior.
 */
@Pipe({ name: 'fechaBogota' })
export class FechaBogotaPipe implements PipeTransform {
  transform(valor: string | null | undefined, conHora = false): string {
    if (!valor) {
      return '';
    }
    const esCalendario = /^\d{4}-\d{2}-\d{2}$/.test(valor);
    const fecha = new Date(esCalendario ? `${valor}T12:00:00-05:00` : valor);
    return Number.isNaN(fecha.getTime()) ? '' : (conHora ? FECHA_HORA : FECHA).format(fecha);
  }
}
