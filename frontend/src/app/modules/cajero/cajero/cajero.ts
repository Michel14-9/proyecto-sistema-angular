// src/app/modules/cajero/cajero/cajero.ts
import { Component, OnInit, ViewEncapsulation, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { LayoutService } from '../../../core/services/layout.service';

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
  origen?: string;  // ✅ 'PRESENCIAL' | 'WEB' | 'ONLINE' — viene del backend
  canal?: string;   // ✅ 'CAJA' | 'WEB'
}

// ✅ Helper: true si el pedido fue creado en caja (no pasa por cocina ni delivery)
function esPedidoPresencial(pedido: Pedido | null): boolean {
  return !!pedido && pedido.origen === 'PRESENCIAL';
}

interface Producto {
  id: number;
  nombre: string;
  precio: number;
  tipo: string;
}

interface ProductoSeleccionado {
  productoId: number;
  nombre: string;
  precio: number;
  cantidad: number;
  subtotal: number;
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

  // Pedidos
  pedidosPendientes: Pedido[] = [];
  pedidoSeleccionado: Pedido | null = null;

  // Estadísticas
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

  // Pedido Presencial
  productosDisponibles: Producto[] = [];
  productosSeleccionados: ProductoSeleccionado[] = [];
  nuevoProducto: any = { productoId: null, cantidad: 1 };
  nombreCliente: string = '';
  telefonoCliente: string = '';
  metodoPago: string = 'EFECTIVO';
  tipoEntrega: string = 'LOCAL';
  mostrarFormularioProducto: boolean = false;
  creandoPedido: boolean = false;

  // Intervalos
  private intervalId: any;
  private recargaId: any;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private layoutService: LayoutService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    if (!this.isAuthenticated) {
      this.router.navigate(['/login']);
      return;
    }

    // Ocultar header y footer global
    this.layoutService.hideHeaderAndFooter();

    // Cargar datos
    this.cargarProductos();
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
    // Restaurar header y footer global
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

  // ==================== CARGAR PRODUCTOS ====================
  cargarProductos(): void {
    console.log('🔄 Cargando productos...');

    // ✅ Usar /cajero/productos
    this.http.get(`${this.apiUrl}/cajero/productos`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Productos cargados:', response);
        if (Array.isArray(response)) {
          this.productosDisponibles = response;
        } else if (response && response.success) {
          this.productosDisponibles = response.data || [];
        } else {
          this.productosDisponibles = [];
        }
        if (this.productosDisponibles.length === 0) {
          console.warn('⚠️ No hay productos disponibles');
          this.mostrarToastInfo('No hay productos disponibles para crear pedidos');
        }
      },
      error: (error) => {
        console.error('❌ Error cargando productos:', error);
        this.productosDisponibles = [];
        if (error.status === 401 || error.status === 403) {
          this.mostrarToastError('Sesión expirada. Redirigiendo al login...');
          setTimeout(() => this.router.navigate(['/login']), 2000);
        } else {
          this.mostrarToastError('Error al cargar productos. Verifica que tengas productos creados.');
        }
      }
    });
  }

  // ==================== PEDIDOS PENDIENTES ====================
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
        this.estadisticas.pendientes = this.pedidosPendientes.length;
      }
    });
  }

  // ==================== DETALLE PEDIDO ====================
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

  // ✅ Los pedidos presenciales (origen === 'PRESENCIAL') no pasan por cocina
  // ni tienen delivery: el cliente pide y retira en el mismo mostrador.
  // Se usa en el HTML para ocultar la barra de 4 estados y el bloque
  // Delivery/Recojo cuando corresponde a un pedido de caja.
  esPresencial(pedido: Pedido | null): boolean {
    return !!pedido && pedido.origen === 'PRESENCIAL';
  }

  // ==================== PEDIDO PRESENCIAL - VALIDACIONES ====================
  agregarProductoSeleccionado(): void {
    // ✅ Validar producto seleccionado
    if (!this.nuevoProducto.productoId) {
      this.mostrarToastError('⚠️ Selecciona un producto de la lista');
      return;
    }

    // ✅ Validar cantidad
    if (!this.nuevoProducto.cantidad || this.nuevoProducto.cantidad < 1) {
      this.mostrarToastError('⚠️ La cantidad debe ser mayor a 0');
      return;
    }

    // ✅ Validar que el producto exista
    // Gracias a [ngValue] en el HTML, nuevoProducto.productoId ahora es un number real,
    // por lo que esta comparación estricta funciona correctamente.
    const producto = this.productosDisponibles.find(p => p.id === this.nuevoProducto.productoId);
    if (!producto) {
      this.mostrarToastError('⚠️ Producto no encontrado');
      return;
    }

    // ✅ Agregar o actualizar producto
    const existente = this.productosSeleccionados.find(p => p.productoId === producto.id);
    if (existente) {
      existente.cantidad += this.nuevoProducto.cantidad;
      existente.subtotal = existente.precio * existente.cantidad;
      this.mostrarToastExito(`✅ Cantidad actualizada: ${existente.nombre} x${existente.cantidad}`);
    } else {
      this.productosSeleccionados.push({
        productoId: producto.id,
        nombre: producto.nombre,
        precio: producto.precio,
        cantidad: this.nuevoProducto.cantidad,
        subtotal: producto.precio * this.nuevoProducto.cantidad
      });
      this.mostrarToastExito(`✅ ${producto.nombre} agregado al pedido`);
    }

    // ✅ Limpiar selección
    this.nuevoProducto = { productoId: null, cantidad: 1 };
  }

  eliminarProductoSeleccionado(index: number): void {
    const producto = this.productosSeleccionados[index];
    if (producto) {
      this.productosSeleccionados.splice(index, 1);
      this.mostrarToastInfo(`❌ ${producto.nombre} eliminado del pedido`);
    }
  }

  calcularTotalPresencial(): number {
    return this.productosSeleccionados.reduce((sum, item) => sum + item.subtotal, 0);
  }

  // ✅ Crear Pedido Presencial con validaciones (SIN observaciones)
  crearPedidoPresencial(): void {
    // ✅ Validar que haya productos
    if (this.productosSeleccionados.length === 0) {
      this.mostrarToastError('⚠️ Agrega al menos un producto');
      return;
    }

    // ✅ Validar nombre del cliente
    if (!this.nombreCliente || this.nombreCliente.trim() === '') {
      this.mostrarToastError('⚠️ Ingresa el nombre del cliente');
      return;
    }

    // ✅ Validar teléfono (opcional pero sugerido)
    if (this.telefonoCliente && this.telefonoCliente.trim() !== '' && !/^\d{9}$/.test(this.telefonoCliente.trim())) {
      this.mostrarToastError('⚠️ El teléfono debe tener 9 dígitos');
      return;
    }

    // ✅ Evitar doble envío
    if (this.creandoPedido) {
      return;
    }

    this.creandoPedido = true;

    const pedidoData = {
      items: this.productosSeleccionados.map(item => ({
        productoId: item.productoId,
        cantidad: item.cantidad
      })),
      metodoPago: this.metodoPago,
      tipoEntrega: this.tipoEntrega,
      nombreCliente: this.nombreCliente.trim(),
      telefonoCliente: this.telefonoCliente?.trim() || ''
    };

    console.log('📤 Creando pedido presencial:', pedidoData);

    this.http.post(`${this.apiUrl}/cajero/crear-pedido-presencial`, pedidoData, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('✅ Pedido creado:', response);
        this.creandoPedido = false;
        this.mostrarToastExito(`✅ Pedido #${response.numeroPedido} creado exitosamente`);

        // ✅ Limpiar formulario
        this.productosSeleccionados = [];
        this.nombreCliente = '';
        this.telefonoCliente = '';
        this.metodoPago = 'EFECTIVO';
        this.tipoEntrega = 'LOCAL';

        // ✅ Recargar pedidos pendientes
        this.cargarPedidosPendientes();
      },
      error: (error) => {
        console.error('❌ Error creando pedido:', error);
        this.creandoPedido = false;
        let mensaje = 'Error al crear pedido';
        if (error.error && error.error.message) {
          mensaje = error.error.message;
        } else if (error.error && typeof error.error === 'string') {
          mensaje = error.error;
        }
        this.mostrarToastError('❌ ' + mensaje);
      }
    });
  }

  // ==================== MODAL PAGO ====================
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
        this.mostrarToastExito('✅ Pedido marcado como PAGADO. Abriendo boleta...');

        // ✅ FIX: responseType es 'text', así que la respuesta llega como
        // string JSON crudo. Hay que parsearla para leer boletaPath.
        try {
          const data = JSON.parse(response);
          if (data.boletaPath) {
            // ✅ Abre la boleta PDF en una pestaña nueva, lista para imprimir.
            // Reutiliza el endpoint GET /cajero/boletas/{filename} que ya
            // expone el backend (servirBoleta en CajeroController).
            window.open(`${this.apiUrl}/cajero/boletas/${data.boletaPath}`, '_blank');
          }
        } catch (parseError) {
          console.error('⚠️ No se pudo parsear la respuesta para abrir la boleta:', parseError);
        }

        this.cargarPedidosPendientes();
        this.ocultarDetalle();
      },
      error: (error) => {
        console.error('❌ Error marcando como pagado:', error);
        if (error.status === 401 || error.status === 403) {
          this.mostrarToastError('⚠️ Sesión expirada. Redirigiendo...');
          setTimeout(() => this.router.navigate(['/login']), 2000);
        } else {
          this.mostrarToastError('❌ ' + (error.error || 'Error al marcar como pagado'));
        }
      }
    });
  }

  // ==================== MODAL CANCELAR ====================
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
        this.mostrarToastExito('✅ Pedido cancelado exitosamente');
        this.cargarPedidosPendientes();
        this.ocultarDetalle();
      },
      error: (error) => {
        console.error('❌ Error cancelando pedido:', error);
        if (error.status === 401 || error.status === 403) {
          this.mostrarToastError('⚠️ Sesión expirada. Redirigiendo...');
          setTimeout(() => this.router.navigate(['/login']), 2000);
        } else {
          this.mostrarToastError('❌ ' + (error.error || 'Error al cancelar el pedido'));
        }
      }
    });
  }

  // ==================== UTILIDADES ====================
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

  // ==================== LOGOUT ====================
  // ✅ FIX: authService.logout() devuelve un Observable, así que hay que suscribirse
  // para que realmente se ejecute la petición HTTP y se limpie la sesión.
  // Antes se llamaba sin .subscribe() y nunca se ejecutaba nada dentro del método.
  cerrarSesion(): void {
    if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
      this.authService.logout().subscribe({
        next: () => {
          this.router.navigate(['/login']);
        },
        error: () => {
          // El AuthService ya limpia localStorage y el estado interno
          // aunque la petición HTTP falle, así que igual navegamos al login.
          this.router.navigate(['/login']);
        }
      });
    }
  }

  // ==================== TOAST ====================
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
}
