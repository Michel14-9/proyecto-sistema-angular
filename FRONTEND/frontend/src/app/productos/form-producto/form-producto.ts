import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProductoService } from '../../core/services/producto';
import { AuthService } from '../../core/services/auth';
import { Subscription } from 'rxjs';

interface Producto {
  id_producto?: number;
  nombre: string;
  descripcion: string;
  precio: number;
  tipo: string;
  imagenUrl: string;
  disponible?: boolean;
}

interface Categoria {
  value: string;
  label: string;
  icon?: string;
}

@Component({
  selector: 'app-form-producto',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './form-producto.html',
  styleUrls: ['./form-producto.css']
})
export class FormProductoComponent implements OnInit, OnDestroy {
  productoForm!: FormGroup;
  esEdicion: boolean = false;
  productoId: number | null = null;
  productoOriginal: Producto | null = null;
  
  isLoading: boolean = false;
  imagenPreview: string = '/imagenes/default-product.jpg';
  mostrarPreview: boolean = true;
  
  categorias: Categoria[] = [
    { value: 'Pollos', label: '🍗 Pollo a la Brasa' },
    { value: 'Parrillas', label: '🥩 Parrillas' },
    { value: 'Chicharrón', label: '🐷 Chicharrón' },
    { value: 'Broaster', label: '🍗 Broaster' },
    { value: 'Hamburguesas', label: '🍔 Hamburguesas' },
    { value: 'Criollos', label: '🇵🇪 Platos Criollos' },
    { value: 'Combos', label: '🎁 Combos' }
  ];

  // Contador de caracteres
  descripcionLength: number = 0;
  readonly MAX_DESCRIPCION = 500;
  
  // Alertas
  alerta: { tipo: 'success' | 'danger' | 'warning' | 'info'; mensaje: string } | null = null;
  
  // Auto-guardado
  private autoSaveTimer: any;
  private readonly AUTO_SAVE_KEY = 'productoBorrador';
  
  // Suscripciones
  private routeSub?: Subscription;
  private formSub?: Subscription;

  constructor(
    private fb: FormBuilder,
    private productoService: ProductoService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.inicializarFormulario();
  }

  ngOnInit(): void {
    this.verificarParametrosRuta();
    this.configurarContadorDescripcion();
    this.configurarPreviewImagen();
    
    if (!this.esEdicion) {
      this.cargarBorrador();
      this.configurarAutoGuardado();
    }
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
    this.formSub?.unsubscribe();
    if (this.autoSaveTimer) {
      clearTimeout(this.autoSaveTimer);
    }
  }

  private inicializarFormulario(): void {
    this.productoForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      tipo: ['', Validators.required],
      descripcion: ['', Validators.maxLength(this.MAX_DESCRIPCION)],
      precio: [null, [Validators.required, Validators.min(0.01), Validators.max(9999.99)]],
      imagenUrl: ['', Validators.pattern(/^(\/[\w\-./]+|https?:\/\/[\w\-./]+)$/)]
    });
  }

  private verificarParametrosRuta(): void {
    this.routeSub = this.route.params.subscribe(params => {
      if (params['id']) {
        this.esEdicion = true;
        this.productoId = +params['id'];
        this.cargarProducto(this.productoId);
      }
    });
  }

  private cargarProducto(id: number): void {
    this.isLoading = true;
    
    this.productoService.getProducto(id).subscribe({
      next: (producto) => {
        this.productoOriginal = {
          id_producto: producto.id_producto,
          nombre: producto.nombre,
          tipo: producto.tipo || '',
          descripcion: producto.descripcion || '',
          precio: producto.precio,
          imagenUrl: producto.imagenUrl || '',
          disponible: producto.disponible
        };
        this.productoForm.patchValue({
          nombre: producto.nombre,
          tipo: producto.tipo,
          descripcion: producto.descripcion || '',
          precio: producto.precio,
          imagenUrl: producto.imagenUrl || ''
        });
        
        this.actualizarPreviewImagen(producto.imagenUrl);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error al cargar el producto:', error);
        this.mostrarAlerta('Error al cargar el producto', 'danger');
        this.isLoading = false;
        this.router.navigate(['/admin/productos']);
      }
    });
  }

  private configurarContadorDescripcion(): void {
    this.formSub = this.productoForm.get('descripcion')?.valueChanges.subscribe(value => {
      this.descripcionLength = value?.length || 0;
    });
  }

  private configurarPreviewImagen(): void {
    this.productoForm.get('imagenUrl')?.valueChanges.subscribe(url => {
      this.actualizarPreviewImagen(url);
    });
  }

  private actualizarPreviewImagen(url: string | null): void {
    if (url && url.trim()) {
      this.imagenPreview = url;
      this.mostrarPreview = true;
    } else if (this.esEdicion && this.productoOriginal?.imagenUrl) {
      this.imagenPreview = this.productoOriginal.imagenUrl;
      this.mostrarPreview = true;
    } else {
      this.imagenPreview = '/imagenes/default-product.jpg';
      this.mostrarPreview = true;
    }
  }

  onImagenError(): void {
    this.imagenPreview = '/imagenes/default-product.jpg';
    this.mostrarAlerta('No se pudo cargar la imagen desde la URL proporcionada', 'warning');
  }

  // Getters para validación en la vista
  get nombre() { return this.productoForm.get('nombre'); }
  get tipo() { return this.productoForm.get('tipo'); }
  get precio() { return this.productoForm.get('precio'); }
  get descripcion() { return this.productoForm.get('descripcion'); }
  get imagenUrl() { return this.productoForm.get('imagenUrl'); }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.productoForm.get(fieldName);
    return field ? field.invalid && (field.touched || field.dirty) : false;
  }

  getFieldError(fieldName: string): string {
    const field = this.productoForm.get(fieldName);
    if (!field || !field.errors) return '';
    
    const errors = field.errors;
    
    if (errors['required']) return 'Este campo es requerido';
    if (errors['minlength']) return `Mínimo ${errors['minlength'].requiredLength} caracteres`;
    if (errors['maxlength']) return `Máximo ${errors['maxlength'].requiredLength} caracteres`;
    if (errors['min']) return `El valor mínimo es ${errors['min'].min}`;
    if (errors['max']) return `El valor máximo es ${errors['max'].max}`;
    if (errors['pattern']) return 'Formato de URL inválido';
    
    return 'Campo inválido';
  }

  get caracteresRestantes(): number {
    return this.MAX_DESCRIPCION - this.descripcionLength;
  }

  get contadorClass(): string {
    if (this.caracteresRestantes < 50) return 'text-warning';
    if (this.caracteresRestantes <= 0) return 'text-danger';
    return 'text-muted';
  }

  // Métodos de guardado automático (solo creación)
  private configurarAutoGuardado(): void {
    this.formSub = this.productoForm.valueChanges.subscribe(() => {
      if (this.autoSaveTimer) {
        clearTimeout(this.autoSaveTimer);
      }
      this.autoSaveTimer = setTimeout(() => {
        this.guardarBorrador();
      }, 2000);
    });
  }

  private guardarBorrador(): void {
    if (this.esEdicion) return;
    
    const formData = this.productoForm.value;
    if (formData.nombre || formData.descripcion) {
      localStorage.setItem(this.AUTO_SAVE_KEY, JSON.stringify(formData));
      this.mostrarAlerta('Borrador guardado automáticamente', 'info');
    }
  }

  private cargarBorrador(): void {
    const draft = localStorage.getItem(this.AUTO_SAVE_KEY);
    if (draft) {
      try {
        const formData = JSON.parse(draft);
        
        Object.keys(formData).forEach(key => {
          const control = this.productoForm.get(key);
          if (control && !control.value) {
            control.setValue(formData[key]);
          }
        });
        
        this.mostrarAlerta('Borrador anterior cargado', 'info');
      } catch (error) {
        console.error('Error al cargar borrador:', error);
      }
    }
  }

  private limpiarBorrador(): void {
    localStorage.removeItem(this.AUTO_SAVE_KEY);
  }

  // Envío del formulario
  onSubmit(): void {
    if (this.productoForm.invalid) {
      this.marcarTodosComoTocados();
      this.mostrarAlerta('Por favor, completa correctamente todos los campos requeridos', 'danger');
      return;
    }

    const productoData = this.productoForm.value;
    
    if (this.esEdicion && this.productoId) {
      this.actualizarProducto(this.productoId, productoData);
    } else {
      this.crearProducto(productoData);
    }
  }

  private crearProducto(productoData: Partial<Producto>): void {
    this.isLoading = true;
    
    const formData = new FormData();
    formData.append('nombre', productoData.nombre!);
    formData.append('descripcion', productoData.descripcion!);
    formData.append('precio', productoData.precio!.toString());
    formData.append('tipo', productoData.tipo!);
    formData.append('disponible', 'true');
    formData.append('imagenUrl', productoData.imagenUrl || '/imagenes/default-product.jpg');

    this.productoService.crearProducto(formData).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.limpiarBorrador();
        this.mostrarAlerta('Producto creado exitosamente', 'success');
        setTimeout(() => {
          this.router.navigate(['/admin/productos']);
        }, 1500);
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error al crear producto:', error);
        this.mostrarAlerta('Error al crear el producto. Intenta nuevamente.', 'danger');
      }
    });
  }

  private actualizarProducto(id: number, productoData: Partial<Producto>): void {
    this.isLoading = true;
    
    const formData = new FormData();
    formData.append('nombre', productoData.nombre!);
    formData.append('descripcion', productoData.descripcion!);
    formData.append('precio', productoData.precio!.toString());
    formData.append('tipo', productoData.tipo!);
    formData.append('disponible', 'true');
    formData.append('imagenUrl', productoData.imagenUrl || this.productoOriginal?.imagenUrl || '/imagenes/default-product.jpg');

    this.productoService.actualizarProducto(id, formData).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.mostrarAlerta('Producto actualizado exitosamente', 'success');
        setTimeout(() => {
          this.router.navigate(['/admin/productos']);
        }, 1500);
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error al actualizar producto:', error);
        this.mostrarAlerta('Error al actualizar el producto. Intenta nuevamente.', 'danger');
      }
    });
  }

  private marcarTodosComoTocados(): void {
    Object.keys(this.productoForm.controls).forEach(key => {
      const control = this.productoForm.get(key);
      control?.markAsTouched();
    });
  }

  // Alertas
  private mostrarAlerta(mensaje: string, tipo: 'success' | 'danger' | 'warning' | 'info'): void {
    this.alerta = { tipo, mensaje };
    
    setTimeout(() => {
      if (this.alerta?.mensaje === mensaje) {
        this.alerta = null;
      }
    }, 5000);
  }

  cerrarAlerta(): void {
    this.alerta = null;
  }

  getAlertaIcon(): string {
    const icons: Record<string, string> = {
      'success': 'check-circle',
      'danger': 'exclamation-triangle',
      'warning': 'exclamation-circle',
      'info': 'info-circle'
    };
    return this.alerta ? icons[this.alerta.tipo] : 'info-circle';
  }

  // Navegación
  volverAlPanel(): void {
    this.router.navigate(['/admin/productos']);
  }

  cancelar(): void {
    if (this.productoForm.dirty && !this.esEdicion) {
      if (confirm('¿Estás seguro de cancelar? Los cambios no guardados se perderán.')) {
        this.limpiarBorrador();
        this.volverAlPanel();
      }
    } else {
      this.volverAlPanel();
    }
  }

  // Helpers
  get tituloPagina(): string {
    return this.esEdicion ? 'Editar Producto' : 'Agregar Nuevo Producto';
  }

  get textoBoton(): string {
    return this.esEdicion ? 'Actualizar Producto' : 'Guardar Producto';
  }

  get iconoBoton(): string {
    return this.esEdicion ? 'fa-save' : 'fa-plus-circle';
  }
}