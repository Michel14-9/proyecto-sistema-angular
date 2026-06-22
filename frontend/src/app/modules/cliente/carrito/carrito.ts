// src/app/modules/cliente/carrito/carrito.ts
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './carrito.html',
  styleUrls: ['./carrito.css'],
  encapsulation: ViewEncapsulation.None
})
export class CarritoComponent implements OnInit {
  carritoItems: any[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';
  total: number = 0;
  isAuthenticated: boolean = false;
  username: string = '';
  mostrarModal: boolean = false;
  modalMensaje: string = '';
  accionConfirmar: (() => void) | null = null;

  constructor(
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    if (this.isAuthenticated) {
      this.cargarCarrito();
    } else {
      this.isLoading = false;
      this.errorMessage = '⚠️ Debes iniciar sesión para ver tu carrito';
    }
  }

  cargarCarrito(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.carritoService.obtenerCarrito().subscribe({
      next: (response: any) => {
        console.log('📦 Respuesta carrito:', response);
        if (response && response.items) {
          this.carritoItems = response.items || [];
          this.total = response.total || 0;
        } else {
          this.carritoItems = [];
          this.total = 0;
        }
        this.isLoading = false;
        console.log('🛒 Carrito cargado:', this.carritoItems);
      },
      error: (error: any) => {
        console.error('❌ Error cargando carrito:', error);
        this.errorMessage = 'Error al cargar el carrito';
        this.isLoading = false;
      }
    });
  }

  getCostoEnvio(): number {
    if (this.total === 0) return 0;
    return this.total >= 50 ? 0 : 5;
  }

  getDescuento(): number {
    return this.total >= 80 ? 10 : 0;
  }

  getTotalFinal(): number {
    return this.total + this.getCostoEnvio() - this.getDescuento();
  }

  actualizarCantidad(itemId: number, nuevaCantidad: number): void {
    if (nuevaCantidad < 1) {
      this.confirmarEliminar(itemId);
      return;
    }

    if (nuevaCantidad > 50) {
      this.mostrarNotificacion('La cantidad máxima es 50', 'error');
      return;
    }

    this.carritoService.actualizarCantidad(itemId, nuevaCantidad).subscribe({
      next: (response: any) => {
        if (response && response.success !== false) {
          this.mostrarNotificacion('✅ Cantidad actualizada', 'success');
          this.cargarCarrito();
          this.carritoService.refrescarTotal();
        } else {
          this.mostrarNotificacion('❌ Error al actualizar cantidad', 'error');
        }
      },
      error: (error: any) => {
        console.error('❌ Error actualizando cantidad:', error);
        this.mostrarNotificacion('❌ Error al actualizar cantidad', 'error');
      }
    });
  }

  eliminarItem(itemId: number): void {
    this.carritoService.eliminarProducto(itemId).subscribe({
      next: (response: any) => {
        if (response && response.success !== false) {
          this.mostrarNotificacion('🗑️ Producto eliminado', 'success');
          this.cargarCarrito();
          this.carritoService.refrescarTotal();
        } else {
          this.mostrarNotificacion('❌ Error al eliminar producto', 'error');
        }
      },
      error: (error: any) => {
        console.error('❌ Error eliminando item:', error);
        this.mostrarNotificacion('❌ Error al eliminar producto', 'error');
      }
    });
  }

  confirmarEliminar(itemId: number): void {
    const item = this.carritoItems.find(i => i.id === itemId);
    if (!item) return;

    this.modalMensaje = `¿Estás seguro de eliminar "${item.nombre}" del carrito?`;
    this.mostrarModal = true;
    this.accionConfirmar = () => {
      this.eliminarItem(itemId);
      this.mostrarModal = false;
    };
  }

  vaciarCarrito(): void {
    if (this.carritoItems.length === 0) {
      this.mostrarNotificacion('🛒 El carrito ya está vacío', 'info');
      return;
    }

    this.modalMensaje = '¿Estás seguro de vaciar todo el carrito? Esta acción no se puede deshacer.';
    this.mostrarModal = true;
    this.accionConfirmar = () => {
      this.confirmarVaciarCarrito();
      this.mostrarModal = false;
    };
  }

  confirmarVaciarCarrito(): void {
    this.carritoService.vaciarCarrito().subscribe({
      next: (response: any) => {
        if (response && response.success !== false) {
          this.carritoItems = [];
          this.total = 0;
          this.mostrarNotificacion('🗑️ Carrito vaciado', 'success');
          this.carritoService.refrescarTotal();
        } else {
          this.mostrarNotificacion('❌ Error al vaciar carrito', 'error');
        }
      },
      error: (error: any) => {
        console.error('❌ Error vaciando carrito:', error);
        this.mostrarNotificacion('❌ Error al vaciar carrito', 'error');
      }
    });
  }

  irAlCheckout(): void {
    if (this.carritoItems.length === 0) {
      this.mostrarNotificacion('El carrito está vacío', 'warning');
      return;
    }
    this.router.navigate(['/checkout']);
  }

  mostrarNotificacion(mensaje: string, tipo: string = 'info'): void {
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
    }, 4000);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
