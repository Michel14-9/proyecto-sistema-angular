// src/app/modules/cajero/cajero/cajero.ts
import { Component, OnInit, ViewEncapsulation, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

interface Pedido {
  id: number;
  numeroPedido: string;
  total: number;
  fecha: string;
  fechaPedido?: string;
  estado: string;
  tipoEntrega: string;
  direccionEntrega: string;
  cliente: any;
  items: any[];
}

@Component({
  selector: 'app-cajero',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './cajero.html',
  styleUrls: ['./cajero.css'],
  encapsulation: ViewEncapsulation.None
})
export class CajeroComponent implements OnInit, OnDestroy {
  private apiUrl = 'http://localhost:8080';

  pedidosPendientes: Pedido[] = [];
  pedidoSeleccionado: Pedido | null = null;
  estadisticas: any = {
    pendientes: 0,
    pagadosHoy: 0,
    ingresosHoy: 0
  };

  // Autenticación
  isAuthenticated: boolean = false;
  username: string = '';

  // Fecha y hora
  fechaActual: string = '';
  horaActual: string = '';

  // Modal
  motivoCancelacion: string = '';

  // Toast
  mensajeToast: string = '';
  tipoToast: string = 'info';
  mostrarToast: boolean = false;

  // Intervalos
  private intervalId: any;
  private recargaId: any;

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

    this.cargarPedidosPendientes();
    this.actualizarHoraYFecha();

    // Actualizar hora cada segundo
    this.intervalId = setInterval(() => {
      this.actualizarHoraYFecha();
    }, 1000);

    // Recargar pedidos cada 30 segundos
    this.recargaId = setInterval(() => {
      this.cargarPedidosPendientes();
    }, 30000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
    if (this.recargaId) {
      clearInterval(this.recargaId);
    }
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

  cargarPedidosPendientes(): void {
    console.log('🔄 Cargando pedidos pendientes...');

    this.http.get(`${this.apiUrl}/cajero/pedidos-pendientes`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Pedidos cargados:', response);
        if (Array.isArray(response)) {
          this.pedidosPendientes = response;
        } else if (response && response.success) {
          this.pedidosPendientes = response.data || [];
        } else {
          this.pedidosPendientes = [];
        }
        this.estadisticas.pendientes = this.pedidosPendientes.length;
        this.cargarMetricas();
      },
      error: (error) => {
        console.error('❌ Error cargando pedidos:', error);
        if (error.status === 401 || error.status === 403) {
          this.mostrarToastError('Sesión expirada. Redirigiendo al login...');
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 2000);
        } else {
          this.mostrarToastError('Error al cargar pedidos pendientes');
        }
      }
    });
  }

  cargarMetricas(): void {
    console.log('📊 Cargando métricas...');

    this.http.get(`${this.apiUrl}/cajero/metricas-hoy`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📊 Métricas cargadas:', response);
        if (response && response.success) {
          this.estadisticas = {
            pendientes: this.pedidosPendientes.length,
            pagadosHoy: response.totalPedidosPagadosHoy || 0,
            ingresosHoy: response.ingresosHoy || 0
          };
        }
      },
      error: (error) => {
        console.error('❌ Error cargando métricas:', error);
        // Calcular localmente
        this.estadisticas.pendientes = this.pedidosPendientes.length;
      }
    });
  }

  mostrarDetallePedido(pedido: Pedido): void {
    this.pedidoSeleccionado = pedido;
  }

  ocultarDetalle(): void {
    this.pedidoSeleccionado = null;
    this.motivoCancelacion = '';
  }

  isEstadoActivo(estado: string): boolean {
    if (!this.pedidoSeleccionado) return false;
    const estados = ['PENDIENTE', 'PAGADO', 'PREPARACION', 'ENTREGADO'];
    const actualIndex = estados.indexOf(this.pedidoSeleccionado.estado);
    const compararIndex = estados.indexOf(estado);
    return actualIndex >= compararIndex;
  }

  // === MODAL PAGO ===
  abrirModalPago(): void {
    if (!this.pedidoSeleccionado) return;
    const modal = document.getElementById('modalConfirmarPago');
    if (modal) {
      modal.classList.add('show');
      modal.style.display = 'block';
    }
  }

  cerrarModalPago(): void {
    const modal = document.getElementById('modalConfirmarPago');
    if (modal) {
      modal.classList.remove('show');
      modal.style.display = 'none';
    }
  }

  confirmarPago(): void {
    if (!this.pedidoSeleccionado) return;
    this.cerrarModalPago();
    this.marcarComoPagado(this.pedidoSeleccionado.id);
  }

  marcarComoPagado(pedidoId: number): void {
    console.log(`💳 Marcando pedido ${pedidoId} como PAGADO...`);

    this.http.post(`${this.apiUrl}/cajero/marcar-pagado/${pedidoId}`, {}, {
      headers: this.getHeaders(),
      withCredentials: true,
      responseType: 'text'
    }).subscribe({
      next: (response: any) => {
        console.log('✅ Respuesta:', response);
        this.mostrarToastExito('Pedido marcado como PAGADO exitosamente');
        this.cargarPedidosPendientes();
        this.ocultarDetalle();
      },
      error: (error) => {
        console.error('❌ Error marcando como pagado:', error);
        if (error.status === 401 || error.status === 403) {
          this.mostrarToastError('Sesión expirada. Redirigiendo...');
          setTimeout(() => this.router.navigate(['/login']), 2000);
        } else {
          this.mostrarToastError(error.error || 'Error al marcar como pagado');
        }
      }
    });
  }

  // === MODAL CANCELAR ===
  abrirModalCancelar(): void {
    if (!this.pedidoSeleccionado) return;
    this.motivoCancelacion = '';
    const modal = document.getElementById('modalCancelar');
    if (modal) {
      modal.classList.add('show');
      modal.style.display = 'block';
    }
  }

  cerrarModalCancelar(): void {
    const modal = document.getElementById('modalCancelar');
    if (modal) {
      modal.classList.remove('show');
      modal.style.display = 'none';
    }
    this.motivoCancelacion = '';
  }

  confirmarCancelacion(): void {
    if (!this.pedidoSeleccionado) return;
    this.cerrarModalCancelar();
    this.cancelarPedido(this.pedidoSeleccionado.id);
  }

  cancelarPedido(pedidoId: number): void {
    console.log(`❌ Cancelando pedido ${pedidoId}...`);

    const body = new URLSearchParams();
    if (this.motivoCancelacion) {
      body.set('motivo', this.motivoCancelacion);
    }

    this.http.post(`${this.apiUrl}/cajero/marcar-cancelado/${pedidoId}`, body.toString(), {
      headers: {
        'X-CSRF-TOKEN': this.getCsrfToken() || '',
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      withCredentials: true,
      responseType: 'text'
    }).subscribe({
      next: (response: any) => {
        console.log('✅ Respuesta:', response);
        this.mostrarToastExito('Pedido cancelado exitosamente');
        this.cargarPedidosPendientes();
        this.ocultarDetalle();
      },
      error: (error) => {
        console.error('❌ Error cancelando pedido:', error);
        if (error.status === 401 || error.status === 403) {
          this.mostrarToastError('Sesión expirada. Redirigiendo...');
          setTimeout(() => this.router.navigate(['/login']), 2000);
        } else {
          this.mostrarToastError(error.error || 'Error al cancelar el pedido');
        }
      }
    });
  }

  // === UTILIDADES ===
  obtenerNombreCliente(pedido: Pedido): string {
    if (pedido.cliente) {
      return `${pedido.cliente.nombres || ''} ${pedido.cliente.apellidos || ''}`.trim() || 'Cliente no especificado';
    }
    return 'Cliente no especificado';
  }

  formatearFecha(fechaString: string): string {
    if (!fechaString) return 'Fecha no disponible';
    try {
      const fecha = new Date(fechaString);
      return fecha.toLocaleDateString('es-ES', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return 'Fecha inválida';
    }
  }

  actualizarHoraYFecha(): void {
    const ahora = new Date();
    this.fechaActual = ahora.toLocaleDateString('es-ES', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
    this.horaActual = ahora.toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }

  // === TOAST ===
  mostrarToastExito(mensaje: string): void {
    this.mensajeToast = mensaje;
    this.tipoToast = 'success';
    this.mostrarToast = true;
    setTimeout(() => this.cerrarToast(), 5000);
  }

  mostrarToastError(mensaje: string): void {
    this.mensajeToast = mensaje;
    this.tipoToast = 'error';
    this.mostrarToast = true;
    setTimeout(() => this.cerrarToast(), 5000);
  }

  mostrarToastInfo(mensaje: string): void {
    this.mensajeToast = mensaje;
    this.tipoToast = 'info';
    this.mostrarToast = true;
    setTimeout(() => this.cerrarToast(), 5000);
  }

  cerrarToast(): void {
    this.mostrarToast = false;
    this.mensajeToast = '';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
