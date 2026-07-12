import { Component, OnInit, OnDestroy, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

interface ItemPedido {
  id?: number;
  productoId?: number;
  nombreProducto?: string;
  nombre?: string;
  cantidad: number;
  precio: number;
  subtotal?: number;
}

interface Pedido {
  id: number;
  numeroPedido: string;
  canal: string;
  estado: string;
  fecha: string;
  fechaPedido?: string;
  total: number;
  observaciones?: string;
  items: ItemPedido[];
}

@Component({
  selector: 'app-sigue-tu-pedido',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './sigue-tu-pedido.html',
  styleUrls: ['./sigue-tu-pedido.css'],
  encapsulation: ViewEncapsulation.None
})
export class SigueTuPedidoComponent implements OnInit, OnDestroy {
  private apiUrl = 'http://localhost:8080';
  private pollingInterval: any = null;

  numeroPedido: string = '';
  canalPedido: string = 'WEB';
  buscando: boolean = false;
  isLoading: boolean = false;
  pedidoEncontrado: boolean = false;
  errorMessage: string = '';
  pedido: Pedido | null = null;

  isAuthenticated: boolean = false;
  username: string = '';
  totalCarrito: number = 0;

  estadosReales = ['PENDIENTE', 'PAGADO', 'LISTO', 'EN_CAMINO', 'RECHAZADO', 'ENTREGADO'];

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    if (!this.isAuthenticated) {
      this.router.navigate(['/login']);
      return;
    }

    this.route.queryParams.subscribe(params => {
      const numero = params['numero'] || params['pedidoId'];
      if (numero) {
        this.numeroPedido = numero;
        setTimeout(() => {
          this.buscarPedido();
        }, 500);
      }
    });
  }

  ngOnDestroy(): void {
    this.detenerPolling();
    this.detenerPollingTracking();
  }

  buscarPedido(): void {
    if (!this.numeroPedido || this.numeroPedido.trim() === '') {
      this.errorMessage = 'Por favor ingresa un número de pedido';
      return;
    }

    this.buscando = true;
    this.errorMessage = '';
    this.pedidoEncontrado = false;
    this.pedido = null;
    this.detenerPolling();

    console.log('🔍 Buscando pedido:', this.numeroPedido);

    const numeroLimpio = this.numeroPedido.trim().toUpperCase();
    const url = `${this.apiUrl}/api/sigue-tu-pedido/buscar?numeroPedido=${numeroLimpio}&canalPedido=${this.canalPedido}`;

    this.http.get(url, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Respuesta del servidor:', response);
        this.buscando = false;

        if (response && response.success && response.pedido) {
          this.pedido = response.pedido;
          this.pedidoEncontrado = true;
          this.errorMessage = '';

          // Arrancar tracking si el pedido ya está EN_CAMINO
          if (response.pedido.estado === 'EN_CAMINO') {
            this.iniciarPollingTracking(response.pedido.numeroPedido);
          }

          const pedidoId = response.pedido.id;
          if (pedidoId) {
            this.iniciarPolling(pedidoId);
          }
        } else {
          this.errorMessage = response?.message || 'No se encontró el pedido';
          this.pedidoEncontrado = false;
          this.pedido = null;
        }
      },
      error: (error) => {
        console.error('❌ Error buscando pedido:', error);
        this.buscando = false;
        this.pedidoEncontrado = false;
        this.pedido = null;

        if (error.status === 401 || error.status === 403) {
          this.errorMessage = '⚠️ Debes iniciar sesión para ver tus pedidos';
          this.isAuthenticated = false;
        } else if (error.status === 404) {
          this.errorMessage = 'No se encontró un pedido con ese número. Verifica que el número sea correcto.';
        } else {
          this.errorMessage = error.error?.message || 'Error al buscar el pedido';
        }
      }
    });
  }

  iniciarPolling(pedidoId: number): void {
    this.detenerPolling();

    console.log('🔄 Iniciando polling en tiempo real para pedido ID:', pedidoId);

    this.pollingInterval = setInterval(() => {
      this.actualizarEstadoPedido(pedidoId);
    }, 5000);
  }

  actualizarEstadoPedido(pedidoId: number): void {
    const url = `${this.apiUrl}/api/pedidos/${pedidoId}/estado`;

    this.http.get(url, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        if (response && response.estado) {
          const estadoAnterior = this.pedido?.estado;
          const estadoNuevo = response.estado;

          if (estadoAnterior !== estadoNuevo && this.pedido) {
            console.log(`🔄 Estado actualizado: ${estadoAnterior} → ${estadoNuevo}`);
            this.pedido.estado = estadoNuevo;
            this.pedido.total = response.total || this.pedido.total;
            this.mostrarCambioEstado(estadoAnterior, estadoNuevo);

            if (estadoNuevo === 'ENTREGADO' || estadoNuevo === 'RECHAZADO') {
              console.log('⏹️ Pedido finalizado, deteniendo polling');
              this.detenerPolling();
            }
          }
        }
      },
      error: (error) => {
        console.error('❌ Error actualizando estado:', error);
      }
    });
  }

  mostrarCambioEstado(estadoAnterior: string | undefined, estadoNuevo: string): void {
    const mensajes: { [key: string]: string } = {
      'PENDIENTE': '⏳ Pedido creado, esperando pago...',
      'PAGADO': '✅ ¡Pago confirmado! Tu pedido está en preparación',
      'LISTO': '📋 ¡Tu pedido está listo para entrega!',
      'EN_CAMINO': '🚚 ¡Tu pedido está en camino!',
      'RECHAZADO': '❌ El pago fue rechazado',
      'ENTREGADO': '📦 ¡Tu pedido ha sido entregado! 🎉'
    };

    const iconos: { [key: string]: string } = {
      'PENDIENTE': '⏳',
      'PAGADO': '✅',
      'LISTO': '📋',
      'EN_CAMINO': '🚚',
      'RECHAZADO': '❌',
      'ENTREGADO': '📦'
    };

    const mensaje = mensajes[estadoNuevo] || `Estado actualizado: ${estadoNuevo}`;
    const icono = iconos[estadoNuevo] || '📌';

    this.mostrarNotificacion(`${icono} ${mensaje}`, 'info');

    if (estadoNuevo === 'ENTREGADO') {
      setTimeout(() => {
        this.mostrarNotificacion('🎉 ¡Pedido entregado exitosamente! Gracias por tu compra', 'success');
      }, 1000);
    }

    if (estadoNuevo === 'EN_CAMINO' && this.pedido) {
      this.iniciarPollingTracking(this.pedido.numeroPedido);
    }
    if (estadoNuevo === 'ENTREGADO' || estadoNuevo === 'RECHAZADO') {
      this.detenerPollingTracking();
    }
  }

  detenerPolling(): void {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
      console.log('⏹️ Polling detenido');
    }
  }

  // ========== MÉTODOS DE ESTADOS ==========

  getEstadoClase(estado: string): string {
    const clases: { [key: string]: string } = {
      'PENDIENTE': 'estado-pendiente',
      'PAGADO': 'estado-pagado',
      'LISTO': 'estado-listo',
      'EN_CAMINO': 'estado-en-camino',
      'RECHAZADO': 'estado-rechazado',
      'ENTREGADO': 'estado-entregado'
    };
    return clases[estado?.toUpperCase()] || 'estado-pendiente';
  }

  getEstadoTexto(estado: string): string {
    const textos: { [key: string]: string } = {
      'PENDIENTE': 'Pendiente de pago',
      'PAGADO': 'Pagado',
      'LISTO': 'Listo para entrega',
      'EN_CAMINO': 'En camino',
      'RECHAZADO': 'Rechazado',
      'ENTREGADO': 'Entregado'
    };
    return textos[estado?.toUpperCase()] || estado || 'Desconocido';
  }

  getEstadoIcono(estado: string): string {
    const iconos: { [key: string]: string } = {
      'PENDIENTE': '⏳',
      'PAGADO': '✅',
      'LISTO': '📋',
      'EN_CAMINO': '🚚',
      'RECHAZADO': '❌',
      'ENTREGADO': '📦'
    };
    return iconos[estado?.toUpperCase()] || '⏳';
  }

  getEstadoBadgeClass(estado: string): string {
    const badges: { [key: string]: string } = {
      'PENDIENTE': 'bg-warning text-dark',
      'PAGADO': 'bg-success text-white',
      'LISTO': 'bg-info text-white',
      'EN_CAMINO': 'bg-orange text-white',
      'RECHAZADO': 'bg-danger text-white',
      'ENTREGADO': 'bg-purple text-white'
    };
    return badges[estado?.toUpperCase()] || 'bg-warning text-dark';
  }

  getPorcentajeEstado(estado: string): number {
    const estados = ['PENDIENTE', 'PAGADO', 'LISTO', 'EN_CAMINO', 'ENTREGADO'];
    const index = estados.indexOf(estado?.toUpperCase());
    if (index === -1) return 0;
    const porcentajes = [0, 25, 50, 75, 100];
    return porcentajes[index] || 0;
  }

  isEstadoActivo(estadoActual: string, estadoComparar: string): boolean {
    const estados = ['PENDIENTE', 'PAGADO', 'LISTO', 'EN_CAMINO', 'ENTREGADO'];
    const actualIndex = estados.indexOf(estadoActual?.toUpperCase());
    const compararIndex = estados.indexOf(estadoComparar);
    if (actualIndex === -1 || compararIndex === -1) return false;
    return actualIndex >= compararIndex;
  }

  getMensajeEstado(estado: string): string {
    const mensajes: { [key: string]: string } = {
      'PENDIENTE': 'Estamos esperando la confirmación de tu pago',
      'PAGADO': 'Tu pago ha sido confirmado, estamos preparando tu pedido',
      'LISTO': 'Tu pedido está listo y será enviado pronto',
      'EN_CAMINO': 'Tu pedido está en camino a tu dirección',
      'RECHAZADO': 'El pago fue rechazado, intenta nuevamente',
      'ENTREGADO': '¡Tu pedido ha sido entregado exitosamente!'
    };
    return mensajes[estado?.toUpperCase()] || 'Pedido en proceso';
  }

  formatearFecha(fecha: string): string {
    if (!fecha) return 'No especificada';
    try {
      const date = new Date(fecha);
      if (isNaN(date.getTime())) return 'Fecha inválida';
      return date.toLocaleDateString('es-PE', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return 'Fecha inválida';
    }
  }

  // ========== ✅ ACCIONES ==========

  copiarNumeroPedido(): void {
    const numero = this.pedido?.numeroPedido || this.pedido?.id?.toString() || '';
    if (!numero) {
      this.mostrarNotificacion('No se encontró el número de pedido', 'error');
      return;
    }

    navigator.clipboard.writeText(numero).then(() => {
      this.mostrarNotificacion('📋 Número de pedido copiado: ' + numero, 'success');
    }).catch(() => {
      const input = document.createElement('input');
      input.value = numero;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
      this.mostrarNotificacion('📋 Número de pedido copiado: ' + numero, 'success');
    });
  }

  repetirPedido(): void {
    if (!this.pedido || !this.pedido.items || this.pedido.items.length === 0) {
      this.mostrarNotificacion('No hay productos para repetir', 'error');
      return;
    }

    this.mostrarNotificacion('🔄 Agregando productos al carrito...', 'info');

    const productos = this.pedido.items.map(item => ({
      productoId: item.productoId || item.id,
      cantidad: item.cantidad
    }));

    const idsValidos = productos.filter(p => p.productoId);
    if (idsValidos.length === 0) {
      this.mostrarNotificacion('No se pudieron identificar los productos', 'error');
      return;
    }

    this.http.post(`${this.apiUrl}/api/carrito/agregar-multiples`, { items: idsValidos }, {
      withCredentials: true
    }).subscribe({
      next: () => {
        this.mostrarNotificacion('✅ Productos agregados al carrito. ¡Ve a pagar!', 'success');
        setTimeout(() => {
          this.router.navigate(['/carrito']);
        }, 1500);
      },
      error: (error) => {
        console.error('❌ Error al repetir pedido:', error);
        this.mostrarNotificacion('Redirigiendo al menú...', 'info');
        setTimeout(() => {
          this.router.navigate(['/menu'], {
            queryParams: { repetir: this.pedido?.id }
          });
        }, 1000);
      }
    });
  }

  /**
   * ✅ Descargar comprobante en PDF
   */
  descargarComprobante(): void {
    if (!this.pedido) {
      this.mostrarNotificacion('No hay pedido para descargar', 'error');
      return;
    }

    this.mostrarNotificacion('📄 Generando comprobante PDF...', 'info');

    // ✅ Abrir el PDF en nueva pestaña para descargar
    const url = `${this.apiUrl}/api/sigue-tu-pedido/${this.pedido.id}/comprobante`;
    window.open(url, '_blank');
  }

  contactarSoporte(): void {
    if (!this.pedido) {
      this.mostrarNotificacion('No hay pedido para consultar', 'error');
      return;
    }

    const numeroPedido = this.pedido.numeroPedido || this.pedido.id;
    const mensaje = `Hola, necesito ayuda con mi pedido #${numeroPedido}`;
    const url = `https://wa.me/51123456789?text=${encodeURIComponent(mensaje)}`;
    window.open(url, '_blank');
  }

  mostrarNotificacion(mensaje: string, tipo: string = 'info'): void {
    const notificacionAnterior = document.querySelector('.notificacion-flotante');
    if (notificacionAnterior) {
      notificacionAnterior.remove();
    }

    const notificacion = document.createElement('div');
    notificacion.className = `notificacion-flotante notificacion-${tipo}`;

    const iconos: { [key: string]: string } = {
      success: '✅',
      error: '❌',
      warning: '⚠️',
      info: 'ℹ️'
    };
    const icono = iconos[tipo] || '';

    notificacion.innerHTML = `
      <div class="notificacion-contenido">
        <span><span class="notificacion-icono">${icono}</span>${mensaje}</span>
        <button class="notificacion-cerrar">&times;</button>
      </div>
    `;

    document.body.appendChild(notificacion);

    setTimeout(() => {
      notificacion.classList.add('notificacion-visible');
    }, 10);

    const btnCerrar = notificacion.querySelector('.notificacion-cerrar');
    btnCerrar?.addEventListener('click', () => {
      notificacion.classList.remove('notificacion-visible');
      notificacion.classList.add('notificacion-salida');
      setTimeout(() => notificacion.remove(), 400);
    });

    setTimeout(() => {
      if (notificacion.parentNode) {
        notificacion.classList.remove('notificacion-visible');
        notificacion.classList.add('notificacion-salida');
        setTimeout(() => notificacion.remove(), 400);
      }
    }, 5000);
  }

  logout(): void {
    this.detenerPolling();
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // ── NUEVAS PROPIEDADES (añadir al bloque de propiedades de la clase) ──────────
  private trackingApiUrl = 'http://localhost:8082';
  trackingData: any = null;
  ultimaActualizacionTracking: string = '';
  private trackingPollingInterval: any = null;
  private distanciaInicial: number | null = null; // para calcular progreso

  // ── NUEVO MÉTODO: iniciar polling de tracking ──────────────────────────────────
  iniciarPollingTracking(numeroPedido: string): void {
    this.detenerPollingTracking();
    this.consultarTracking(numeroPedido);
    this.trackingPollingInterval = setInterval(() => {
      this.consultarTracking(numeroPedido);
    }, 10000); // cada 10 segundos
  }

  consultarTracking(numeroPedido: string): void {
    this.http.get<any>(`${this.trackingApiUrl}/api/tracking/${numeroPedido}`).subscribe({
      next: (data) => {
        if (data && data.id) {
          this.trackingData = data;
          // Guardar distancia inicial para calcular progreso
          if (this.distanciaInicial === null && data.distanciaKm) {
            this.distanciaInicial = data.distanciaKm;
          }
          const ahora = new Date();
          this.ultimaActualizacionTracking = ahora.toLocaleTimeString('es-PE', {
            hour: '2-digit', minute: '2-digit', second: '2-digit'
          });
          // Detener si ya fue entregado
          if (data.estado === 'COMPLETED') {
            this.detenerPollingTracking();
          }
        }
      },
      error: () => { /* tracking no disponible, continúa sin él */ }
    });
  }

  detenerPollingTracking(): void {
    if (this.trackingPollingInterval) {
      clearInterval(this.trackingPollingInterval);
      this.trackingPollingInterval = null;
    }
  }

  getProgresoPorcentaje(): number {
    if (!this.trackingData?.distanciaKm || !this.distanciaInicial) return 0;
    const recorrido = this.distanciaInicial - this.trackingData.distanciaKm;
    const pct = Math.round((recorrido / this.distanciaInicial) * 100);
    return Math.min(Math.max(pct, 0), 100);
  }
}
