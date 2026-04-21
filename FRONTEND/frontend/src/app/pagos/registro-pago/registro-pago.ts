import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService, CurrentUser } from '../../core/services/auth.service';
import { PedidoService } from '../../core/services/pedido';
import { Subscription } from 'rxjs';
import { PagoService } from '../../core/services/pago';

interface Direccion {
  id: number;
  nombre: string;
  direccion: string;
  predeterminada: boolean;
}

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

interface UsuarioInfo {
  id_usuario: number;
  nombres: string;
  apellidos: string;
  correo: string;
  telefono: string;
  rol: string;
}

@Component({
  selector: 'app-registro-pago',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './registro-pago.html',
  styleUrls: ['./registro-pago.css']
})
export class RegistroPagoComponent implements OnInit, OnDestroy {
  // Formularios
  pagoForm!: FormGroup;
  tarjetaForm!: FormGroup;
  
  // Datos del carrito
  carrito: ItemCarrito[] = [];
  subtotal: number = 0;
  costoEnvio: number = 0;
  descuento: number = 0;
  total: number = 0;
  
  // Usuario
  usuario: UsuarioInfo | null = null;
  isAuthenticated: boolean = false;
  direcciones: Direccion[] = [];
  direccionPredeterminada: Direccion | null = null;
  
  // Estado de la UI
  metodoPagoSeleccionado: 'efectivo' | 'tarjeta' | 'yape' = 'efectivo';
  tipoEntregaSeleccionado: 'delivery' | 'recojo' = 'delivery';
  mostrarTarjetaInfo: boolean = false;
  mostrarYapeInfo: boolean = false;
  mostrarDireccionManual: boolean = false;
  isLoading: boolean = false;
  idPedidoPendiente: number | null = null;
  
  // Horarios disponibles
  horariosEntrega: string[] = [
    '12:00', '12:30', '13:00', '13:30', '14:00', '14:30', '15:00', '15:30',
    '16:00', '16:30', '17:00', '17:30', '18:00', '18:30', '19:00', '19:30',
    '20:00', '20:30', '21:00', '21:30', '22:00'
  ];
  
  // Fechas
  fechaMinima: string = '';
  fechaMaxima: string = '';
  
  private authSubscription?: Subscription;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private pedidoService: PedidoService,
    private pagoService: PagoService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.inicializarFormularios();
    this.configurarFechas();
  }

  ngOnInit(): void {
    this.cargarCarrito();
    this.verificarAutenticacion();
    this.verificarPedidoPendiente();
  }

  ngOnDestroy(): void {
    this.authSubscription?.unsubscribe();
  }

  private inicializarFormularios(): void {
    this.pagoForm = this.fb.group({
      tipoEntrega: ['delivery', Validators.required],
      direccion: ['', Validators.required],
      instrucciones: [''],
      fechaEntrega: ['', Validators.required],
      horaEntrega: ['', Validators.required],
      nombres: ['', Validators.required],
      apellidos: ['', Validators.required],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9+\-\s]{9,15}$/)]],
      email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
      recibirOfertas: [false],
      metodoPago: ['efectivo', Validators.required]
    });

    this.tarjetaForm = this.fb.group({
      numeroTarjeta: ['', [Validators.required, Validators.pattern(/^[\d\s]{16,19}$/)]],
      fechaVencimiento: ['', [Validators.required, Validators.pattern(/^\d{2}\/\d{2}$/)]],
      cvv: ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
      nombreTarjeta: ['', Validators.required]
    });
  }

  private configurarFechas(): void {
    const hoy = new Date();
    const manana = new Date(hoy);
    manana.setDate(hoy.getDate() + 1);
    
    const maxFecha = new Date(hoy);
    maxFecha.setDate(hoy.getDate() + 7);
    
    this.fechaMinima = this.formatoFechaInput(manana);
    this.fechaMaxima = this.formatoFechaInput(maxFecha);
    
    this.pagoForm.patchValue({
      fechaEntrega: this.fechaMinima
    });
  }

  private formatoFechaInput(fecha: Date): string {
    return fecha.toISOString().split('T')[0];
  }

  private verificarAutenticacion(): void {
    this.authSubscription = this.authService.currentUser$.subscribe((user: CurrentUser | null) => {
      this.isAuthenticated = !!user;
      
      if (user) {
        // Convertir CurrentUser a UsuarioInfo
        this.usuario = this.convertirAUsuarioInfo(user);
        this.cargarDatosUsuario(this.usuario);
        this.cargarDirecciones();
      } else {
        this.usuario = null;
      }
    });
  }

  private convertirAUsuarioInfo(currentUser: CurrentUser): UsuarioInfo {
    return {
      id_usuario: currentUser.id_usuario,
      nombres: currentUser.nombres || '',
      apellidos: currentUser.apellidos || '',
      correo: currentUser.username || '', // username es el correo
      telefono: '', // No viene en CurrentUser, se puede obtener de otro lado
      rol: currentUser.rol || 'cliente'
    };
  }

  private cargarDatosUsuario(user: UsuarioInfo): void {
    this.pagoForm.patchValue({
      nombres: user.nombres || '',
      apellidos: user.apellidos || '',
      telefono: user.telefono || '',
      email: user.correo || ''
    });
  }

  private cargarDirecciones(): void {
    // Simulación - Reemplazar con llamada real al servicio
    this.direcciones = [
      { id: 1, nombre: 'Casa', direccion: 'Av. Principal 123, Ica', predeterminada: true },
      { id: 2, nombre: 'Trabajo', direccion: 'Calle Comercio 456, Ica', predeterminada: false }
    ];
    
    this.direccionPredeterminada = this.direcciones.find(d => d.predeterminada) || null;
    
    if (this.direccionPredeterminada) {
      this.pagoForm.patchValue({
        direccion: this.direccionPredeterminada.direccion
      });
    }
  }

  private verificarPedidoPendiente(): void {
    this.route.params.subscribe(params => {
      if (params['idPedido']) {
        this.idPedidoPendiente = +params['idPedido'];
      }
    });
  }

  private cargarCarrito(): void {
    const carritoGuardado = localStorage.getItem('carrito');
    if (carritoGuardado) {
      try {
        this.carrito = JSON.parse(carritoGuardado);
        this.calcularTotales();
      } catch (error) {
        console.error('Error al cargar el carrito:', error);
        this.redirigirACarrito();
      }
    } else {
      this.redirigirACarrito();
    }
  }

  private calcularTotales(): void {
    this.subtotal = this.carrito.reduce((sum, item) => {
      return sum + (item.precioUnitario * item.cantidad);
    }, 0);
    
    this.costoEnvio = this.subtotal >= 50 ? 0 : 5.00;
    this.descuento = this.subtotal >= 80 ? 10.00 : 0;
    this.total = this.subtotal - this.descuento + this.costoEnvio;
  }

  private redirigirACarrito(): void {
    this.mostrarNotificacion('No hay productos en el carrito', 'warning');
    this.router.navigate(['/carrito']);
  }

  // Getters para la vista
  get carritoVacio(): boolean {
    return this.carrito.length === 0;
  }

  get cantidadProductos(): number {
    return this.carrito.reduce((sum, item) => item.cantidad, 0);
  }

  get envioGratis(): boolean {
    return this.subtotal >= 50;
  }

  get hayDescuento(): boolean {
    return this.subtotal >= 80;
  }

  get tiempoEstimado(): string {
    return this.tipoEntregaSeleccionado === 'recojo' ? '15-25 minutos' : '30-45 minutos';
  }

  // Métodos de formato
  formatearPrecio(precio: number): string {
    return precio.toFixed(2);
  }

  getSubtotalItem(item: ItemCarrito): number {
    return item.precioUnitario * item.cantidad;
  }

  // Manejo de cambios en el formulario
  onTipoEntregaChange(): void {
    const tipoEntregaControl = this.pagoForm.get('tipoEntrega');
    this.tipoEntregaSeleccionado = tipoEntregaControl?.value;
    
    const direccionControl = this.pagoForm.get('direccion');
    if (this.tipoEntregaSeleccionado === 'recojo') {
      direccionControl?.clearValidators();
      direccionControl?.setValue('');
    } else {
      direccionControl?.setValidators(Validators.required);
      if (this.direccionPredeterminada) {
        direccionControl?.setValue(this.direccionPredeterminada.direccion);
      }
    }
    direccionControl?.updateValueAndValidity();
  }

  onMetodoPagoChange(): void {
    const metodo = this.pagoForm.get('metodoPago')?.value;
    this.metodoPagoSeleccionado = metodo as 'efectivo' | 'tarjeta' | 'yape';
    this.mostrarTarjetaInfo = this.metodoPagoSeleccionado === 'tarjeta';
    this.mostrarYapeInfo = this.metodoPagoSeleccionado === 'yape';
  }

  onDireccionSelectChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const valor = select.value;
    
    if (valor === 'nueva') {
      this.mostrarDireccionManual = true;
      this.pagoForm.patchValue({ direccion: '' });
    } else if (valor) {
      this.mostrarDireccionManual = false;
      this.pagoForm.patchValue({ direccion: valor });
    }
  }

  // Formateo de inputs
  formatearNumeroTarjeta(event: Event): void {
    const input = event.target as HTMLInputElement;
    let valor = input.value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    const partes: string[] = [];
    
    for (let i = 0; i < valor.length; i += 4) {
      partes.push(valor.substring(i, i + 4));
    }
    
    input.value = partes.join(' ').substring(0, 19);
    this.tarjetaForm.patchValue({ numeroTarjeta: input.value });
  }

  formatearFechaVencimiento(event: Event): void {
    const input = event.target as HTMLInputElement;
    let valor = input.value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    
    if (valor.length >= 2) {
      input.value = valor.substring(0, 2) + '/' + valor.substring(2, 4);
    }
    this.tarjetaForm.patchValue({ fechaVencimiento: input.value });
  }

  formatearCVV(event: Event): void {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/\D/g, '').substring(0, 3);
    this.tarjetaForm.patchValue({ cvv: input.value });
  }

  // Validación
  private validarFormulario(): boolean {
    if (this.pagoForm.invalid) {
      this.marcarCamposInvalidos();
      this.mostrarNotificacion('Completa todos los campos requeridos', 'error');
      return false;
    }

    if (this.metodoPagoSeleccionado === 'tarjeta' && this.tarjetaForm.invalid) {
      this.marcarCamposInvalidosTarjeta();
      this.mostrarNotificacion('Completa correctamente los datos de la tarjeta', 'error');
      return false;
    }

    return true;
  }

  private marcarCamposInvalidos(): void {
    Object.keys(this.pagoForm.controls).forEach(key => {
      const control = this.pagoForm.get(key);
      if (control?.invalid) {
        control.markAsTouched();
      }
    });
  }

  private marcarCamposInvalidosTarjeta(): void {
    Object.keys(this.tarjetaForm.controls).forEach(key => {
      const control = this.tarjetaForm.get(key);
      if (control?.invalid) {
        control.markAsTouched();
      }
    });
  }

  // Confirmar pedido
  confirmarPedido(): void {
    if (!this.validarFormulario()) {
      return;
    }

    this.isLoading = true;

    const datosPago = this.construirDatosPago();
    
    this.pagoService.procesarPago(datosPago).subscribe({
      next: (response) => {
        this.isLoading = false;
        localStorage.removeItem('carrito');
        this.mostrarNotificacion('¡Pedido confirmado exitosamente!', 'success');
        this.router.navigate(['/pedidos/confirmacion', response.id_pedido]);
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error al procesar el pago:', error);
        this.mostrarNotificacion('Error al procesar el pago. Intenta nuevamente.', 'error');
      }
    });
  }

  private construirDatosPago(): any {
    const formValues = this.pagoForm.getRawValue();
    
    const datosPago: any = {
      tipoEntrega: formValues.tipoEntrega,
      fechaEntrega: formValues.fechaEntrega,
      horaEntrega: formValues.horaEntrega,
      instrucciones: formValues.instrucciones,
      metodoPago: formValues.metodoPago,
      recibirOfertas: formValues.recibirOfertas,
      carrito: this.carrito,
      subtotal: this.subtotal,
      costoEnvio: this.costoEnvio,
      descuento: this.descuento,
      total: this.total
    };

    if (formValues.tipoEntrega === 'delivery') {
      datosPago.direccion = formValues.direccion;
    }

    if (formValues.metodoPago === 'tarjeta') {
      datosPago.tarjeta = this.tarjetaForm.value;
    }

    if (this.idPedidoPendiente) {
      datosPago.idPedido = this.idPedidoPendiente;
    }

    return datosPago;
  }

  volverAlCarrito(): void {
    this.router.navigate(['/carrito']);
  }

  // Notificaciones
  private mostrarNotificacion(mensaje: string, tipo: 'success' | 'error' | 'info' | 'warning'): void {
    const notificacion = document.createElement('div');
    notificacion.className = `notificacion-flotante notificacion-${tipo}`;
    notificacion.innerHTML = `
      <div class="notificacion-contenido">
        <span class="notificacion-texto">${mensaje}</span>
        <button class="notificacion-cerrar">&times;</button>
      </div>
    `;
    
    notificacion.querySelector('.notificacion-cerrar')?.addEventListener('click', () => {
      notificacion.classList.add('notificacion-salida');
      setTimeout(() => notificacion.remove(), 300);
    });
    
    document.body.appendChild(notificacion);
    
    setTimeout(() => {
      notificacion.classList.add('notificacion-salida');
      setTimeout(() => notificacion.remove(), 300);
    }, 5000);
  }

  // Helpers para la vista
  isFieldInvalid(fieldName: string): boolean {
    const field = this.pagoForm.get(fieldName);
    return field ? field.invalid && field.touched : false;
  }

  isTarjetaFieldInvalid(fieldName: string): boolean {
    const field = this.tarjetaForm.get(fieldName);
    return field ? field.invalid && field.touched : false;
  }
}