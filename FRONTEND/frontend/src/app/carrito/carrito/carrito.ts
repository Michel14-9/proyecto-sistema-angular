import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { PedidoService } from '../../core/services/pedido';
import { Subscription } from 'rxjs';

interface ItemCarrito {
  id: number;
  producto: {
    id: number;
    nombre: string;
    descripcion: string;
    precio: number;
    imagenUrl: string;
  };
  cantidad: number;
  precioUnitario: number;
}

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './carrito.html',
  styleUrls: ['./carrito.css']
})
export class CarritoComponent implements OnInit, OnDestroy {
  carrito: ItemCarrito[] = [];
  total: number = 0;
  isAuthenticated: boolean = false;
  isLoading: boolean = false;
  
  // Para el modal de confirmación
  modalMensaje: string = '';
  accionPendiente: 'eliminar' | 'vaciar' | null = null;
  itemAEliminar: number | null = null;
  
  private authSubscription?: Subscription;

  constructor(
    private authService: AuthService,
    private pedidoService: PedidoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarCarrito();
    this.verificarAutenticacion();
  }

  ngOnDestroy(): void {
    this.authSubscription?.unsubscribe();
  }

  verificarAutenticacion(): void {
    this.authSubscription = this.authService.currentUser$.subscribe(user => {
      this.isAuthenticated = !!user;
    });
  }

  cargarCarrito(): void {
    const carritoGuardado = localStorage.getItem('carrito');
    if (carritoGuardado) {
      try {
        this.carrito = JSON.parse(carritoGuardado);
        this.calcularTotal();
      } catch (error) {
        console.error('Error al cargar el carrito:', error);
        this.carrito = [];
        this.total = 0;
      }
    } else {
      this.carrito = [];
      this.total = 0;
    }
  }

  calcularTotal(): void {
    this.total = this.carrito.reduce((sum, item) => {
      return sum + (item.precioUnitario * item.cantidad);
    }, 0);
  }

  get cantidadProductos(): number {
    return this.carrito.length;
  }

  get carritoVacio(): boolean {
    return this.carrito.length === 0;
  }

  get costoEnvio(): number {
    if (this.carritoVacio || this.total >= 50) {
      return 0;
    }
    return 5.00;
  }

  get descuento(): number {
    return this.total >= 80 ? 10.00 : 0;
  }

  get totalPagar(): number {
    return this.total - this.descuento + this.costoEnvio;
  }

  get envioGratis(): boolean {
    return !this.carritoVacio && this.total >= 50;
  }

  get hayDescuento(): boolean {
    return this.total >= 80;
  }

  formatearPrecio(precio: number): string {
    return precio.toFixed(2);
  }

  aumentarCantidad(itemId: number): void {
    const item = this.carrito.find(i => i.id === itemId);
    if (item) {
      item.cantidad++;
      this.guardarCarrito();
      this.mostrarNotificacion(`Cantidad actualizada: ${item.producto.nombre}`, 'success');
    }
  }

  disminuirCantidad(itemId: number): void {
    const item = this.carrito.find(i => i.id === itemId);
    if (item && item.cantidad > 1) {
      item.cantidad--;
      this.guardarCarrito();
      this.mostrarNotificacion(`Cantidad actualizada: ${item.producto.nombre}`, 'info');
    } else if (item && item.cantidad === 1) {
      this.confirmarEliminarItem(itemId);
    }
  }

  confirmarEliminarItem(itemId: number): void {
    const item = this.carrito.find(i => i.id === itemId);
    if (item) {
      this.modalMensaje = `¿Estás seguro de eliminar "${item.producto.nombre}" del carrito?`;
      this.accionPendiente = 'eliminar';
      this.itemAEliminar = itemId;
      this.abrirModal();
    }
  }

  eliminarItem(): void {
    if (this.itemAEliminar !== null) {
      const item = this.carrito.find(i => i.id === this.itemAEliminar);
      this.carrito = this.carrito.filter(i => i.id !== this.itemAEliminar);
      this.guardarCarrito();
      
      if (item) {
        this.mostrarNotificacion(`${item.producto.nombre} eliminado del carrito`, 'warning');
      }
      
      this.cerrarModal();
    }
  }

  confirmarVaciarCarrito(): void {
    if (this.carritoVacio) return;
    
    this.modalMensaje = '¿Estás seguro de vaciar todo el carrito?';
    this.accionPendiente = 'vaciar';
    this.abrirModal();
  }

  vaciarCarrito(): void {
    this.carrito = [];
    localStorage.removeItem('carrito');
    this.total = 0;
    this.mostrarNotificacion('El carrito ha sido vaciado', 'info');
    this.cerrarModal();
  }

  guardarCarrito(): void {
    localStorage.setItem('carrito', JSON.stringify(this.carrito));
    this.calcularTotal();
  }

  getSubtotalItem(item: ItemCarrito): number {
    return item.precioUnitario * item.cantidad;
  }

  getImagenProducto(item: ItemCarrito): string {
    return item.producto.imagenUrl || '/imagenes/default-product.jpg';
  }

  procederAlPago(): void {
    if (this.carritoVacio) return;
    
    if (!this.isAuthenticated) {
      this.mostrarNotificacion('Por favor, inicia sesión para continuar con el pago', 'warning');
      this.router.navigate(['/auth/login'], { 
        queryParams: { returnUrl: '/pago' } 
      });
      return;
    }

    this.isLoading = true;
    
    const pedidoRequest = {
      detalles: this.carrito.map(item => ({
        id_producto: item.producto.id,
        cantidad: item.cantidad,
        precio_unitario: item.precioUnitario
      }))
    };

    this.pedidoService.crearPedido(pedidoRequest).subscribe({
      next: (response) => {
        this.isLoading = false;
        localStorage.removeItem('carrito');
        this.router.navigate(['/pagos/registro', response.id_pedido]);
      },
      error: (error) => {
        this.isLoading = false;
        this.mostrarNotificacion('Error al procesar el pedido. Intenta nuevamente.', 'error');
        console.error('Error creando pedido:', error);
      }
    });
  }

  seguirComprando(): void {
    this.router.navigate(['/menu']);
  }

  // Métodos para el modal
  abrirModal(): void {
    const modalElement = document.getElementById('modalConfirmacion');
    if (modalElement) {
      // @ts-ignore - Bootstrap se carga globalmente
      const modal = new bootstrap.Modal(modalElement);
      modal.show();
    }
  }

  cerrarModal(): void {
    const modalElement = document.getElementById('modalConfirmacion');
    if (modalElement) {
      // @ts-ignore - Bootstrap se carga globalmente
      const modal = bootstrap.Modal.getInstance(modalElement);
      modal?.hide();
    }
    this.resetModal();
  }

  resetModal(): void {
    this.modalMensaje = '';
    this.accionPendiente = null;
    this.itemAEliminar = null;
  }

  ejecutarAccionPendiente(): void {
    if (this.accionPendiente === 'eliminar') {
      this.eliminarItem();
    } else if (this.accionPendiente === 'vaciar') {
      this.vaciarCarrito();
    }
  }

  // Sistema de notificaciones
  private mostrarNotificacion(mensaje: string, tipo: 'success' | 'error' | 'info' | 'warning'): void {
    const notificacion = document.createElement('div');
    notificacion.className = `notificacion-flotante notificacion-${tipo}`;
    notificacion.innerHTML = `
      <div class="notificacion-contenido">
        <span>${mensaje}</span>
        <button class="notificacion-cerrar" onclick="this.parentElement.parentElement.remove()">
          <i class="fas fa-times"></i>
        </button>
      </div>
    `;
    document.body.appendChild(notificacion);

    setTimeout(() => {
      notificacion.classList.add('notificacion-salida');
      setTimeout(() => notificacion.remove(), 300);
    }, 4000);
  }
}