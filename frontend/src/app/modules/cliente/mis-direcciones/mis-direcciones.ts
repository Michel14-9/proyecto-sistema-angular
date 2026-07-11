// src/app/modules/cliente/mis-direcciones/mis-direcciones.ts
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

interface Direccion {
  id: number;
  nombre: string;
  tipo: string;
  direccion: string;
  referencia: string;
  ciudad: string;
  telefono: string;
  predeterminada: boolean;
  facturacion: boolean;
}

@Component({
  selector: 'app-mis-direcciones',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './mis-direcciones.html',
  styleUrls: ['./mis-direcciones.css'],
  encapsulation: ViewEncapsulation.None
})
export class MisDireccionesComponent implements OnInit {
  private apiUrl = 'http://localhost:8080';

  direcciones: Direccion[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';
  isAuthenticated: boolean = false;
  username: string = '';
  telefonoUsuario: string = '';

  // Modal
  mostrarModal: boolean = false;
  modalTitulo: string = 'Agregar Nueva Dirección';
  editandoIndex: number | null = null;
  guardando: boolean = false;

  // Formulario
  direccionFormData: Direccion = {
    id: 0,
    nombre: '',
    tipo: '',
    direccion: '',
    referencia: '',
    ciudad: 'Ica',
    telefono: '',
    predeterminada: false,
    facturacion: false
  };

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

    this.cargarDirecciones();
    this.cargarTelefonoUsuario();
  }

  private getCsrfToken(): string | null {
    const cookies = document.cookie.split(';');
    for (const cookie of cookies) {
      const [name, value] = cookie.trim().split('=');
      if (name === 'XSRF-TOKEN') {
        return decodeURIComponent(value);
      }
    }
    return null;
  }

  private getHeaders(): HttpHeaders {
    const csrfToken = this.getCsrfToken();
    let headers = new HttpHeaders();
    if (csrfToken) {
      headers = headers.set('X-XSRF-TOKEN', csrfToken);
    }
    headers = headers.set('Content-Type', 'application/json');
    return headers;
  }

  cargarDirecciones(): void {
    this.isLoading = true;
    this.errorMessage = '';

    console.log(' Cargando direcciones...');

    this.http.get(`${this.apiUrl}/api/direcciones`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log(' Respuesta del servidor:', response);
        this.isLoading = false;

        if (Array.isArray(response)) {
          this.direcciones = response;
          console.log(` ${this.direcciones.length} direcciones cargadas`);
        } else if (response && response.success === true) {
          this.direcciones = response.data || [];
          console.log(` ${this.direcciones.length} direcciones cargadas`);
        } else {
          this.direcciones = [];
          this.errorMessage = response?.message || 'Error al cargar las direcciones';
        }
      },
      error: (error) => {
        console.error(' Error cargando direcciones:', error);
        this.isLoading = false;

        if (error.status === 401 || error.status === 403) {
          this.errorMessage = '️ Debes iniciar sesión para ver tus direcciones';
          this.isAuthenticated = false;
        } else {
          this.errorMessage = error.error?.message || 'Error al cargar las direcciones';
        }
        this.direcciones = [];
      }
    });
  }

  cargarTelefonoUsuario(): void {
    this.http.get(`${this.apiUrl}/api/auth/datos-usuario`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        if (response && response.telefono) {
          this.telefonoUsuario = response.telefono;
          this.direccionFormData.telefono = response.telefono;
        }
      },
      error: (error) => {
        console.error(' Error cargando teléfono:', error);
      }
    });
  }

  abrirModalNuevaDireccion(): void {
    console.log('Abriendo modal para NUEVA dirección');
    this.editandoIndex = null;
    this.modalTitulo = 'Agregar Nueva Dirección';
    this.direccionFormData = {
      id: 0,
      nombre: '',
      tipo: '',
      direccion: '',
      referencia: '',
      ciudad: 'Ica',
      telefono: this.telefonoUsuario || '',
      predeterminada: false,
      facturacion: false
    };
    this.mostrarModal = true;
  }

  editarDireccion(index: number): void {
    console.log('️ Editando dirección:', index);
    const direccion = this.direcciones[index];
    if (!direccion) {
      this.mostrarError('No se encontró la dirección para editar');
      return;
    }

    this.editandoIndex = index;
    this.modalTitulo = 'Editar Dirección';
    this.direccionFormData = { ...direccion };
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.editandoIndex = null;
    this.guardando = false;
  }

  guardarDireccion(): void {
    // Validaciones
    if (!this.direccionFormData.nombre.trim()) {
      this.mostrarError('El campo "Nombre de la dirección" es obligatorio');
      return;
    }
    if (!this.direccionFormData.tipo) {
      this.mostrarError('El campo "Tipo de dirección" es obligatorio');
      return;
    }
    if (!this.direccionFormData.direccion.trim()) {
      this.mostrarError('El campo "Dirección completa" es obligatorio');
      return;
    }
    if (!this.direccionFormData.ciudad) {
      this.mostrarError('El campo "Ciudad" es obligatorio');
      return;
    }

    this.guardando = true;
    const data = { ...this.direccionFormData };

    let url: string;
    let method: string;

    if (this.editandoIndex !== null && this.direcciones[this.editandoIndex]) {
      // Modo edición
      url = `${this.apiUrl}/api/direcciones/${this.direcciones[this.editandoIndex].id}`;
      method = 'PUT';
      console.log(' Editando dirección existente:', url);
    } else {
      // Modo nuevo
      url = `${this.apiUrl}/api/direcciones`;
      method = 'POST';
      console.log(' Creando nueva dirección:', url);
    }

    this.http.request(method, url, {
      body: data,
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('Dirección guardada correctamente:', response);
        this.guardando = false;
        this.cerrarModal();
        this.mostrarExito('Dirección guardada correctamente');
        this.cargarDirecciones();
      },
      error: (error) => {
        console.error(' Error guardando dirección:', error);
        this.guardando = false;
        this.mostrarError(error.error?.message || 'Error al guardar la dirección');
      }
    });
  }

  eliminarDireccion(index: number): void {
    const direccion = this.direcciones[index];
    if (!direccion) {
      this.mostrarError('No se encontró la dirección para eliminar');
      return;
    }

    if (!confirm(`¿Estás seguro de ELIMINAR la dirección "${direccion.nombre}"?`)) {
      return;
    }

    console.log('🗑 Eliminando dirección:', direccion);

    this.http.delete(`${this.apiUrl}/api/direcciones/${direccion.id}`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log(' Dirección eliminada correctamente');
        this.mostrarExito('Dirección eliminada correctamente');
        this.cargarDirecciones();
      },
      error: (error) => {
        console.error(' Error eliminando dirección:', error);
        this.mostrarError(error.error?.message || 'Error al eliminar la dirección');
      }
    });
  }

  marcarPredeterminada(index: number): void {
    const direccion = this.direcciones[index];
    if (!direccion) {
      this.mostrarError('No se encontró la dirección para marcar como principal');
      return;
    }

    console.log(' Marcando como principal:', direccion);

    this.http.put(`${this.apiUrl}/api/direcciones/${direccion.id}/predeterminada`, {}, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log(' Dirección marcada como principal');
        this.mostrarExito('Dirección marcada como principal');
        this.cargarDirecciones();
      },
      error: (error) => {
        console.error(' Error marcando como principal:', error);
        this.mostrarError(error.error?.message || 'Error al marcar como principal');
      }
    });
  }

  mostrarExito(mensaje: string): void {

    alert(mensaje);
  }

  mostrarError(mensaje: string): void {

    alert(mensaje);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
