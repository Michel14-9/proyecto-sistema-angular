// src/app/modules/delivery/delivery/delivery.ts
import { Component, OnInit, ViewEncapsulation, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { LayoutService } from '../../../core/services/layout.service'; // ✅ Importar LayoutService

interface Pedido {
  id: number;
  numeroPedido: string;
  total: number;
  fecha: string;
  estado: string;
  tipoEntrega: string;
  direccionEntrega: string;
  referenciaDireccion?: string;
  observaciones: string;
  cliente: any;
  items: any[];
}

@Component({
  selector: 'app-delivery',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './delivery.html',
  styleUrls: ['./delivery.css'],
  encapsulation: ViewEncapsulation.None
})
export class DeliveryComponent implements OnInit, OnDestroy {
  private apiUrl = 'http://localhost:8080';

  pedidosPendientesEntrega: Pedido[] = [];
  pedidosEnCamino: Pedido[] = [];
  pedidoSeleccionado: Pedido | null = null;
  estadoSeleccionado: string = '';

  estadisticas: any = {
    pendientesEntrega: 0,
    enCamino: 0,
    entregadosHoy: 0
  };

  // Autenticación
  isAuthenticated: boolean = false;
  username: string = '';

  // Fecha y hora
  fechaActual: string = '';
  horaActual: string = '';

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
    private router: Router,
    private layoutService: LayoutService // ✅ Inyectar LayoutService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    if (!this.isAuthenticated) {
      this.router.navigate(['/login']);
      return;
    }

    // ✅ Ocultar header y footer global
    this.layoutService.hideHeaderAndFooter();

    this.cargarPedidosDelivery();
    this.actualizarHoraYFecha();

    // ✅ Actualizar hora cada segundo
    this.intervalId = setInterval(() => {
      this.actualizarHoraYFecha();
    }, 1000);

    // Recargar pedidos cada 30 segundos
    this.recargaId = setInterval(() => {
      this.cargarPedidosDelivery();
    }, 30000);
  }

  ngOnDestroy(): void {
    // ✅ Restaurar header y footer global
    this.layoutService.showHeaderAndFooter();

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

  cargarPedidosDelivery(): void {
    console.log('🔄 Cargando pedidos para delivery...');

    // Cargar pedidos pendientes de entrega (LISTO)
    this.http.get(`${this.apiUrl}/delivery/pedidos-para-entrega`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        this.pedidosPendientesEntrega = Array.isArray(response) ? response : [];
        console.log(`📦 Pendientes entrega: ${this.pedidosPendientesEntrega.length}`);
        this.estadisticas.pendientesEntrega = this.pedidosPendientesEntrega.length;
      },
      error: (error) => {
        console.error('❌ Error cargando pedidos pendientes de entrega:', error);
      }
    });

    // Cargar pedidos en camino (EN_CAMINO)
    this.http.get(`${this.apiUrl}/delivery/pedidos-en-camino`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        this.pedidosEnCamino = Array.isArray(response) ? response : [];
        console.log(`📦 En camino: ${this.pedidosEnCamino.length}`);
        this.estadisticas.enCamino = this.pedidosEnCamino.length;
      },
      error: (error) => {
        console.error('❌ Error cargando pedidos en camino:', error);
      }
    });

    // Cargar métricas
    this.cargarMetricasDelivery();
  }

  cargarMetricasDelivery(): void {
    this.http.get(`${this.apiUrl}/delivery/metricas-delivery`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📊 Métricas delivery:', response);
        if (response && response.success) {
          this.estadisticas.entregadosHoy = response.totalEntregadosHoy || 0;
        }
      },
      error: (error) => {
        console.error('❌ Error cargando métricas delivery:', error);
      }
    });
  }

  mostrarDetallePedido(pedido: Pedido, estado: string): void {
    this.pedidoSeleccionado = pedido;
    this.estadoSeleccionado = estado;
  }

  ocultarDetalle(): void {
    this.pedidoSeleccionado = null;
    this.estadoSeleccionado = '';
  }

  // === MODAL INICIAR ENTREGA ===
  abrirModalIniciarEntrega(): void {
    if (!this.pedidoSeleccionado) return;
    const modal = document.getElementById('modalIniciarEntrega');
    if (modal) {
      modal.classList.add('show');
      modal.style.display = 'block';
    }
  }

  cerrarModalIniciarEntrega(): void {
    const modal = document.getElementById('modalIniciarEntrega');
    if (modal) {
      modal.classList.remove('show');
      modal.style.display = 'none';
    }
  }

  confirmarIniciarEntrega(): void {
    if (!this.pedidoSeleccionado) return;
    this.cerrarModalIniciarEntrega();
    this.iniciarEntrega(this.pedidoSeleccionado.id);
  }

  iniciarEntrega(pedidoId: number): void {
    console.log(`🚚 Iniciando entrega del pedido ${pedidoId}...`);

    this.http.post(`${this.apiUrl}/delivery/iniciar-entrega/${pedidoId}`, {}, {
      headers: this.getHeaders(),
      withCredentials: true,
      responseType: 'text'
    }).subscribe({
      next: (response: any) => {
        console.log('✅ Respuesta:', response);
        this.mostrarToastExito('Entrega iniciada correctamente');
        this.cargarPedidosDelivery();
        this.ocultarDetalle();
      },
      error: (error) => {
        console.error('❌ Error iniciando entrega:', error);
        if (error.status === 401 || error.status === 403) {
          this.mostrarToastError('Sesión expirada. Redirigiendo...');
          setTimeout(() => this.router.navigate(['/login']), 2000);
        } else {
          this.mostrarToastError(error.error || 'Error al iniciar entrega');
        }
      }
    });
  }

  // === MODAL MARCAR ENTREGADO ===
  abrirModalMarcarEntregado(): void {
    if (!this.pedidoSeleccionado) return;
    const modal = document.getElementById('modalMarcarEntregado');
    if (modal) {
      modal.classList.add('show');
      modal.style.display = 'block';
    }
  }

  cerrarModalMarcarEntregado(): void {
    const modal = document.getElementById('modalMarcarEntregado');
    if (modal) {
      modal.classList.remove('show');
      modal.style.display = 'none';
    }
  }

  confirmarMarcarEntregado(): void {
    if (!this.pedidoSeleccionado) return;
    this.cerrarModalMarcarEntregado();
    this.marcarComoEntregado(this.pedidoSeleccionado.id);
  }

  marcarComoEntregado(pedidoId: number): void {
    console.log(`✅ Marcando pedido ${pedidoId} como ENTREGADO...`);

    this.http.post(`${this.apiUrl}/delivery/marcar-entregado/${pedidoId}`, {}, {
      headers: this.getHeaders(),
      withCredentials: true,
      responseType: 'text'
    }).subscribe({
      next: (response: any) => {
        console.log('✅ Respuesta:', response);
        this.mostrarToastExito('Pedido marcado como ENTREGADO correctamente');
        this.cargarPedidosDelivery();
        this.ocultarDetalle();
      },
      error: (error) => {
        console.error('❌ Error marcando como entregado:', error);
        if (error.status === 401 || error.status === 403) {
          this.mostrarToastError('Sesión expirada. Redirigiendo...');
          setTimeout(() => this.router.navigate(['/login']), 2000);
        } else {
          this.mostrarToastError(error.error || 'Error al marcar como entregado');
        }
      }
    });
  }

  // === UTILIDADES ===
  obtenerNombreCliente(pedido: Pedido): string {
    if (pedido.cliente && typeof pedido.cliente === 'object') {
      const nombres = pedido.cliente.nombres || '';
      const apellidos = pedido.cliente.apellidos || '';
      return `${nombres} ${apellidos}`.trim() || 'Cliente no especificado';
    } else if (pedido.cliente && typeof pedido.cliente === 'string') {
      return pedido.cliente;
    }
    return 'Cliente no especificado';
  }

  obtenerTelefonoCliente(pedido: Pedido): string {
    if (pedido.cliente && typeof pedido.cliente === 'object') {
      return pedido.cliente.telefono || 'No especificado';
    }
    return 'No especificado';
  }

  getTiempoTranscurrido(fechaString: string): { texto: string; minutosTotales: number } {
    if (!fechaString) return { texto: 'N/A', minutosTotales: 0 };

    try {
      const fechaPedido = new Date(fechaString);
      if (isNaN(fechaPedido.getTime())) {
        return { texto: 'N/A', minutosTotales: 0 };
      }

      const ahora = new Date();
      const diferenciaMs = ahora.getTime() - fechaPedido.getTime();
      const minutosTotales = Math.floor(diferenciaMs / (1000 * 60));

      if (minutosTotales > 360) {
        return { texto: 'Revisar', minutosTotales };
      }

      if (minutosTotales < 60) {
        return { texto: `${minutosTotales}min`, minutosTotales };
      } else {
        const horas = Math.floor(minutosTotales / 60);
        const minutos = minutosTotales % 60;
        return { texto: `${horas}h ${minutos}m`, minutosTotales };
      }
    } catch (error) {
      return { texto: 'N/A', minutosTotales: 0 };
    }
  }

  formatearFecha(fechaString: string): string {
    if (!fechaString) return 'Fecha no disponible';
    try {
      const fecha = new Date(fechaString);
      if (isNaN(fecha.getTime())) return 'Fecha inválida';
      return fecha.toLocaleDateString('es-PE', {
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

  formatearFechaCorta(fechaString: string): string {
    if (!fechaString) return '';
    try {
      const fecha = new Date(fechaString);
      if (isNaN(fecha.getTime())) return '';
      return fecha.toLocaleTimeString('es-PE', {
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return '';
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

  // === LOGOUT ===
  cerrarSesion(): void {
    if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
      this.authService.logout().subscribe({
        next: () => {
          this.router.navigate(['/login']);
        },
        error: () => {
          this.router.navigate(['/login']);
        }
      });
    }
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

  cerrarToast(): void {
    this.mostrarToast = false;
    this.mensajeToast = '';
  }
}
