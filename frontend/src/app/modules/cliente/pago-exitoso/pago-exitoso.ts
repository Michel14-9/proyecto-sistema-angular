import { Component, OnInit, OnDestroy, ViewEncapsulation } from '@angular/core';
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
export class PagoExitosoComponent implements OnInit, OnDestroy {
  private apiUrl = 'http://localhost:8080';

  // Datos del pedido
  pedidoId: string = '';
  numeroPedido: string = '';
  total: number = 0;
  fecha: Date = new Date();
  items: any[] = [];
  pedido: any = null;

  // Estado del componente
  isAuthenticated: boolean = false;
  username: string = '';
  isLoading: boolean = true;
  errorMessage: string = '';

  // Estado del pago
  estadoActual: string = 'PENDIENTE'; // PENDIENTE | PAGADO | RECHAZADO
  private pollingInterval: any = null;
  private intentosPolling: number = 0;
  private maxIntentosPolling: number = 20; // 20 * 3s = 60 segundos
  private pedidoIdConsulta: string = '';

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
      this.pedidoId = params['preference_id'] ||
                      params['pedidoId'] ||
                      params['external_reference'] ||
                      '';

      // También buscar en localStorage
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
    this.pedidoIdConsulta = pedidoId;

    // Buscar el pedido por ID
    this.http.get(`${this.apiUrl}/api/pedidos/${pedidoId}`, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Pedido encontrado:', response);
        this.procesarPedido(response);
        this.isLoading = false;

        // Si el pedido NO está pagado, iniciar polling
        if (this.estadoActual !== 'PAGADO' && this.estadoActual !== 'RECHAZADO') {
          this.iniciarPolling(pedidoId);
        }
      },
      error: (error) => {
        console.error('❌ Error cargando pedido:', error);
        // Si falla, intentar buscar por número de pedido
        if (error.status === 404 && pedidoId) {
          this.buscarPorNumeroPedido(pedidoId);
        } else {
          this.errorMessage = 'Error al cargar los detalles del pedido';
          this.isLoading = false;
        }
      }
    });
  }

  procesarPedido(data: any): void {
    this.pedido = data;
    this.numeroPedido = data.numeroPedido || data.numero || 'N/A';
    this.total = data.total || 0;
    this.items = data.items || [];
    this.fecha = new Date(data.fechaPedido || data.fecha || Date.now());
    this.estadoActual = data.estado || 'PENDIENTE';

    console.log(`🔄 Estado del pedido: ${this.estadoActual}`);
  }

  buscarPorNumeroPedido(numeroPedido: string): void {
    this.http.get(`${this.apiUrl}/api/pedidos/buscar/${numeroPedido}`, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Pedido encontrado por número:', response);
        if (response.success && response.data) {
          this.procesarPedido(response.data);
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

  iniciarPolling(pedidoId: string): void {
    this.detenerPolling(); // Limpiar intervalos previos
    this.intentosPolling = 0;

    console.log('🔄 Iniciando polling para pedido:', pedidoId);

    this.pollingInterval = setInterval(() => {
      this.intentosPolling++;

      this.http.get(`${this.apiUrl}/api/pedidos/${pedidoId}`, {
        withCredentials: true
      }).subscribe({
        next: (response: any) => {
          const nuevoEstado = response.estado || 'PENDIENTE';

          // Si cambió el estado, actualizar
          if (nuevoEstado !== this.estadoActual) {
            console.log(`🔄 Estado cambiado: ${this.estadoActual} → ${nuevoEstado}`);
            this.estadoActual = nuevoEstado;
            this.pedido = response;
            this.total = response.total || 0;
            this.items = response.items || [];

            // Si ya está pagado o rechazado, detener polling
            if (nuevoEstado === 'PAGADO' || nuevoEstado === 'RECHAZADO') {
              this.detenerPolling();

              // Si está pagado, mostrar animación de éxito
              if (nuevoEstado === 'PAGADO') {
                this.mostrarConfirmacion();
              }
            }
          }

          // Si supera el máximo de intentos, detener polling
          if (this.intentosPolling >= this.maxIntentosPolling) {
            this.detenerPolling();
            this.errorMessage = '⏳ Tiempo de espera agotado. El pago está siendo procesado. Revisa el estado en "Mis Pedidos" más tarde.';
          }
        },
        error: (error) => {
          console.error('❌ Error en polling:', error);
          // Si falla, continuar intentando
          if (this.intentosPolling >= this.maxIntentosPolling) {
            this.detenerPolling();
          }
        }
      });
    }, 3000); // Consultar cada 3 segundos
  }

  detenerPolling(): void {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
      console.log('⏹️ Polling detenido');
    }
  }

  mostrarConfirmacion(): void {
    // Efecto visual de confirmación
    const successIcon = document.querySelector('.success-icon');
    if (successIcon) {
      successIcon.classList.add('confirmado');
    }

    // Opcional: Reproducir sonido
    // this.playConfirmSound();

    console.log('✅ ¡PAGO CONFIRMADO! 🎉');
  }

  // Getters para el HTML
  get estadoColor(): string {
    switch(this.estadoActual) {
      case 'PAGADO': return 'success';
      case 'RECHAZADO': return 'danger';
      case 'PENDIENTE':
      default: return 'warning';
    }
  }

  get estadoIcono(): string {
    switch(this.estadoActual) {
      case 'PAGADO': return 'fa-check-circle';
      case 'RECHAZADO': return 'fa-times-circle';
      case 'PENDIENTE':
      default: return 'fa-clock';
    }
  }

  get estadoTexto(): string {
    switch(this.estadoActual) {
      case 'PAGADO': return '✅ Pago Confirmado';
      case 'RECHAZADO': return '❌ Pago Rechazado';
      case 'PENDIENTE':
      default: return '⏳ Confirmando pago...';
    }
  }

  get estadoBadgeClass(): string {
    switch(this.estadoActual) {
      case 'PAGADO': return 'bg-success';
      case 'RECHAZADO': return 'bg-danger';
      case 'PENDIENTE':
      default: return 'bg-warning text-dark';
    }
  }

  get tituloPagina(): string {
    switch(this.estadoActual) {
      case 'PAGADO': return '¡PAGO EXITOSO!';
      case 'RECHAZADO': return '¡PAGO RECHAZADO!';
      case 'PENDIENTE':
      default: return 'Confirmando pago...';
    }
  }

  get mensajePrincipal(): string {
    switch(this.estadoActual) {
      case 'PAGADO':
        return 'Tu pedido ha sido registrado correctamente. ¡Gracias por tu compra en Luren Chicken!';
      case 'RECHAZADO':
        return 'El pago no pudo ser procesado. Por favor, intenta nuevamente.';
      case 'PENDIENTE':
      default:
        return 'Estamos esperando la confirmación de MercadoPago. No cierres esta página.';
    }
  }

  get mostrarProgresoPago(): boolean {
    return this.estadoActual === 'PENDIENTE';
  }

  get mostrarProgresoConfirmacion(): boolean {
    return this.estadoActual === 'PAGADO';
  }

  get mostrarErrorPago(): boolean {
    return this.estadoActual === 'RECHAZADO';
  }

  irAMisPedidos(): void {
    this.detenerPolling();
    this.router.navigate(['/mis-pedidos']);
  }

  irAlMenu(): void {
    this.detenerPolling();
    this.router.navigate(['/menu']);
  }

  irAlInicio(): void {
    this.detenerPolling();
    this.router.navigate(['/']);
  }

  reintentarPago(): void {
    this.detenerPolling();
    this.router.navigate(['/pago']);
  }

  logout(): void {
    this.detenerPolling();
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  ngOnDestroy(): void {
    this.detenerPolling();
  }
}
