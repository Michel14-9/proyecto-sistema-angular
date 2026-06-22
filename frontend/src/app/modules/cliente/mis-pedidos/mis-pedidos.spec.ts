// src/app/modules/cliente/mis-pedidos/mis-pedidos.component.ts
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-mis-pedidos',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './mis-pedidos.component.html',
  styleUrls: ['./mis-pedidos.css'],
  encapsulation: ViewEncapsulation.None
})
export class MisPedidosComponent implements OnInit {
  private apiUrl = 'http://localhost:8080';

  pedidos: any[] = [];
  pedidosFiltrados: any[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';
  isAuthenticated: boolean = false;
  username: string = '';
  totalCarrito: number = 0;

  // Filtros
  terminoBusqueda: string = '';
  filtroEstado: string = 'todos';
  estadosDisponibles: string[] = ['todos', 'PENDIENTE', 'PAGADO', 'CONFIRMADO', 'RECHAZADO', 'CANCELADO', 'ENTREGADO'];

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

    this.cargarPedidos();
  }

  cargarPedidos(): void {
    this.isLoading = true;
    this.errorMessage = '';

    console.log('🔄 Cargando pedidos...');

    this.http.get(`${this.apiUrl}/api/pedidos/mis-pedidos`, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Respuesta del servidor:', response);
        this.isLoading = false;

        if (response && response.success === true) {
          this.pedidos = response.data || [];
          console.log(`✅ ${this.pedidos.length} pedidos cargados`);
        } else if (Array.isArray(response)) {
          this.pedidos = response;
          console.log(`✅ ${this.pedidos.length} pedidos cargados (array)`);
        } else if (response && response.data && Array.isArray(response.data)) {
          this.pedidos = response.data;
          console.log(`✅ ${this.pedidos.length} pedidos cargados (data array)`);
        } else {
          this.errorMessage = 'Error al cargar los pedidos';
          this.pedidos = [];
        }

        this.aplicarFiltros();
      },
      error: (error) => {
        console.error('❌ Error al cargar pedidos:', error);
        this.isLoading = false;

        if (error.status === 401 || error.status === 403) {
          this.errorMessage = '⚠️ Debes iniciar sesión para ver tus pedidos';
          this.isAuthenticated = false;
        } else {
          this.errorMessage = error.error?.message || 'Error al cargar los pedidos';
        }
        this.pedidos = [];
        this.pedidosFiltrados = [];
      }
    });
  }

  aplicarFiltros(): void {
    let filtrados = this.pedidos;

    if (this.filtroEstado !== 'todos') {
      filtrados = filtrados.filter(p => p.estado === this.filtroEstado);
    }

    if (this.terminoBusqueda.trim()) {
      const term = this.terminoBusqueda.toLowerCase().trim();
      filtrados = filtrados.filter(p => {
        const codigo = (p.numeroPedido || p.id || '').toString().toLowerCase();
        const itemsMatch = p.items?.some((item: any) =>
          (item.nombreProducto || item.nombre || '').toLowerCase().includes(term)
        ) || false;
        return codigo.includes(term) || itemsMatch;
      });
    }

    this.pedidosFiltrados = filtrados;
    console.log(`📊 Filtrados: ${this.pedidosFiltrados.length} pedidos`);
  }

  onFiltroEstadoChange(): void {
    this.aplicarFiltros();
  }

  onBusquedaChange(): void {
    this.aplicarFiltros();
  }

  limpiarFiltros(): void {
    this.terminoBusqueda = '';
    this.filtroEstado = 'todos';
    this.aplicarFiltros();
  }

  verDetalle(pedido: any): void {
    this.router.navigate(['/pedido', pedido.id]);
  }

  getEstadoClase(estado: string): string {
    const clases: { [key: string]: string } = {
      'PENDIENTE': 'estado-pendiente',
      'PAGADO': 'estado-pagado',
      'CONFIRMADO': 'estado-pagado',
      'RECHAZADO': 'estado-rechazado',
      'CANCELADO': 'estado-cancelado',
      'ENTREGADO': 'estado-entregado'
    };
    return clases[estado] || 'estado-pendiente';
  }

  getEstadoIcono(estado: string): string {
    const iconos: { [key: string]: string } = {
      'PENDIENTE': '⏳',
      'PAGADO': '✅',
      'CONFIRMADO': '✅',
      'RECHAZADO': '❌',
      'CANCELADO': '🚫',
      'ENTREGADO': '📦'
    };
    return iconos[estado] || '⏳';
  }

  getEstadoTexto(estado: string): string {
    const textos: { [key: string]: string } = {
      'PENDIENTE': 'Pendiente',
      'PAGADO': 'Pagado',
      'CONFIRMADO': 'Confirmado',
      'RECHAZADO': 'Rechazado',
      'CANCELADO': 'Cancelado',
      'ENTREGADO': 'Entregado'
    };
    return textos[estado] || estado;
  }

  getResumenProductos(pedido: any): string {
    if (!pedido.items || pedido.items.length === 0) return 'Sin productos';
    const nombres = pedido.items.slice(0, 2).map((item: any) =>
      item.nombreProducto || item.nombre || 'Producto'
    );
    const restante = pedido.items.length > 2 ? ` y ${pedido.items.length - 2} más` : '';
    return nombres.join(', ') + restante;
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

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
