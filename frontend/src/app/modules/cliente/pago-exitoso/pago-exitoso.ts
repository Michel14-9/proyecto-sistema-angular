// src/app/modules/cliente/pago-exitoso/pago-exitoso.ts
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-pago-exitoso',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './pago-exitoso.html',
  styleUrls: ['./pago-exitoso.css'],
  encapsulation: ViewEncapsulation.None
})
export class PagoExitosoComponent implements OnInit {
  private apiUrl = 'http://localhost:8080';

  pedidoId: string = '';
  numeroPedido: string = '';
  total: number = 0;
  fecha: Date = new Date();
  items: any[] = [];
  isAuthenticated: boolean = false;
  username: string = '';
  isLoading: boolean = true;
  errorMessage: string = '';
  pedido: any = null;

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    // Obtener parámetros de la URL
    this.route.queryParams.subscribe(params => {
      // MercadoPago envía estos parámetros
      this.pedidoId = params['preference_id'] || params['pedidoId'] || params['external_reference'] || '';

      // También buscar en localStorage (por si acaso)
      if (!this.pedidoId) {
        this.pedidoId = localStorage.getItem('ultimoPedidoId') || '';
      }

      console.log('📝 ID del pedido:', this.pedidoId);

      if (this.pedidoId) {
        this.cargarPedido(this.pedidoId);
      } else {
        this.isLoading = false;
        this.errorMessage = 'No se encontró información del pedido';
      }
    });
  }

  cargarPedido(pedidoId: string): void {
    this.isLoading = true;
    this.errorMessage = '';

    // Buscar el pedido por ID
    this.http.get(`${this.apiUrl}/api/pedidos/${pedidoId}`, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Pedido encontrado:', response);
        this.pedido = response;
        this.numeroPedido = response.numeroPedido || response.numero || 'N/A';
        this.total = response.total || 0;
        this.items = response.items || [];
        this.fecha = new Date(response.fechaPedido || response.fecha || Date.now());
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error cargando pedido:', error);

        // Si no encuentra el pedido, intentar buscar por número de pedido
        if (error.status === 404 && this.pedidoId) {
          this.buscarPorNumeroPedido();
        } else {
          this.errorMessage = 'Error al cargar los detalles del pedido';
          this.isLoading = false;
        }
      }
    });
  }

  buscarPorNumeroPedido(): void {
    // Si el ID es numérico, buscar por ID alternativo
    const numeroPedido = this.pedidoId;
    this.http.get(`${this.apiUrl}/api/pedidos/buscar/${numeroPedido}`, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Pedido encontrado por número:', response);
        if (response.success && response.data) {
          this.pedido = response.data;
          this.numeroPedido = response.data.numeroPedido || response.data.numero || 'N/A';
          this.total = response.data.total || 0;
          this.items = response.data.items || [];
          this.fecha = new Date(response.data.fechaPedido || response.data.fecha || Date.now());
        } else {
          this.errorMessage = 'No se encontró el pedido';
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error buscando pedido:', error);
        this.errorMessage = 'No se encontró el pedido. Por favor, verifica tu historial.';
        this.isLoading = false;
      }
    });
  }

  irAMisPedidos(): void {
    this.router.navigate(['/mis-pedidos']);
  }

  irAlMenu(): void {
    this.router.navigate(['/menu']);
  }

  irAlInicio(): void {
    this.router.navigate(['/']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
