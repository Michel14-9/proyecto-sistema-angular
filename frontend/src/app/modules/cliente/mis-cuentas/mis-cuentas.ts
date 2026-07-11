// src/app/modules/cliente/mis-cuentas/mis-cuentas.ts

import { Component, OnInit, ViewEncapsulation, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
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
  selector: 'app-mis-cuentas',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './mis-cuentas.html',
  styleUrls: ['./mis-cuentas.css'],
  encapsulation: ViewEncapsulation.None
})
export class MisCuentasComponent implements OnInit, OnDestroy {
  private apiUrl = 'http://localhost:8080';
  private intervalId: any;

  usuario: Usuario = {
    nombre: '',
    apellidos: '',
    email: '',
    tipoDocumento: '',
    numeroDocumento: '',
    telefono: '',
    fechaNacimiento: ''
  };

  direcciones: Direccion[] = [];
  direccionFacturacion: Direccion | null = null;
  direccionEnvio: Direccion | null = null;

  isLoading: boolean = true;
  errorMessage: string = '';
  isAuthenticated: boolean = false;
  username: string = '';

  totalPedidos: number = 0;
  totalFavoritos: number = 0;

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

    this.cargarTodosLosDatos();

    // Actualizar cada 2 minutos
    this.intervalId = setInterval(() => {
      if (!document.hidden && navigator.onLine) {
        console.log(' Actualización automática de datos...');
        this.cargarTodosLosDatos();
      }
    }, 120000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  private getHeaders(): HttpHeaders {
    let headers = new HttpHeaders();
    headers = headers.set('Content-Type', 'application/json');
    return headers;
  }

  cargarTodosLosDatos(): void {
    this.cargarDatosUsuario();
    this.cargarEstadisticas();
    this.cargarDirecciones();
  }


  cargarDatosUsuario(): void {
    console.log(' Cargando datos del usuario...');


    this.http.get(`${this.apiUrl}/api/usuarios/perfil`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log(' Datos del usuario recibidos:', response);
        this.isLoading = false;


        if (response && response.success && response.usuario) {
          const data = response.usuario;
          this.usuario = {
            nombre: data.nombres || '',
            apellidos: data.apellidos || '',
            email: data.email || '',
            tipoDocumento: data.tipoDocumento || '',
            numeroDocumento: data.numeroDocumento || '',
            telefono: data.telefono || '',
            fechaNacimiento: data.fechaNacimiento || ''
          };
        } else {
          this.errorMessage = 'Error al cargar los datos del usuario';
        }
      },
      error: (error) => {
        console.error(' Error cargando datos del usuario:', error);
        this.isLoading = false;

        if (error.status === 401 || error.status === 403) {
          this.errorMessage = ' Debes iniciar sesión para ver tu información';
          this.isAuthenticated = false;
        } else {
          this.errorMessage = error.error?.message || 'Error al cargar los datos del usuario';
        }
      }
    });
  }

  cargarEstadisticas(): void {
    console.log(' Cargando estadísticas...');

    // Cargar contador de favoritos
    this.http.get(`${this.apiUrl}/api/favoritos/count`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        if (response && response.success) {
          this.totalFavoritos = response.count || 0;
        }
      },
      error: (error) => {
        console.error(' Error cargando favoritos:', error);
        this.totalFavoritos = 0;
      }
    });

    // Cargar contador de pedidos
    this.http.get(`${this.apiUrl}/api/pedidos/mis-pedidos`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        if (Array.isArray(response)) {
          this.totalPedidos = response.length;
        } else if (response && response.success) {
          this.totalPedidos = response.data?.length || 0;
        } else {
          this.totalPedidos = 0;
        }
      },
      error: (error) => {
        console.error(' Error cargando pedidos:', error);
        this.totalPedidos = 0;
      }
    });
  }

  cargarDirecciones(): void {
    console.log(' Cargando direcciones...');

    this.http.get(`${this.apiUrl}/api/direcciones`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log(' Direcciones recibidas:', response);

        if (Array.isArray(response)) {
          this.direcciones = response;
        } else if (response && response.success) {
          this.direcciones = response.data || [];
        } else {
          this.direcciones = [];
        }

        // Encontrar dirección de facturación y envío
        this.direccionFacturacion = this.direcciones.find(dir => dir.facturacion) ||
                                   this.direcciones.find(dir => dir.predeterminada) ||
                                   null;

        this.direccionEnvio = this.direcciones.find(dir => dir.predeterminada) ||
                            (this.direcciones.length > 0 ? this.direcciones[0] : null);

        console.log(' Dirección facturación:', this.direccionFacturacion);
        console.log(' Dirección envío:', this.direccionEnvio);
      },
      error: (error) => {
        console.error(' Error cargando direcciones:', error);
        this.direcciones = [];
        this.direccionFacturacion = null;
        this.direccionEnvio = null;
      }
    });
  }

  formatearFecha(fechaString: string): string {
    if (!fechaString) return 'No especificada';
    try {
      const fecha = new Date(fechaString);
      if (isNaN(fecha.getTime())) return 'Fecha inválida';
      return fecha.toLocaleDateString('es-ES', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch {
      return 'Fecha inválida';
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
