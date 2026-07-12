import { Component, OnInit, ViewEncapsulation, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { LayoutService } from '../../../core/services/layout.service';

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

interface TrackingInfo {
  id?: number;
  estado: string;
  etaMinutos: number;
  distanciaKm: number;
  urlNavegacion: string;
  nombreRepartidor?: string;
  direccionCliente?: string;
  mensaje: string;
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
  private trackingUrl = 'http://localhost:8082'; // microservicio tracking

  pedidosPendientesEntrega: Pedido[] = [];
  pedidosEnCamino: Pedido[] = [];
  pedidoSeleccionado: Pedido | null = null;
  estadoSeleccionado: string = '';

  // Tracking del pedido seleccionado
  trackingActivo: TrackingInfo | null = null;
  enviandoUbicacion = false;

  estadisticas: any = { pendientesEntrega: 0, enCamino: 0, entregadosHoy: 0 };

  isAuthenticated = false;
  username = '';
  fechaActual = '';
  horaActual = '';

  mensajeToast = '';
  tipoToast = 'info';
  mostrarToast = false;

  private intervalReloj: any;
  private intervalRecarga: any;
  // Intervalo GPS automático (ms) — empieza en 2 min, pasa a 5 min si dist > 3 km
  private intervalGPS: any;
  gpsActivo = false;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private layoutService: LayoutService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    if (!this.isAuthenticated) { this.router.navigate(['/login']); return; }



    this.layoutService.hideHeaderAndFooter();
    this.cargarPedidosDelivery();
    this.actualizarHoraYFecha();

    this.intervalReloj = setInterval(() => this.actualizarHoraYFecha(), 1000);
    this.intervalRecarga = setInterval(() => this.cargarPedidosDelivery(), 30000);
  }

  ngOnDestroy(): void {

    this.layoutService.showHeaderAndFooter();
    this.detenerGPS();
    clearInterval(this.intervalReloj);
    clearInterval(this.intervalRecarga);
  }

  // ─── GPS AUTOMÁTICO ────────────────────────────────────────────────────────

  iniciarEnvioGPS(numeroPedido: string): void {
    if (this.gpsActivo) return;
    if (!navigator.geolocation) {
      this.mostrarToastError('Tu navegador no soporta geolocalización');
      return;
    }
    this.gpsActivo = true;
    this.enviarUbicacionGPS(numeroPedido); // envío inmediato al arrancar
    // Luego cada 2 min (120 000 ms)
    this.intervalGPS = setInterval(() => {
      this.ajustarIntervaloGPS(numeroPedido);
    }, 120000);
    this.mostrarToastExito('GPS activado — enviando ubicación cada 2 min');
  }

  private ajustarIntervaloGPS(numeroPedido: string): void {
    this.enviarUbicacionGPS(numeroPedido);

    // Si la distancia restante es > 3 km, extender intervalo a 5 min
    if (this.trackingActivo && (this.trackingActivo.distanciaKm || 0) > 3) {
      clearInterval(this.intervalGPS);
      this.intervalGPS = setInterval(() => {
        this.ajustarIntervaloGPS(numeroPedido);
      }, 300000); // 5 min
    } else if (this.trackingActivo && (this.trackingActivo.distanciaKm || 0) <= 3) {
      // Cerca del cliente → volver a 2 min
      clearInterval(this.intervalGPS);
      this.intervalGPS = setInterval(() => {
        this.ajustarIntervaloGPS(numeroPedido);
      }, 120000);
    }
  }

  private enviarUbicacionGPS(numeroPedido: string): void {
    this.enviandoUbicacion = true;
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const body = {
          numeroPedido,
          latRepartidor: pos.coords.latitude,
          lngRepartidor: pos.coords.longitude
        };
        this.http.post<TrackingInfo>(`${this.trackingUrl}/api/tracking/ubicacion`, body).subscribe({
          next: (resp) => {
            this.trackingActivo = resp;
            this.enviandoUbicacion = false;
            console.log(`📍 GPS enviado → ${resp.distanciaKm}km | ETA: ${resp.etaMinutos}min`);
          },
          error: () => { this.enviandoUbicacion = false; }
        });
      },
      (err) => {
        this.enviandoUbicacion = false;
        console.warn('GPS error:', err.message);
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
    );
  }

  detenerGPS(): void {
    if (this.intervalGPS) {
      clearInterval(this.intervalGPS);
      this.intervalGPS = null;
    }
    this.gpsActivo = false;
  }

  // ─── NAVEGACIÓN A MAPS (sin API key requerida) ─────────────────────────────

  abrirNavegacionMaps(): void {
    if (!this.pedidoSeleccionado) return;

    let url: string;

    if (this.trackingActivo?.urlNavegacion) {
      // URL provista por el microservicio con coordenadas exactas
      url = this.trackingActivo.urlNavegacion;
    } else if (this.pedidoSeleccionado.direccionEntrega) {
      // Fallback: buscar la dirección textual en Google Maps
      const dir = encodeURIComponent(
        this.pedidoSeleccionado.direccionEntrega + ', Ica, Peru'
      );
      url = `https://www.google.com/maps/dir/?api=1&destination=${dir}&travelmode=driving`;
    } else {
      this.mostrarToastError('No hay dirección disponible para navegar');
      return;
    }

    window.open(url, '_blank');
  }

  // ─── CARGA DE PEDIDOS ─────────────────────────────────────────────────────

  cargarPedidosDelivery(): void {
    console.log(' Cargando pedidos para delivery...');

    // Cargar pedidos pendientes de entrega (LISTO)

    this.http.get(`${this.apiUrl}/delivery/pedidos-para-entrega`, {
      headers: this.getHeaders(), withCredentials: true
    }).subscribe({
      next: (response: any) => {
        this.pedidosPendientesEntrega = Array.isArray(response) ? response : [];
        console.log(` Pendientes entrega: ${this.pedidosPendientesEntrega.length}`);
        this.estadisticas.pendientesEntrega = this.pedidosPendientesEntrega.length;
      },
      error: (error) => {
        console.error(' Error cargando pedidos pendientes de entrega:', error);
      }

    });

    this.http.get(`${this.apiUrl}/delivery/pedidos-en-camino`, {
      headers: this.getHeaders(), withCredentials: true
    }).subscribe({
      next: (response: any) => {
        this.pedidosEnCamino = Array.isArray(response) ? response : [];
        console.log(` En camino: ${this.pedidosEnCamino.length}`);
        this.estadisticas.enCamino = this.pedidosEnCamino.length;
      },
      error: (error) => {
        console.error(' Error cargando pedidos en camino:', error);
      }

    });

    this.cargarMetricasDelivery();
  }

  cargarMetricasDelivery(): void {
    this.http.get(`${this.apiUrl}/delivery/metricas-delivery`, {
      headers: this.getHeaders(), withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log(' Métricas delivery:', response);
        if (response && response.success) {
          this.estadisticas.entregadosHoy = response.totalEntregadosHoy || 0;
        }
      },
      error: (error) => {
        console.error(' Error cargando métricas delivery:', error);
      }

    });
  }

  mostrarDetallePedido(pedido: Pedido, estado: string): void {
    this.pedidoSeleccionado = pedido;
    this.estadoSeleccionado = estado;
    this.trackingActivo = null;

    // Consultar si ya hay tracking activo para este pedido
    this.http.get<TrackingInfo>(
      `${this.trackingUrl}/api/tracking/${pedido.numeroPedido}`
    ).subscribe({
      next: (t) => {
        if (t?.id) this.trackingActivo = t;
      },
      error: () => { }
    });
  }

  ocultarDetalle(): void {
    this.detenerGPS();
    this.pedidoSeleccionado = null;
    this.estadoSeleccionado = '';
    this.trackingActivo = null;
  }

  // ─── INICIAR ENTREGA ──────────────────────────────────────────────────────

  abrirModalIniciarEntrega(): void {
    const modal = document.getElementById('modalIniciarEntrega');
    if (modal) { modal.classList.add('show'); modal.style.display = 'block'; }
  }

  cerrarModalIniciarEntrega(): void {
    const modal = document.getElementById('modalIniciarEntrega');
    if (modal) { modal.classList.remove('show'); modal.style.display = 'none'; }
  }

  confirmarIniciarEntrega(): void {
    if (!this.pedidoSeleccionado) return;
    this.cerrarModalIniciarEntrega();
    this.iniciarEntrega(this.pedidoSeleccionado);
  }

  iniciarEntrega(pedidoId: number): void {
    console.log(` Iniciando entrega del pedido ${pedidoId}...`);

    this.http.post(`${this.apiUrl}/delivery/iniciar-entrega/${pedidoId}`, {}, {
      headers: this.getHeaders(),
      withCredentials: true,
      responseType: 'text'
    }).subscribe({
      next: (response: any) => {
        console.log(' Respuesta:', response);
        this.mostrarToastExito('Entrega iniciada correctamente');

        this.cargarPedidosDelivery();

        // Crear sesión de tracking en el microservicio
        const trackingBody = {
          pedidoId: pedido.id,
          numeroPedido: pedido.numeroPedido,
          direccionCliente: pedido.direccionEntrega,
          nombreRepartidor: this.username
        };
        this.http.post<TrackingInfo>(
          `${this.trackingUrl}/api/tracking/crear`, trackingBody
        ).subscribe({
          next: (t) => {
            this.trackingActivo = t;
            // Iniciar envío automático de GPS
            this.iniciarEnvioGPS(pedido.numeroPedido);
          },
          error: () => console.warn('Tracking no disponible, continúa sin GPS')
        });
      },
      error: (error) => {
        console.error(' Error iniciando entrega:', error);
        if (error.status === 401 || error.status === 403) {
          this.mostrarToastError('Sesión expirada. Redirigiendo...');

          setTimeout(() => this.router.navigate(['/login']), 2000);
        } else {
          this.mostrarToastError(e.error || 'Error al iniciar entrega');
        }
      }
    });
  }

  // ─── MARCAR ENTREGADO ─────────────────────────────────────────────────────

  abrirModalMarcarEntregado(): void {
    const modal = document.getElementById('modalMarcarEntregado');
    if (modal) { modal.classList.add('show'); modal.style.display = 'block'; }
  }

  cerrarModalMarcarEntregado(): void {
    const modal = document.getElementById('modalMarcarEntregado');
    if (modal) { modal.classList.remove('show'); modal.style.display = 'none'; }
  }

  confirmarMarcarEntregado(): void {
    if (!this.pedidoSeleccionado) return;
    this.cerrarModalMarcarEntregado();
    this.marcarComoEntregado(this.pedidoSeleccionado);
  }

  marcarComoEntregado(pedidoId: number): void {
    console.log(` Marcando pedido ${pedidoId} como ENTREGADO...`);

    this.http.post(`${this.apiUrl}/delivery/marcar-entregado/${pedidoId}`, {}, {
      headers: this.getHeaders(),
      withCredentials: true,
      responseType: 'text'
    }).subscribe({
      next: (response: any) => {
        console.log(' Respuesta:', response);
        this.mostrarToastExito('Pedido marcado como ENTREGADO correctamente');
        this.cargarPedidosDelivery();
        this.ocultarDetalle();
      },
      error: (error) => {
        console.error(' Error marcando como entregado:', error);
        if (error.status === 401 || error.status === 403) {
          this.mostrarToastError('Sesión expirada. Redirigiendo...');

          setTimeout(() => this.router.navigate(['/login']), 2000);
        } else {
          this.mostrarToastError(e.error || 'Error al marcar como entregado');
        }
      }
    });
  }

  // ─── UTILIDADES ───────────────────────────────────────────────────────────

  private getCsrfToken(): string | null {
    for (const c of document.cookie.split(';')) {
      const [k, v] = c.trim().split('=');
      if (k === 'XSRF-TOKEN') return decodeURIComponent(v);
    }
    return null;
  }

  private getHeaders(): HttpHeaders {
    let h = new HttpHeaders({ 'Content-Type': 'application/json' });
    const csrf = this.getCsrfToken();
    if (csrf) h = h.set('X-XSRF-TOKEN', csrf);
    return h;
  }

  obtenerNombreCliente(p: Pedido): string {
    if (p.cliente && typeof p.cliente === 'object') {
      return `${p.cliente.nombres || ''} ${p.cliente.apellidos || ''}`.trim() || 'Cliente';
    }
    return typeof p.cliente === 'string' ? p.cliente : 'Cliente';
  }

  obtenerTelefonoCliente(p: Pedido): string {
    return p.cliente?.telefono || 'No especificado';
  }

  getTiempoTranscurrido(f: string): { texto: string; minutosTotales: number } {
    if (!f) return { texto: 'N/A', minutosTotales: 0 };
    const diff = Date.now() - new Date(f).getTime();
    const min = Math.floor(diff / 60000);
    if (min > 360) return { texto: 'Revisar', minutosTotales: min };
    if (min < 60) return { texto: `${min}min`, minutosTotales: min };
    return { texto: `${Math.floor(min / 60)}h ${min % 60}m`, minutosTotales: min };
  }

  formatearFecha(f: string): string {
    if (!f) return 'No disponible';
    try {
      return new Date(f).toLocaleDateString('es-PE', {
        day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
      });
    } catch { return 'Inválida'; }
  }

  formatearFechaCorta(f: string): string {
    if (!f) return '';
    try {
      return new Date(f).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
    } catch { return ''; }
  }

  actualizarHoraYFecha(): void {
    const n = new Date();
    this.fechaActual = n.toLocaleDateString('es-ES', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
    this.horaActual = n.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  cerrarSesion(): void {
    if (confirm('¿Cerrar sesión?')) {
      this.authService.logout().subscribe({
        next: () => this.router.navigate(['/login']),
        error: () => this.router.navigate(['/login'])
      });
    }
  }

  mostrarToastExito(msg: string): void { this.mensajeToast = msg; this.tipoToast = 'success'; this.mostrarToast = true; setTimeout(() => this.cerrarToast(), 5000); }
  mostrarToastError(msg: string): void { this.mensajeToast = msg; this.tipoToast = 'error'; this.mostrarToast = true; setTimeout(() => this.cerrarToast(), 5000); }
  cerrarToast(): void { this.mostrarToast = false; this.mensajeToast = ''; }
}