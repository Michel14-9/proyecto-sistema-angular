// src/app/modules/auth/registrate/registrate.ts

import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';

declare var grecaptcha: any;

@Component({
  selector: 'app-registrate',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './registrate.html',
  styleUrls: ['./registrate.css'],
  encapsulation: ViewEncapsulation.None
})
export class RegistrateComponent implements OnInit {

  // Datos del formulario
  registroData = {
    nombres: '',
    apellidos: '',
    tipoDocumento: '',
    numeroDocumento: '',
    telefono: '',
    fechaNacimiento: '',
    email: '',
    password: '',
    recibirPromos: false,
    aceptaDatos: false
  };

  // ✅ Errores por campo
  errores = {
    nombres: '',
    apellidos: '',
    tipoDocumento: '',
    numeroDocumento: '',
    telefono: '',
    fechaNacimiento: '',
    email: '',
    password: '',
    aceptaDatos: ''
  };

  // Estados
  registroExitoso: boolean = false;
  errorMensaje: string = '';
  cargando: boolean = false;
  submitted: boolean = false;

  // Navbar
  isAuthenticated: boolean = false;
  username: string = '';
  totalCarrito: number = 0;

  // Validación de contraseña
  passwordValid = {
    minLength: false,
    hasUpperCase: false,
    hasLowerCase: false,
    hasNumber: false
  };
  passwordStrength: string = '';

  private readonly RECAPTCHA_SITE_KEY = "6LcOWNQrAAAAAD_mcy9fM5j71rg4kr0p-THrhQ-L";

  constructor(
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    this.carritoService.getTotal().subscribe({
      next: (total: number) => {
        this.totalCarrito = total;
      },
      error: () => {
        this.totalCarrito = 0;
      }
    });
  }

  // ✅ Validar solo letras y espacios (mínimo 3 caracteres)
  soloLetras(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '');
    this.registroData.nombres = input.value;
    this.validarCampo('nombres');
  }

  soloLetrasApellidos(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '');
    this.registroData.apellidos = input.value;
    this.validarCampo('apellidos');
  }

  // ✅ Validar solo números para DNI (máximo 8)
  soloNumerosDNI(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '').slice(0, 8);
    this.registroData.numeroDocumento = input.value;
    this.validarCampo('numeroDocumento');
  }

  // ✅ Validar solo números para teléfono (máximo 9)
  soloNumerosTelefono(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '').slice(0, 9);
    this.registroData.telefono = input.value;
    this.validarCampo('telefono');
  }

  // ✅ Validar campo específico en tiempo real
  validarCampo(campo: string): void {
    this.submitted = true;

    switch(campo) {
      case 'nombres':
        const nombreTrim = this.registroData.nombres.trim();
        if (!nombreTrim) {
          this.errores.nombres = 'El nombre es obligatorio';
        } else if (nombreTrim.length < 3) {
          this.errores.nombres = 'El nombre debe tener al menos 3 caracteres';
        } else if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(nombreTrim)) {
          this.errores.nombres = 'Solo se permiten letras y espacios';
        } else {
          this.errores.nombres = '';
        }
        break;

      case 'apellidos':
        const apellidoTrim = this.registroData.apellidos.trim();
        if (!apellidoTrim) {
          this.errores.apellidos = 'El apellido es obligatorio';
        } else if (apellidoTrim.length < 3) {
          this.errores.apellidos = 'El apellido debe tener al menos 3 caracteres';
        } else if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(apellidoTrim)) {
          this.errores.apellidos = 'Solo se permiten letras y espacios';
        } else {
          this.errores.apellidos = '';
        }
        break;

      case 'numeroDocumento':
        const doc = this.registroData.numeroDocumento.trim();
        if (!doc) {
          this.errores.numeroDocumento = 'El número de documento es obligatorio';
        } else if (this.registroData.tipoDocumento === 'DNI' && doc.length !== 8) {
          this.errores.numeroDocumento = 'El DNI debe tener exactamente 8 dígitos';
        } else if (this.registroData.tipoDocumento === 'Carné de extranjería' && doc.length !== 9) {
          this.errores.numeroDocumento = 'El carné de extranjería debe tener 9 dígitos';
        } else if (this.registroData.tipoDocumento === 'Pasaporte' && (doc.length < 6 || doc.length > 12)) {
          this.errores.numeroDocumento = 'El pasaporte debe tener entre 6 y 12 caracteres';
        } else {
          this.errores.numeroDocumento = '';
        }
        break;

      case 'telefono':
        const tel = this.registroData.telefono.trim();
        if (!tel) {
          this.errores.telefono = 'El teléfono es obligatorio';
        } else if (tel.length !== 9) {
          this.errores.telefono = 'El teléfono debe tener exactamente 9 dígitos';
        } else {
          this.errores.telefono = '';
        }
        break;

      case 'email':
        const email = this.registroData.email.trim();
        if (!email) {
          this.errores.email = 'El correo electrónico es obligatorio';
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
          this.errores.email = 'Ingresa un correo electrónico válido';
        } else {
          this.errores.email = '';
        }
        break;

      case 'fechaNacimiento':
        if (!this.registroData.fechaNacimiento) {
          this.errores.fechaNacimiento = 'La fecha de nacimiento es obligatoria';
        } else {
          const fechaNac = new Date(this.registroData.fechaNacimiento);
          const hoy = new Date();
          let edad = hoy.getFullYear() - fechaNac.getFullYear();
          const mes = hoy.getMonth() - fechaNac.getMonth();
          if (mes < 0 || (mes === 0 && hoy.getDate() < fechaNac.getDate())) {
            edad--;
          }
          if (edad < 18) {
            this.errores.fechaNacimiento = 'Debes ser mayor de edad (18 años o más)';
          } else {
            this.errores.fechaNacimiento = '';
          }
        }
        break;

      case 'tipoDocumento':
        if (!this.registroData.tipoDocumento) {
          this.errores.tipoDocumento = 'Selecciona un tipo de documento';
        } else {
          this.errores.tipoDocumento = '';
          // Re-validar número de documento cuando cambia el tipo
          this.validarCampo('numeroDocumento');
        }
        break;

      case 'password':
        if (!this.registroData.password) {
          this.errores.password = 'La contraseña es obligatoria';
        } else if (this.registroData.password.length < 6) {
          this.errores.password = 'La contraseña debe tener al menos 6 caracteres';
        } else if (!/(?=.*[a-z])/.test(this.registroData.password)) {
          this.errores.password = 'Debe tener al menos una minúscula';
        } else if (!/(?=.*[A-Z])/.test(this.registroData.password)) {
          this.errores.password = 'Debe tener al menos una mayúscula';
        } else if (!/(?=.*\d)/.test(this.registroData.password)) {
          this.errores.password = 'Debe tener al menos un número';
        } else {
          this.errores.password = '';
        }
        break;

      case 'aceptaDatos':
        if (!this.registroData.aceptaDatos) {
          this.errores.aceptaDatos = 'Debes aceptar los términos y condiciones';
        } else {
          this.errores.aceptaDatos = '';
        }
        break;
    }
  }

  // ✅ Validar todos los campos
  private validarTodosLosCampos(): boolean {
    this.submitted = true;
    const campos = ['nombres', 'apellidos', 'tipoDocumento', 'numeroDocumento', 'telefono', 'fechaNacimiento', 'email', 'password', 'aceptaDatos'];
    campos.forEach(campo => this.validarCampo(campo));

    // Verificar si hay algún error
    return Object.values(this.errores).every(error => error === '');
  }

  // Validar contraseña en tiempo real
  validarPassword(): void {
    const password = this.registroData.password;

    this.passwordValid = {
      minLength: password.length >= 6,
      hasUpperCase: /[A-Z]/.test(password),
      hasLowerCase: /[a-z]/.test(password),
      hasNumber: /\d/.test(password)
    };

    if (password.length === 0) {
      this.passwordStrength = '';
    } else if (password.length < 6) {
      this.passwordStrength = 'Débil - Muy corta (mínimo 6 caracteres)';
    } else if (password.length < 8) {
      this.passwordStrength = 'Media - Podría ser más segura';
    } else if (/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/.test(password)) {
      this.passwordStrength = 'Fuerte - ¡Excelente! Cumple todos los requisitos';
    } else {
      this.passwordStrength = 'Media - Usa mayúsculas, minúsculas y números';
    }

    this.validarCampo('password');
  }

  // ✅ Envío del formulario
  registrar(): void {
    if (!this.validarTodosLosCampos()) {
      // ✅ Scroll al primer error
      const primerError = document.querySelector('.is-invalid');
      if (primerError) {
        primerError.scrollIntoView({ behavior: 'smooth', block: 'center' });
        (primerError as HTMLElement).focus();
      }
      return;
    }

    this.cargando = true;
    this.errorMensaje = '';

    if (typeof grecaptcha === 'undefined') {
      this.errorMensaje = 'Error de verificación de seguridad. Recarga la página.';
      this.cargando = false;
      return;
    }

    console.log('⏳ Obteniendo token de reCAPTCHA v3...');

    grecaptcha.ready(() => {
      grecaptcha.execute(this.RECAPTCHA_SITE_KEY, { action: 'register' })
        .then((token: string) => {
          console.log('✅ reCAPTCHA v3 token obtenido:', token);
          this.enviarRegistro(token);
        })
        .catch((error: any) => {
          console.error('❌ Error en reCAPTCHA:', error);
          this.cargando = false;
          this.errorMensaje = 'Error de verificación de seguridad. Intenta nuevamente.';
        });
    });
  }

  enviarRegistro(captchaToken: string): void {
    const datosRegistro = {
      nombres: this.registroData.nombres.trim(),
      apellidos: this.registroData.apellidos.trim(),
      tipoDocumento: this.registroData.tipoDocumento,
      numeroDocumento: this.registroData.numeroDocumento.trim(),
      telefono: this.registroData.telefono.trim(),
      fechaNacimiento: this.registroData.fechaNacimiento,
      email: this.registroData.email.trim().toLowerCase(),
      password: this.registroData.password,
      rol: 'CLIENTE',
      'g-recaptcha-response': captchaToken
    };

    console.log('📤 Enviando datos de registro:', datosRegistro);

    this.authService.register(datosRegistro).subscribe({
      next: (response: any) => {
        console.log('✅ Registro exitoso:', response);
        this.registroExitoso = true;
        this.cargando = false;

        setTimeout(() => {
          this.router.navigate(['/login'], { queryParams: { registroExitoso: 'true' } });
        }, 3000);
      },
      error: (error: any) => {
        console.error('❌ Error en registro:', error);
        this.errorMensaje = error.error?.message || error.error?.mensaje || 'Error al registrar. Intenta nuevamente';
        this.cargando = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
