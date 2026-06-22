// src/app/modules/cliente/sigue-tu-pedido/sigue-tu-pedido.ts
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

interface ItemPedido {
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
export class SigueTuPedidoComponent implements OnInit {
  private apiUrl = 'http://localhost:8080';

  numeroPedido: string = '';
  canalPedido: string = 'WEB';
  buscando: boolean = false;
  isLoading: boolean = false;
  pedidoEncontrado: boolean = false;
  errorMessage: string = '';
  pedido: Pedido | null = null;

  // Autenticación
  isAuthenticated: boolean = false;
  username: string = '';
  totalCarrito: number = 0;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    if (!this.isAuthenticated) {
      this.router.navigate(['/login']);
      return;
    }

    // Verificar si hay número de pedido en la URL
    this.route.queryParams.subscribe(params => {
      const numero = params['numero'];
      if (numero) {
        this.numeroPedido = numero;
        setTimeout(() => {
          this.buscarPedido();
        }, 500);
      }
    });
  }

  private getHeaders(): HttpHeaders {
    let headers = new HttpHeaders();
    headers = headers.set('Content-Type', 'application/json');
    return headers;
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

    console.log('🔍 Buscando pedido:', this.numeroPedido);

    const body = {
      numeroPedido: this.numeroPedido.trim().toUpperCase(),
      canal: this.canalPedido
    };

    this.http.post(`${this.apiUrl}/api/pedidos/buscar`, body, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Respuesta del servidor:', response);
        this.buscando = false;

        if (response && response.success && response.data) {
          this.pedido = response.data;
          this.pedidoEncontrado = true;
          this.errorMessage = '';
        } else if (response && response.pedido) {
          this.pedido = response.pedido;
          this.pedidoEncontrado = true;
          this.errorMessage = '';
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
          this.errorMessage = 'No se encontró un pedido con ese número';
        } else {
          this.errorMessage = error.error?.message || 'Error al buscar el pedido';
        }
      }
    });
  }

  getEstadoClase(estado: string): string {
    const clases: { [key: string]: string } = {
      'PENDIENTE': 'bg-secondary',
      'CONFIRMADO': 'bg-warning',
      'PAGADO': 'bg-warning',
      'PREPARACION': 'bg-primary',
      'EN_CAMINO': 'bg-info',
      'ENTREGADO': 'bg-success',
      'RECHAZADO': 'bg-danger',
      'CANCELADO': 'bg-danger'
    };
    return clases[estado?.toUpperCase()] || 'bg-secondary';
  }

  getEstadoTexto(estado: string): string {
    const textos: { [key: string]: string } = {
      'PENDIENTE': 'Pendiente',
      'CONFIRMADO': 'Pagado',
      'PAGADO': 'Pagado',
      'PREPARACION': 'En Preparación',
      'EN_CAMINO': 'En Camino',
      'ENTREGADO': 'Entregado',
      'RECHAZADO': 'Rechazado',
      'CANCELADO': 'Cancelado'
    };
    return textos[estado?.toUpperCase()] || estado || 'Desconocido';
  }

  getPorcentajeEstado(estado: string): number {
    const estados = ['PENDIENTE', 'CONFIRMADO', 'PAGADO', 'PREPARACION', 'EN_CAMINO', 'ENTREGADO'];
    const index = estados.indexOf(estado?.toUpperCase());
    if (index === -1) return 0;
    const porcentajes = [0, 20, 40, 60, 80, 100];
    return porcentajes[index] || 0;
  }

  isEstadoActivo(estadoActual: string, estadoComparar: string): boolean {
    const estados = ['PENDIENTE', 'CONFIRMADO', 'PAGADO', 'PREPARACION', 'EN_CAMINO', 'ENTREGADO'];
    const actualIndex = estados.indexOf(estadoActual?.toUpperCase());
    const compararIndex = estados.indexOf(estadoComparar);
    if (actualIndex === -1 || compararIndex === -1) return false;
    return actualIndex >= compararIndex;
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

  copiarNumeroPedido(): void {
    const numero = this.pedido?.numeroPedido || this.pedido?.id?.toString() || '';
    if (!numero) {
      this.mostrarNotificacion('No se encontró el número de pedido', 'error');
      return;
    }

    navigator.clipboard.writeText(numero).then(() => {
      this.mostrarNotificacion('Número de pedido copiado: ' + numero, 'success');
    }).catch(() => {
      // Fallback
      const input = document.createElement('input');
      input.value = numero;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
      this.mostrarNotificacion('Número de pedido copiado: ' + numero, 'success');
    });
  }

  repetirPedido(): void {
    if (!this.pedido) {
      this.mostrarNotificacion('No hay pedido para repetir', 'error');
      return;
    }

    this.mostrarNotificacion('Preparando para repetir pedido...', 'info');
    setTimeout(() => {
      this.router.navigate(['/menu'], {
        queryParams: { repetir: this.pedido?.id }
      });
    }, 1000);
  }

  descargarComprobante(): void {
    if (!this.pedido) {
      this.mostrarNotificacion('No hay pedido para descargar', 'error');
      return;
    }

    this.mostrarNotificacion('Generando comprobante...', 'info');
    setTimeout(() => {
      window.open(`${this.apiUrl}/api/pedidos/${this.pedido?.id}/comprobante`, '_blank');
    }, 1000);
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
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
