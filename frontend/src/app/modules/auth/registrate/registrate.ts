// src/app/modules/auth/registrate/registrate.ts
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { CarritoService } from '../../../core/services/carrito';

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

  // Estados
  registroExitoso: boolean = false;
  errorMensaje: string = '';
  cargando: boolean = false;

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
      }
    });
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
  }

  // Validación completa del formulario
  private validarFormularioCompleto(): boolean {
    this.errorMensaje = '';

    if (!this.registroData.nombres.trim()) {
      this.errorMensaje = 'El nombre es obligatorio';
      return false;
    }

    if (!this.registroData.apellidos.trim()) {
      this.errorMensaje = 'El apellido es obligatorio';
      return false;
    }

    if (!this.registroData.tipoDocumento) {
      this.errorMensaje = 'Selecciona un tipo de documento';
      return false;
    }

    // Validar número de documento
    const valorDoc = this.registroData.numeroDocumento;
    if (!valorDoc) {
      this.errorMensaje = 'El número de documento es obligatorio';
      return false;
    }

    switch (this.registroData.tipoDocumento) {
      case 'DNI':
        if (!/^\d{8}$/.test(valorDoc)) {
          this.errorMensaje = 'El DNI debe tener 8 dígitos';
          return false;
        }
        break;
      case 'Pasaporte':
        if (!/^[A-Z0-9]{6,12}$/i.test(valorDoc)) {
          this.errorMensaje = 'El pasaporte debe tener entre 6 y 12 caracteres alfanuméricos';
          return false;
        }
        break;
      case 'Carné de extranjería':
        if (!/^\d{9}$/.test(valorDoc)) {
          this.errorMensaje = 'El carné de extranjería debe tener 9 dígitos';
          return false;
        }
        break;
    }

    if (!this.registroData.telefono || !/^\d{9}$/.test(this.registroData.telefono)) {
      this.errorMensaje = 'El teléfono debe tener 9 dígitos';
      return false;
    }

    if (!this.validarFechaNacimiento()) {
      return false;
    }

    if (!this.registroData.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.registroData.email)) {
      this.errorMensaje = 'Ingresa un email válido';
      return false;
    }

    if (!this.registroData.password || this.registroData.password.length < 6) {
      this.errorMensaje = 'La contraseña debe tener al menos 6 caracteres';
      return false;
    }

    const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;
    if (!passwordPattern.test(this.registroData.password)) {
      this.errorMensaje = 'La contraseña debe contener mayúscula, minúscula y número';
      return false;
    }

    if (!this.registroData.aceptaDatos) {
      this.errorMensaje = 'Debes aceptar los términos y condiciones';
      return false;
    }

    return true;
  }

  private validarFechaNacimiento(): boolean {
    const valor = this.registroData.fechaNacimiento;

    if (!valor) {
      this.errorMensaje = 'La fecha de nacimiento es obligatoria';
      return false;
    }

    const fechaNac = new Date(valor);
    const hoy = new Date();

    if (isNaN(fechaNac.getTime())) {
      this.errorMensaje = 'La fecha ingresada no es válida';
      return false;
    }

    if (fechaNac > hoy) {
      this.errorMensaje = 'La fecha no puede ser futura';
      return false;
    }

    let edad = hoy.getFullYear() - fechaNac.getFullYear();
    const mes = hoy.getMonth() - fechaNac.getMonth();

    if (mes < 0 || (mes === 0 && hoy.getDate() < fechaNac.getDate())) {
      edad--;
    }

    if (edad < 18) {
      this.errorMensaje = 'Debes ser mayor de edad (18 años o más)';
      return false;
    }

    return true;
  }

  // Envío del formulario
  registrar(): void {
    if (!this.validarFormularioCompleto()) {
      return;
    }

    this.cargando = true;
    this.errorMensaje = '';

    this.authService.register(this.registroData).subscribe({
      next: (response: any) => {
        console.log('Registro exitoso:', response);
        this.registroExitoso = true;

        setTimeout(() => {
          this.router.navigate(['/login'], { queryParams: { registroExitoso: true } });
        }, 3000);
      },
      error: (error: any) => {
        console.error('Error en registro:', error);
        this.errorMensaje = error.error?.message || 'Error al registrar. Intenta nuevamente';
        this.cargando = false;
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login']);
      }
    });
  }
}
