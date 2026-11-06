import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../core/auth/auth.service';
import { Login } from './login';

describe('Login', () => {
  const login = vi.fn();

  async function crear() {
    const fixture = TestBed.createComponent(Login);
    await fixture.whenStable();
    return fixture;
  }

  function escribir(elemento: HTMLElement, selector: string, valor: string): void {
    const input = elemento.querySelector<HTMLInputElement>(selector)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
  }

  beforeEach(() => {
    login.mockReset();
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: AuthService, useValue: { login } }],
    });
  });

  it('muestra errores por campo y no llama al servidor si está vacío', async () => {
    const fixture = await crear();
    const elemento: HTMLElement = fixture.nativeElement;

    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(login).not.toHaveBeenCalled();
    expect(elemento.textContent).toContain('Este campo es obligatorio');
    expect(elemento.querySelector('input[type=email]')!.getAttribute('aria-invalid')).toBe('true');
  });

  it('cada campo tiene una etiqueta asociada', async () => {
    const fixture = await crear();
    const elemento: HTMLElement = fixture.nativeElement;

    for (const input of Array.from(elemento.querySelectorAll('input'))) {
      expect(elemento.querySelector(`label[for="${input.id}"]`)).not.toBeNull();
    }
  });

  it('rechaza un correo mal escrito', async () => {
    const fixture = await crear();
    const elemento: HTMLElement = fixture.nativeElement;

    escribir(elemento, 'input[type=email]', 'sin-arroba');
    await fixture.whenStable();

    expect(elemento.textContent).toContain('El formato no es válido');
  });

  it('muestra el mensaje del servidor cuando las credenciales fallan', async () => {
    login.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            error: { code: 'CREDENCIALES_INVALIDAS', detail: 'Correo o contraseña incorrectos.' },
          }),
      ),
    );
    const fixture = await crear();
    const elemento: HTMLElement = fixture.nativeElement;

    escribir(elemento, 'input[type=email]', 'ana@empresa.co');
    escribir(elemento, 'input[type=password]', 'incorrecta');
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(elemento.querySelector('[role=alert]')?.textContent).toContain('Correo o contraseña incorrectos.');
  });

  it('con contraseña temporal lleva a cambiarla', async () => {
    login.mockReturnValue(of({ debeCambiarPassword: true }));
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = await crear();
    const elemento: HTMLElement = fixture.nativeElement;

    escribir(elemento, 'input[type=email]', 'ana@empresa.co');
    escribir(elemento, 'input[type=password]', 'Temporal.2026');
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));

    expect(navegar).toHaveBeenCalledWith('/cambiar-password');
  });
});
