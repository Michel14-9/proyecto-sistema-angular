// src/app/modules/cliente/mis-datos/mis-datos.ts
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

interface Usuario {
  nombre: string;
  apellidos: string;
  email: string;
  tipoDocumento: string;
  numeroDocumento: string;
  telefono: string;
  fechaNacimiento: string;
}

interface FormData {
  nombre: string;
  apellidos: string;
  tipoDocumento: string;
  numeroDocumento: string;
  telefono: string;
  fechaNacimiento: string;
  passwordActual: string;
  nuevaPassword: string;
}

@Component({
  selector: 'app-mis-datos',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './mis-datos.html',
  styleUrls: ['./mis-datos.css'],
  encapsulation: ViewEncapsulation.None
})
export class MisDatosComponent implements OnInit {
  private apiUrl = 'http://localhost:8080';

  usuario: Usuario = {
    nombre: '',
    apellidos: '',
    email: '',
    tipoDocumento: '',
    numeroDocumento: '',
    telefono: '',
    fechaNacimiento: ''
  };

  formData: FormData = {
    nombre: '',
    apellidos: '',
    tipoDocumento: '',
    numeroDocumento: '',
    telefono: '',
    fechaNacimiento: '',
    passwordActual: '',
    nuevaPassword: ''
  };

  isLoading: boolean = true;
  errorMessage: string = '';
  isAuthenticated: boolean = false;
  username: string = '';
  mostrarFormulario: boolean = false;
  guardando: boolean = false;

  // Password strength
  passwordStrengthMessage: string = '';
  passwordStrengthColor: string = '';

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    if (!this.isAuthenticated) {
      this.router.navigate(['/login']);
      return;
    }

    this.cargarDatosUsuario();
  }

  private getHeaders(): HttpHeaders {
    let headers = new HttpHeaders();
    headers = headers.set('Content-Type', 'application/json');
    return headers;
  }

  cargarDatosUsuario(): void {
    this.isLoading = true;
    this.errorMessage = '';

    console.log('🔄 Cargando datos del usuario...');

    this.http.get(`${this.apiUrl}/api/auth/datos-usuario`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Datos del usuario:', response);
        this.isLoading = false;

        if (response) {
          this.usuario = {
            nombre: response.nombre || '',
            apellidos: response.apellidos || '',
            email: response.email || '',
            tipoDocumento: response.tipoDocumento || '',
            numeroDocumento: response.numeroDocumento || '',
            telefono: response.telefono || '',
            fechaNacimiento: response.fechaNacimiento || ''
          };
        }
      },
      error: (error) => {
        console.error('❌ Error cargando datos:', error);
        this.isLoading = false;

        if (error.status === 401 || error.status === 403) {
          this.errorMessage = '⚠️ Debes iniciar sesión para ver tus datos';
          this.isAuthenticated = false;
        } else {
          this.errorMessage = error.error?.message || 'Error al cargar los datos del usuario';
        }
      }
    });
  }

  habilitarEdicion(): void {
    console.log('✏️ Habilitando modo edición...');

    // Copiar datos al formulario
    this.formData = {
      nombre: this.usuario.nombre,
      apellidos: this.usuario.apellidos,
      tipoDocumento: this.usuario.tipoDocumento,
      numeroDocumento: this.usuario.numeroDocumento,
      telefono: this.usuario.telefono,
      fechaNacimiento: this.usuario.fechaNacimiento,
      passwordActual: '',
      nuevaPassword: ''
    };

    this.mostrarFormulario = true;
    this.passwordStrengthMessage = '';
    this.passwordStrengthColor = '';
  }

  cancelarEdicion(): void {
    console.log('❌ Cancelando edición...');
    this.mostrarFormulario = false;
    this.guardando = false;
    this.errorMessage = '';
    this.passwordStrengthMessage = '';
    this.passwordStrengthColor = '';
  }

  guardarCambios(): void {
    // Validaciones
    if (!this.formData.nombre.trim()) {
      this.errorMessage = 'El nombre es obligatorio';
      return;
    }
    if (!this.formData.apellidos.trim()) {
      this.errorMessage = 'Los apellidos son obligatorios';
      return;
    }
    if (!this.formData.tipoDocumento) {
      this.errorMessage = 'El tipo de documento es obligatorio';
      return;
    }
    if (!this.formData.numeroDocumento.trim()) {
      this.errorMessage = 'El número de documento es obligatorio';
      return;
    }
    if (!this.formData.telefono.trim()) {
      this.errorMessage = 'El teléfono es obligatorio';
      return;
    }
    if (!this.formData.fechaNacimiento) {
      this.errorMessage = 'La fecha de nacimiento es obligatoria';
      return;
    }

    // Validar fecha de nacimiento (mayor de 18 años)
    const fechaNac = new Date(this.formData.fechaNacimiento);
    const hoy = new Date();
    let edad = hoy.getFullYear() - fechaNac.getFullYear();
    const mes = hoy.getMonth() - fechaNac.getMonth();
    if (mes < 0 || (mes === 0 && hoy.getDate() < fechaNac.getDate())) {
      edad--;
    }
    if (edad < 18) {
      this.errorMessage = 'Debes ser mayor de edad (18 años o más)';
      return;
    }

    // Validar contraseña
    if (this.formData.nuevaPassword && !this.formData.passwordActual) {
      this.errorMessage = 'Debes ingresar tu contraseña actual para cambiarla';
      return;
    }

    if (this.formData.nuevaPassword && this.formData.nuevaPassword.length < 6) {
      this.errorMessage = 'La nueva contraseña debe tener al menos 6 caracteres';
      return;
    }

    this.guardando = true;
    this.errorMessage = '';

    const datosActualizados: any = {
      nombre: this.formData.nombre.trim(),
      apellidos: this.formData.apellidos.trim(),
      tipoDocumento: this.formData.tipoDocumento,
      numeroDocumento: this.formData.numeroDocumento.trim(),
      telefono: this.formData.telefono.trim(),
      fechaNacimiento: this.formData.fechaNacimiento
    };

    // Solo incluir campos de contraseña si se están cambiando
    if (this.formData.nuevaPassword) {
      datosActualizados.passwordActual = this.formData.passwordActual;
      datosActualizados.nuevaPassword = this.formData.nuevaPassword;
    }

    console.log('📤 Enviando datos actualizados:', datosActualizados);

    this.http.put(`${this.apiUrl}/api/auth/actualizar-datos`, datosActualizados, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('✅ Datos actualizados correctamente:', response);
        this.guardando = false;
        this.mostrarExito('Tus datos se han actualizado correctamente.');

        // Recargar datos
        setTimeout(() => {
          this.cargarDatosUsuario();
          this.cancelarEdicion();
        }, 1000);
      },
      error: (error) => {
        console.error('❌ Error actualizando datos:', error);
        this.guardando = false;
        this.errorMessage = error.error?.message || 'Error al actualizar los datos';
      }
    });
  }

  validarFortalezaPassword(password: string): void {
    if (!password) {
      this.passwordStrengthMessage = '';
      this.passwordStrengthColor = '';
      return;
    }

    let strength = '';
    let color = '';
    let message = '';

    if (password.length < 6) {
      strength = 'Débil';
      color = '#dc3545';
      message = 'Muy corta (mínimo 6 caracteres)';
    } else if (password.length < 8) {
      strength = 'Media';
      color = '#fd7e14';
      message = 'Podría ser más segura';
    } else if (/[A-Z]/.test(password) && /\d/.test(password)) {
      strength = 'Fuerte';
      color = '#198754';
      message = 'Excelente seguridad';
    } else {
      strength = 'Media';
      color = '#fd7e14';
      message = 'Usa mayúsculas, minúsculas y números';
    }

    this.passwordStrengthMessage = `Seguridad: ${strength} - ${message}`;
    this.passwordStrengthColor = color;
  }

  formatearFecha(fechaString: string): string {
    if (!fechaString) return '-';
    try {
      const fecha = new Date(fechaString);
      return fecha.toLocaleDateString('es-ES', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch {
      return '-';
    }
  }

  mostrarExito(mensaje: string): void {
    // Crear alerta temporal
    const alerta = document.createElement('div');
    alerta.className = 'alert alert-success alert-dismissible fade show';
    alerta.innerHTML = `
      <i class="fas fa-check-circle me-2"></i>
      ${mensaje}
      <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    const contenido = document.querySelector('.col-md-9');
    if (contenido) {
      contenido.insertBefore(alerta, contenido.firstChild);
    }

    setTimeout(() => {
      if (alerta.parentNode) {
        alerta.remove();
      }
    }, 5000);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
