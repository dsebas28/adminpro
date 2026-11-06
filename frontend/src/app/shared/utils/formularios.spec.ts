import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup } from '@angular/forms';

import {
  PATRON_CORREO,
  aplicarErroresServidor,
  coincideCon,
  mensajeDeError,
  passwordValida,
} from './formularios';

describe('validación de formularios', () => {
  describe('passwordValida', () => {
    const validar = (valor: string) => passwordValida(new FormControl(valor));

    it('acepta una contraseña de 10 caracteres', () => {
      expect(validar('1234567890')).toBeNull();
    });

    it('rechaza menos de 10 caracteres', () => {
      expect(validar('123456789')).toEqual({ minlength: { requiredLength: 10 } });
    });

    it('cuenta bytes, no caracteres: 60 eñes superan el límite de BCrypt', () => {
      expect(validar('ñ'.repeat(60))).toEqual({ passwordLarga: true });
    });

    it('acepta exactamente 72 bytes', () => {
      expect(validar('a'.repeat(72))).toBeNull();
    });

    it('deja el campo vacío a la validación de obligatorio', () => {
      expect(validar('')).toBeNull();
    });
  });

  it('coincideCon compara con el otro campo', () => {
    const formulario = new FormGroup({
      nueva: new FormControl('uno'),
      repetida: new FormControl('dos', coincideCon('nueva')),
    });
    formulario.controls.repetida.updateValueAndValidity();
    expect(formulario.controls.repetida.errors).toEqual({ noCoincide: true });

    formulario.controls.repetida.setValue('uno');
    expect(formulario.controls.repetida.errors).toBeNull();
  });

  it.each([
    ['ana@empresa.co', true],
    ['ana.perez@adminpro.test', true],
    ['sin-arroba', false],
    ['ana@sinpunto', false],
    ['ana perez@empresa.co', false],
  ])('el correo %s es válido: %s', (correo, esperado) => {
    expect(PATRON_CORREO.test(correo)).toBe(esperado);
  });

  it('traduce los errores a mensajes en español', () => {
    expect(mensajeDeError({ required: true })).toBe('Este campo es obligatorio');
    expect(mensajeDeError({ minlength: { requiredLength: 10 } })).toBe('Debe tener al menos 10 caracteres');
    expect(mensajeDeError({ servidor: 'La cédula ya existe' })).toBe('La cédula ya existe');
    expect(mensajeDeError(null)).toBeNull();
  });

  describe('aplicarErroresServidor', () => {
    const respuesta = (error: object) => new HttpErrorResponse({ status: 400, error });

    it('lleva cada error a su campo', () => {
      const formulario = new FormGroup({ cedula: new FormControl('') });

      const general = aplicarErroresServidor(
        formulario,
        respuesta({ code: 'VALIDACION', detail: 'x', errors: [{ field: 'cedula', message: 'Inválida' }] }),
      );

      expect(general).toBeNull();
      expect(formulario.controls.cedula.errors).toEqual({ servidor: 'Inválida' });
      expect(formulario.controls.cedula.touched).toBe(true);
    });

    it('devuelve el mensaje general si el error no es de un campo', () => {
      const formulario = new FormGroup({ cedula: new FormControl('') });

      expect(
        aplicarErroresServidor(formulario, respuesta({ code: 'CONFLICTO', detail: 'Ya existe' })),
      ).toBe('Ya existe');
    });

    it('un error sin formato de la API no rompe el formulario', () => {
      expect(aplicarErroresServidor(new FormGroup({}), new Error('red'))).toBeNull();
    });
  });
});
