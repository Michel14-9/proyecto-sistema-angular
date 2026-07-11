import { Component, OnInit, ViewEncapsulation, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';

import { AlertService } from '../../../core/services/alert.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './checkout.html',
  styleUrls: ['./checkout.css'],
  encapsulation: ViewEncapsulation.None
})
export class CheckoutComponent implements OnInit, OnDestroy {
  private apiUrl = 'http://localhost:8080';

  carritoItems: any[] = [];
  subtotal: number = 0;
  costoEnvio: number = 0;
  descuento: number = 0;
  total: number = 0;
  isLoading: boolean = true;

  isAuthenticated: boolean = false;
  username: string = '';
  usuario: any = {};

  direccion: string = '';
  referencia: string = '';
  ciudad: string = 'Ica';
  telefono: string = '';
  instrucciones: string = '';

  metodoPago: string = 'MERCADOPAGO';

  errorMessage: string = '';
  procesandoPago: boolean = false;
  pedidoCreado: boolean = false;
  pedidoId: number | null = null;

  direcciones: any[] = [];
  direccionSeleccionada: any = null;
  mostrarFormularioDireccion: boolean = true;

  pagoIniciado: boolean = false;
  pagoExitoso: boolean = false;
  verificandoPago: boolean = false;
  private ventanaPago: Window | null = null;
  private intervalId: any = null;
  private intentosVerificacion: number = 0;
  private maxIntentos: number = 30;


  mostrarReintentar: boolean = false;
  pagoFallido: boolean = false;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    if (!this.isAuthenticated) {
      this.router.navigate(['/login']);
      return;
    }

    this.cargarDatosCheckout();
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
    if (this.ventanaPago && !this.ventanaPago.closed) {
      this.ventanaPago.close();
    }
  }

  cargarDatosCheckout(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.http.get(`${this.apiUrl}/api/pago`, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Datos checkout:', response);

        if (response.success) {
          this.usuario = response.usuario || {};
          this.telefono = this.usuario.telefono || '';

          this.carritoItems = response.carrito || [];
          this.subtotal = response.resumen?.subtotal || 0;
          this.costoEnvio = response.resumen?.costoEnvio || 0;
          this.descuento = response.resumen?.descuento || 0;
          this.total = response.resumen?.total || 0;

          this.direcciones = response.direcciones || [];
          if (response.direccionPredeterminada) {
            this.direccionSeleccionada = response.direccionPredeterminada;
            this.cargarDireccionSeleccionada();
          }

          this.isLoading = false;
        } else {
          this.errorMessage = response.message || 'Error al cargar datos';
          this.isLoading = false;
        }
      },
      error: (error) => {
        console.error('❌ Error cargando checkout:', error);
        this.errorMessage = error.error?.message || 'Error al cargar datos del checkout';
        this.isLoading = false;
      }
    });
  }

  cargarDireccionSeleccionada(): void {
    if (this.direccionSeleccionada) {
      this.direccion = this.direccionSeleccionada.direccion || '';
      this.referencia = this.direccionSeleccionada.referencia || '';
      this.ciudad = this.direccionSeleccionada.ciudad || 'Ica';
      this.telefono = this.direccionSeleccionada.telefono || this.usuario.telefono || '';
      this.mostrarFormularioDireccion = false;
    } else {
      this.mostrarFormularioDireccion = true;
    }
  }

  seleccionarDireccion(direccion: any): void {
    this.direccionSeleccionada = direccion;
    this.cargarDireccionSeleccionada();
  }

  usarNuevaDireccion(): void {
    this.direccionSeleccionada = null;
    this.mostrarFormularioDireccion = true;
    this.direccion = '';
    this.referencia = '';
    this.ciudad = 'Ica';
  }

  getCostoEnvioTexto(): string {
    if (this.costoEnvio === 0 && this.subtotal > 0) {
      return 'GRATIS';
    }
    return `S/ ${this.costoEnvio.toFixed(2)}`;
  }

  getTotalFinal(): number {
    return this.subtotal + this.costoEnvio - this.descuento;
  }


  reiniciarPago(): void {
    this.mostrarReintentar = false;
    this.pagoFallido = false;
    this.pagoIniciado = false;
    this.procesandoPago = false;
    this.errorMessage = '';
    this.pagoExitoso = false;
    this.verificandoPago = false;
    this.intentosVerificacion = 0;


    if (this.ventanaPago && !this.ventanaPago.closed) {
      this.ventanaPago.close();
      this.ventanaPago = null;
    }

    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }

    this.alertService.info('🔄 Reintentando pago...');


    if (this.pedidoId) {
      this.crearPreferenciaPago(this.pedidoId);
    } else {
      this.realizarPago();
    }
  }

  realizarPago(): void {
    if (!this.direccion || this.direccion.trim() === '') {
      this.errorMessage = 'Por favor ingresa una dirección de entrega';
      return;
    }

    if (!this.telefono || this.telefono.trim() === '') {
      this.errorMessage = 'Por favor ingresa un teléfono de contacto';
      return;
    }

    if (this.carritoItems.length === 0) {
      this.errorMessage = 'El carrito está vacío';
      return;
    }

    this.procesandoPago = true;
    this.errorMessage = '';
    this.mostrarReintentar = false;
    this.pagoFallido = false;

    const pedidoData = {
      metodoPago: this.metodoPago,
      tipoEntrega: 'DELIVERY',
      direccion: this.direccion,
      instrucciones: this.instrucciones,
      observaciones: this.referencia
    };

    const csrfToken = this.getCsrfToken();
    let headers = new HttpHeaders();
    if (csrfToken) {
      headers = headers.set('X-XSRF-TOKEN', csrfToken);
    }
    headers = headers.set('Content-Type', 'application/json');

    this.http.post(`${this.apiUrl}/api/pedidos/crear`, pedidoData, {
      headers,
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Pedido creado:', response);

        if (response.success) {
          this.pedidoId = response.pedidoId;
          this.pedidoCreado = true;
          localStorage.setItem('ultimoPedidoId', response.pedidoId.toString());
          this.crearPreferenciaPago(response.pedidoId);
        } else {
          this.errorMessage = response.message || 'Error al crear el pedido';
          this.procesandoPago = false;
        }
      },
      error: (error) => {
        console.error('❌ Error creando pedido:', error);
        this.errorMessage = error.error?.message || 'Error al crear el pedido';
        this.procesandoPago = false;
      }
    });
  }

  crearPreferenciaPago(pedidoId: number): void {
    console.log('📝 Creando preferencia para pedido:', pedidoId);

    const csrfToken = this.getCsrfToken();
    let headers = new HttpHeaders();
    if (csrfToken) {
      headers = headers.set('X-XSRF-TOKEN', csrfToken);
    }
    headers = headers.set('Content-Type', 'application/json');

    this.http.post(`${this.apiUrl}/api/pago/crear-preferencia`,
      { pedidoId },
      { headers, withCredentials: true }
    ).subscribe({
      next: (response: any) => {
        console.log('📝 Preferencia creada:', response);

        if (response.initPoint) {
          this.pagoIniciado = true;
          this.procesandoPago = false;


          this.abrirVentanaPago(response.initPoint);


          this.iniciarVerificacionAutomatica();
        } else {
          this.errorMessage = 'Error: No se recibió la URL de pago';
          this.procesandoPago = false;
        }
      },
      error: (error) => {
        console.error('❌ Error creando preferencia:', error);
        this.errorMessage = error.error?.error || 'Error al iniciar el pago';
        this.procesandoPago = false;
      }
    });
  }

  abrirVentanaPago(url: string): void {
    const ancho = 480;
    const alto = 700;
    const izquierda = (window.innerWidth - ancho) / 2;
    const arriba = (window.innerHeight - alto) / 2;

    this.ventanaPago = window.open(
      url,
      'MercadoPago',
      `width=${ancho},height=${alto},left=${izquierda},top=${arriba},resizable=yes,scrollbars=yes`
    );

    if (this.ventanaPago) {

      this.intervalId = setInterval(() => {
        if (this.ventanaPago && this.ventanaPago.closed) {
          console.log('🔄 Ventana de pago cerrada');
          clearInterval(this.intervalId);
          this.intervalId = null;


          if (!this.pagoExitoso && !this.verificandoPago) {
            this.handlePagoCancelado();
          }
        }
      }, 1000);
    } else {
      console.warn('⚠️ No se pudo abrir nueva ventana, redirigiendo...');
      window.location.href = url;
    }
  }


  handlePagoCancelado(): void {
    console.log('🔄 Pago cancelado o ventana cerrada');
    this.pagoIniciado = false;
    this.procesandoPago = false;


    this.alertService.warning(
      'La ventana de pago fue cerrada. Si tuviste un problema, intenta nuevamente.',
      'Pago no completado'
    );


    this.mostrarReintentar = true;
    this.pagoFallido = true;
    this.errorMessage = 'El pago no se completó. Haz clic en "Reintentar pago" para volver a intentarlo.';
  }

  verificarPago(): void {
    if (this.verificandoPago || this.pagoExitoso) return;

    this.verificandoPago = true;
    this.intentosVerificacion = 0;
    this.realizarVerificacion();
  }

  realizarVerificacion(): void {
    if (!this.pedidoId) {
      this.verificandoPago = false;
      return;
    }

    this.intentosVerificacion++;
    console.log(`🔄 Verificando pago (intento ${this.intentosVerificacion})...`);

    this.http.get(`${this.apiUrl}/api/pedidos/${this.pedidoId}/estado`, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log(' Estado del pedido:', response);

        if (response && response.estado) {
          if (response.estado === 'PAGADO' || response.estado === 'CONFIRMADO' || response.estado === 'ENTREGADO') {
            this.pagoExitoso = true;
            this.verificandoPago = false;
            this.errorMessage = '';
            this.mostrarReintentar = false;

            if (this.intervalId) {
              clearInterval(this.intervalId);
              this.intervalId = null;
            }

            this.alertService.success('✅ Pago completado exitosamente');

            setTimeout(() => {
              this.router.navigate(['/pago-exitoso'], {
                queryParams: { pedidoId: this.pedidoId }
              });
            }, 1000);

          } else if (response.estado === 'RECHAZADO' || response.estado === 'CANCELADO') {
            this.errorMessage = '❌ El pago fue rechazado. Intenta nuevamente.';
            this.verificandoPago = false;
            this.mostrarReintentar = true;
            this.pagoFallido = true;

          } else if (this.intentosVerificacion < this.maxIntentos) {

            setTimeout(() => {
              this.realizarVerificacion();
            }, 3000);
          } else {

            this.verificandoPago = false;
            this.errorMessage = '⏳ El pago está en proceso. Puedes verificar en "Mis Pedidos" o reintentar.';
            this.mostrarReintentar = true;
            this.pagoFallido = true;

            this.alertService.warning(
              'El pago está en proceso de verificación. Puedes reintentar o revisar tus pedidos.',
              'Pago en proceso'
            );
          }
        }
      },
      error: (error) => {
        console.error('❌ Error verificando pago:', error);

        if (this.intentosVerificacion < this.maxIntentos) {
          setTimeout(() => {
            this.realizarVerificacion();
          }, 3000);
        } else {
          this.verificandoPago = false;
          this.errorMessage = '⚠️ Error al verificar el pago. Reintenta o revisa tus pedidos.';
          this.mostrarReintentar = true;
          this.pagoFallido = true;
        }
      }
    });
  }

  iniciarVerificacionAutomatica(): void {
    setTimeout(() => {
      if (!this.pagoExitoso) {
        this.verificarPago();
      }
    }, 5000);
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

  volverAlCarrito(): void {
    this.router.navigate(['/carrito']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
