import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';

import { ProblemDetails } from '../../models/api';

const LONGITUD_MINIMA_PASSWORD = 10;
const MAX_BYTES_PASSWORD = 72;

/** Misma expresión que Patrones.CORREO en el backend y el CHECK de la base de datos. */
export const PATRON_CORREO = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

/** Mensaje en español para el primer error de un control. */
export function mensajeDeError(errores: ValidationErrors | null): string | null {
  if (!errores) {
    return null;
  }
  if (errores['servidor']) return errores['servidor'] as string;
  if (errores['required']) return 'Este campo es obligatorio';
  if (errores['pattern']) return 'El formato no es válido';
  if (errores['minlength']) return `Debe tener al menos ${errores['minlength'].requiredLength} caracteres`;
  if (errores['maxlength']) return `Debe tener máximo ${errores['maxlength'].requiredLength} caracteres`;
  if (errores['passwordLarga']) return 'La contraseña es demasiado larga';
  if (errores['noCoincide']) return 'Las contraseñas no coinciden';
  return 'El valor no es válido';
}

/** BCrypt admite 72 bytes, no 72 caracteres: una tilde ocupa dos. Ver D-30. */
export const passwordValida: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = (control.value ?? '') as string;
  if (!valor) {
    return null;
  }
  if (valor.length < LONGITUD_MINIMA_PASSWORD) {
    return { minlength: { requiredLength: LONGITUD_MINIMA_PASSWORD } };
  }
  return new TextEncoder().encode(valor).length > MAX_BYTES_PASSWORD ? { passwordLarga: true } : null;
};

/** Valida que un campo repita el valor de otro del mismo formulario. */
export function coincideCon(campo: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const otro = control.parent?.get(campo);
    return otro && control.value !== otro.value ? { noCoincide: true } : null;
  };
}

export function problemaDe(error: unknown): ProblemDetails | null {
  if (error instanceof HttpErrorResponse && error.error?.code) {
    return error.error as ProblemDetails;
  }
  return null;
}

/** Mensaje para un aviso cuando falla una acción que no pertenece a un formulario. */
export function mensajeDeFallo(error: unknown): string {
  const problema = problemaDe(error);
  return problema?.errors?.[0]?.message ?? problema?.detail ?? 'No se pudo completar la acción.';
}

/**
 * Lleva los errores de validación del backend a sus campos. Devuelve el mensaje general
 * cuando el error no corresponde a ningún campo, para mostrarlo arriba del formulario.
 */
export function aplicarErroresServidor(formulario: FormGroup, error: unknown): string | null {
  const problema = problemaDe(error);
  if (!problema) {
    return null;
  }
  let huboErrorDeCampo = false;
  for (const { field, message } of problema.errors ?? []) {
    const control = formulario.get(field);
    if (control) {
      control.setErrors({ servidor: message });
      control.markAsTouched();
      huboErrorDeCampo = true;
    }
  }
  return huboErrorDeCampo ? null : problema.detail;
}
