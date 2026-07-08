// src/app/modules/admin/admin-menu/admin-menu.ts
import { Component, OnInit, ViewEncapsulation, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import {
  Chart,
  LinearScale,
  LogarithmicScale,
  CategoryScale,
  BarController,
  BarElement,
  LineController,
  LineElement,
  PointElement,
  ArcElement,
  DoughnutController,
  PieController,
  Legend,
  Tooltip,
  Title,
  Filler
} from 'chart.js';

Chart.register(
  LinearScale,
  LogarithmicScale,
  CategoryScale,
  BarController,
  BarElement,
  LineController,
  LineElement,
  PointElement,
  ArcElement,
  DoughnutController,
  PieController,
  Legend,
  Tooltip,
  Title,
  Filler
);

declare var bootstrap: any;

interface Producto {
  id: number;
  nombre: string;
  tipo: string;
  precio: number;
  descripcion: string;
  imagenUrl: string;
}

interface Usuario {
  id: number;
  nombres: string;
  apellidos: string;
  tipoDocumento: string;
  numeroDocumento: string;
  telefono: string;
  fechaNacimiento: string;
  username: string;
  rol: string;
  password?: string;
}

interface Pedido {
  id: number;
  numeroPedido: string;
  cliente: string;
  fecha: string;
  total: number;
  estado: string;
  items: any[];
}

@Component({
  selector: 'app-admin-menu',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './admin-menu.html',
  styleUrls: ['./admin-menu.css'],
  encapsulation: ViewEncapsulation.None
})
export class AdminMenuComponent implements OnInit, AfterViewInit, OnDestroy {
  private apiUrl = 'http://localhost:8080';

  // Autenticación
  isAuthenticated: boolean = false;
  username: string = '';
  seccionActual: string = 'dashboard';
  tituloSeccion: string = 'Dashboard';

  // Alertas
  mensajeAlerta: string = '';
  tipoAlerta: string = '';

  // Estadísticas Dashboard
  estadisticas: any = {
    totalProductos: 0,
    totalUsuarios: 0,
    pedidosHoy: 0,
    ingresosHoy: 0,
    ventasMesTotal: 0,
    promedioDiario: 0,
    ventaMaxima: 0,
    totalPedidos: 0
  };
  ventasRecientes: Pedido[] = [];
  tieneDatosVentas: boolean = false;
  salesChartInstance: any = null;

  // Productos
  productos: Producto[] = [];
  productosFiltrados: Producto[] = [];
  busquedaProductos: string = '';
  filtroCategoria: string = '';
  productoForm: Producto = {
    id: 0,
    nombre: '',
    tipo: '',
    precio: 0,
    descripcion: '',
    imagenUrl: ''
  };
  productoEditando: boolean = false;
  guardandoProducto: boolean = false;

  // Usuarios
  usuarios: Usuario[] = [];
  usuariosFiltrados: Usuario[] = [];
  busquedaUsuarios: string = '';
  filtroRol: string = '';
  usuarioForm: Usuario = {
    id: 0,
    nombres: '',
    apellidos: '',
    tipoDocumento: '',
    numeroDocumento: '',
    telefono: '',
    fechaNacimiento: '',
    username: '',
    rol: '',
    password: ''
  };
  usuarioEditando: boolean = false;
  guardandoUsuario: boolean = false;

  // Modales
  modalProductoTitulo: string = 'Agregar Producto';
  modalUsuarioTitulo: string = 'Agregar Usuario';
  mensajeConfirmacion: string = '';
  accionConfirmar: any = null;

  // Reportes
  reporteTipo: string = 'ventas';
  reporteRango: string = 'hoy';
  reporteFechaInicio: string = '';
  reporteFechaFin: string = '';
  reporteMetricas: any = {
    totalVentas: 0,
    totalPedidos: 0,
    productosVendidos: 0,
    crecimiento: 0
  };
  reporteDatosGrafico: any = null;
  reporteDatosTabla: any[] = [];
  reporteColumnas: string[] = [];
  reporteTituloGrafico: string = 'Ventas por Día';
  reporteTituloTabla: string = 'Detalle de Ventas';
  reportChartInstance: any = null;
  categoryChartInstance: any = null;

  // Modales Bootstrap
  private productModal: any = null;
  private userModal: any = null;
  private confirmModal: any = null;

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

    this.inicializarFechas();
    this.cargarDashboard();
    this.cargarProductos();
    this.cargarUsuarios();
  }

  ngAfterViewInit(): void {
    const productModalEl = document.getElementById('productModal');
    const userModalEl = document.getElementById('userModal');
    const confirmModalEl = document.getElementById('confirmModal');

    if (productModalEl) {
      this.productModal = new bootstrap.Modal(productModalEl);
    }
    if (userModalEl) {
      this.userModal = new bootstrap.Modal(userModalEl);
    }
    if (confirmModalEl) {
      this.confirmModal = new bootstrap.Modal(confirmModalEl);
    }
  }

  ngOnDestroy(): void {
    if (this.salesChartInstance) {
      this.salesChartInstance.destroy();
      this.salesChartInstance = null;
    }
    if (this.reportChartInstance) {
      this.reportChartInstance.destroy();
      this.reportChartInstance = null;
    }
    if (this.categoryChartInstance) {
      this.categoryChartInstance.destroy();
      this.categoryChartInstance = null;
    }
  }

  private getHeaders(): HttpHeaders {
    let headers = new HttpHeaders();
    headers = headers.set('Content-Type', 'application/json');
    return headers;
  }

  // ============ SECCIONES ============
  cambiarSeccion(seccion: string): void {
    this.seccionActual = seccion;
    const titulos: { [key: string]: string } = {
      'dashboard': 'Dashboard',
      'menu': 'Gestionar Menú',
      'users': 'Gestionar Usuarios',
      'reports': 'Reportes y Estadísticas'
    };
    this.tituloSeccion = titulos[seccion] || seccion;

    if (seccion === 'reports') {
      this.inicializarFechas();
    }
  }

  // ============ ALERTAS ============
  mostrarAlerta(mensaje: string, tipo: string = 'success'): void {
    this.mensajeAlerta = mensaje;
    this.tipoAlerta = tipo;
    setTimeout(() => {
      this.limpiarAlerta();
    }, 5000);
  }

  limpiarAlerta(): void {
    this.mensajeAlerta = '';
    this.tipoAlerta = '';
  }

  // ============ DASHBOARD ============
  cargarDashboard(): void {
    this.cargarEstadisticasDashboard();
    this.cargarVentasRecientes();
    this.cargarDatosGraficoVentas();
  }

  cargarEstadisticasDashboard(): void {
    this.http.get(`${this.apiUrl}/admin-menu/estadisticas-dashboard`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (data: any) => {
        if (data && data.success !== false) {
          this.estadisticas = {
            totalProductos: data.totalProductos || 0,
            totalUsuarios: data.totalUsuarios || 0,
            pedidosHoy: data.pedidosHoy || 0,
            ingresosHoy: data.ingresosHoy || 0,
            ventasMesTotal: data.ventasMesTotal || 0,
            promedioDiario: data.promedioDiario || 0,
            ventaMaxima: data.ventaMaxima || 0,
            totalPedidos: data.totalPedidos || 0
          };
        }
      },
      error: (error) => {
        console.error('❌ Error cargando estadísticas:', error);
      }
    });
  }

  cargarVentasRecientes(): void {
    this.http.get(`${this.apiUrl}/admin-menu/ventas-recientes`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (data: any) => {
        if (Array.isArray(data)) {
          this.ventasRecientes = data;
        }
      },
      error: (error) => {
        console.error('❌ Error cargando ventas recientes:', error);
      }
    });
  }

  cargarDatosGraficoVentas(): void {
    this.http.get(`${this.apiUrl}/admin-menu/estadisticas-ventas`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (data: any) => {
        if (data && data.success && data.ventasPorDia) {
          const ventasPorDia = data.ventasPorDia;
          const hasData = Object.values(ventasPorDia).some((v: any) => v > 0);
          this.tieneDatosVentas = hasData;

          if (hasData) {
            setTimeout(() => {
              this.actualizarGraficoVentas(ventasPorDia);
            }, 300);
          }
        }
      },
      error: (error) => {
        console.error('❌ Error cargando datos de ventas:', error);
      }
    });
  }

  actualizarGraficoVentas(ventasPorDia: any): void {
    const ctx = document.getElementById('salesChart') as HTMLCanvasElement;
    if (!ctx) return;

    if (this.salesChartInstance) {
      this.salesChartInstance.destroy();
      this.salesChartInstance = null;
    }
    const existing = Chart.getChart(ctx);
    if (existing) {
      existing.destroy();
    }

    const labels = Object.keys(ventasPorDia);
    const data = Object.values(ventasPorDia);

    this.salesChartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Ventas (S/)',
          data: data,
          borderColor: '#007bff',
          backgroundColor: 'rgba(0, 123, 255, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: {
            display: true,
            position: 'top'
          },
          tooltip: {
            callbacks: {
              label: function(context: any) {
                return `Ventas: S/ ${context.parsed.y.toFixed(2)}`;
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: function(value: any) {
                return 'S/ ' + value.toFixed(2);
              }
            }
          }
        }
      }
    });
  }

  // ============ PRODUCTOS ============
  cargarProductos(): void {
    this.http.get(`${this.apiUrl}/admin-menu/productos`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (data: any) => {
        if (Array.isArray(data)) {
          this.productos = data.filter((p: any) => p.activo !== false);
          this.filtrarProductos();
        }
      },
      error: (error) => {
        console.error('❌ Error cargando productos:', error);
        this.mostrarAlerta('Error al cargar productos', 'warning');
      }
    });
  }

  filtrarProductos(): void {
    let filtrados = this.productos;

    if (this.busquedaProductos.trim()) {
      const term = this.busquedaProductos.toLowerCase().trim();
      filtrados = filtrados.filter(p =>
        p.nombre.toLowerCase().includes(term) ||
        (p.descripcion && p.descripcion.toLowerCase().includes(term))
      );
    }

    if (this.filtroCategoria) {
      filtrados = filtrados.filter(p =>
        p.tipo.toLowerCase() === this.filtroCategoria.toLowerCase()
      );
    }

    this.productosFiltrados = filtrados;
  }

  abrirModalProducto(producto?: Producto): void {
    if (producto) {
      this.productoEditando = true;
      this.modalProductoTitulo = 'Editar Producto';
      this.productoForm = { ...producto };
    } else {
      this.productoEditando = false;
      this.modalProductoTitulo = 'Agregar Producto';
      this.productoForm = {
        id: 0,
        nombre: '',
        tipo: '',
        precio: 0,
        descripcion: '',
        imagenUrl: ''
      };
    }
    if (this.productModal) {
      this.productModal.show();
    }
  }

  editarProducto(producto: Producto): void {
    this.abrirModalProducto(producto);
  }

  cerrarModalProducto(): void {
    if (this.productModal) {
      this.productModal.hide();
    }
  }

  guardarProducto(): void {
    if (!this.productoForm.nombre.trim()) {
      this.mostrarAlerta('El nombre del producto es requerido', 'warning');
      return;
    }
    if (!this.productoForm.tipo) {
      this.mostrarAlerta('Debe seleccionar una categoría', 'warning');
      return;
    }
    if (this.productoForm.precio <= 0) {
      this.mostrarAlerta('El precio debe ser mayor a 0', 'warning');
      return;
    }

    this.guardandoProducto = true;

    const url = this.productoEditando
      ? `${this.apiUrl}/admin-menu/actualizar/${this.productoForm.id}`
      : `${this.apiUrl}/admin-menu/guardar`;

    const formData = new FormData();
    formData.append('nombre', this.productoForm.nombre);
    formData.append('tipo', this.productoForm.tipo);
    formData.append('precio', this.productoForm.precio.toString());
    formData.append('descripcion', this.productoForm.descripcion || '');
    formData.append('imagenUrl', this.productoForm.imagenUrl || '');

    this.http.post(url, formData, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        this.guardandoProducto = false;
        this.cerrarModalProducto();
        this.mostrarAlerta(
          this.productoEditando ? 'Producto actualizado exitosamente' : 'Producto guardado exitosamente',
          'success'
        );
        this.cargarProductos();
      },
      error: (error) => {
        this.guardandoProducto = false;
        console.error('❌ Error guardando producto:', error);
        this.mostrarAlerta('Error al guardar el producto', 'danger');
      }
    });
  }

  confirmarEliminarProducto(producto: Producto): void {
    this.mensajeConfirmacion = `¿Está seguro de eliminar el producto "${producto.nombre}"? Esta acción no se puede deshacer.`;
    this.accionConfirmar = () => {
      this.eliminarProducto(producto.id);
    };
    if (this.confirmModal) {
      this.confirmModal.show();
    }
  }

  eliminarProducto(id: number): void {
    this.http.post(`${this.apiUrl}/admin-menu/eliminar/${id}`, {}, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        this.cerrarModalConfirmacion();
        const mensaje = response.desactivado
          ? 'Producto desactivado (tiene pedidos históricos)'
          : 'Producto eliminado exitosamente';
        this.mostrarAlerta(mensaje, 'success');
        this.cargarProductos();
      },
      error: (error) => {
        console.error('❌ Error eliminando producto:', error);
        this.mostrarAlerta('Error al eliminar el producto', 'danger');
      }
    });
  }

  // ============ USUARIOS ============
  cargarUsuarios(): void {
    this.http.get(`${this.apiUrl}/admin-menu/usuarios`, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (data: any) => {
        if (Array.isArray(data)) {
          this.usuarios = data.filter((u: any) => u.activo !== false);
          this.filtrarUsuarios();
        }
      },
      error: (error) => {
        console.error('❌ Error cargando usuarios:', error);
        this.mostrarAlerta('Error al cargar usuarios', 'warning');
      }
    });
  }

  filtrarUsuarios(): void {
    let filtrados = this.usuarios;

    if (this.busquedaUsuarios.trim()) {
      const term = this.busquedaUsuarios.toLowerCase().trim();
      filtrados = filtrados.filter(u =>
        u.nombres.toLowerCase().includes(term) ||
        u.apellidos.toLowerCase().includes(term) ||
        u.username.toLowerCase().includes(term)
      );
    }

    if (this.filtroRol) {
      filtrados = filtrados.filter(u =>
        u.rol.toLowerCase() === this.filtroRol.toLowerCase()
      );
    }

    this.usuariosFiltrados = filtrados;
  }

  abrirModalUsuario(usuario?: Usuario): void {
    if (usuario) {
      this.usuarioEditando = true;
      this.modalUsuarioTitulo = 'Editar Usuario';
      this.usuarioForm = { ...usuario };
      this.usuarioForm.password = '';
    } else {
      this.usuarioEditando = false;
      this.modalUsuarioTitulo = 'Agregar Usuario';
      this.usuarioForm = {
        id: 0,
        nombres: '',
        apellidos: '',
        tipoDocumento: '',
        numeroDocumento: '',
        telefono: '',
        fechaNacimiento: '',
        username: '',
        rol: '',
        password: ''
      };
    }
    if (this.userModal) {
      this.userModal.show();
    }
  }

  cerrarModalUsuario(): void {
    if (this.userModal) {
      this.userModal.hide();
    }
  }

  guardarUsuario(): void {
    if (!this.usuarioForm.nombres.trim()) {
      this.mostrarAlerta('Los nombres son requeridos', 'warning');
      return;
    }
    if (!this.usuarioForm.apellidos.trim()) {
      this.mostrarAlerta('Los apellidos son requeridos', 'warning');
      return;
    }
    if (!this.usuarioForm.tipoDocumento) {
      this.mostrarAlerta('Debe seleccionar un tipo de documento', 'warning');
      return;
    }
    if (!this.usuarioForm.numeroDocumento.trim()) {
      this.mostrarAlerta('El número de documento es requerido', 'warning');
      return;
    }
    if (!this.usuarioForm.telefono.trim()) {
      this.mostrarAlerta('El teléfono es requerido', 'warning');
      return;
    }
    if (!this.usuarioForm.fechaNacimiento) {
      this.mostrarAlerta('La fecha de nacimiento es requerida', 'warning');
      return;
    }
    if (!this.usuarioForm.username) {
      this.mostrarAlerta('El email es requerido', 'warning');
      return;
    }
    if (!this.usuarioForm.rol) {
      this.mostrarAlerta('Debe seleccionar un rol', 'warning');
      return;
    }
    if (!this.usuarioForm.password || this.usuarioForm.password.length < 6) {
      this.mostrarAlerta('La contraseña debe tener al menos 6 caracteres', 'warning');
      return;
    }

    this.guardandoUsuario = true;

    const formData = new FormData();
    formData.append('nombres', this.usuarioForm.nombres);
    formData.append('apellidos', this.usuarioForm.apellidos);
    formData.append('tipoDocumento', this.usuarioForm.tipoDocumento);
    formData.append('numeroDocumento', this.usuarioForm.numeroDocumento);
    formData.append('telefono', this.usuarioForm.telefono);
    formData.append('fechaNacimiento', this.usuarioForm.fechaNacimiento);
    formData.append('email', this.usuarioForm.username);
    formData.append('rol', this.usuarioForm.rol);
    formData.append('password', this.usuarioForm.password);

    this.http.post(`${this.apiUrl}/admin-menu/usuarios/guardar`, formData, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        this.guardandoUsuario = false;
        this.cerrarModalUsuario();
        this.mostrarAlerta('Usuario guardado exitosamente', 'success');
        this.cargarUsuarios();
      },
      error: (error) => {
        this.guardandoUsuario = false;
        console.error('❌ Error guardando usuario:', error);
        this.mostrarAlerta('Error al guardar el usuario', 'danger');
      }
    });
  }

  confirmarEliminarUsuario(usuario: Usuario): void {
    this.mensajeConfirmacion = `¿Está seguro de eliminar al usuario "${usuario.nombres} ${usuario.apellidos}"? Esta acción no se puede deshacer.`;
    this.accionConfirmar = () => {
      this.eliminarUsuario(usuario.id);
    };
    if (this.confirmModal) {
      this.confirmModal.show();
    }
  }

  eliminarUsuario(id: number): void {
    this.http.post(`${this.apiUrl}/admin-menu/usuarios/eliminar/${id}`, {}, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        this.cerrarModalConfirmacion();
        const mensaje = response.desactivado
          ? 'Usuario desactivado (tiene pedidos históricos)'
          : 'Usuario eliminado exitosamente';
        this.mostrarAlerta(mensaje, 'success');
        this.cargarUsuarios();
      },
      error: (error) => {
        console.error('❌ Error eliminando usuario:', error);
        this.mostrarAlerta('Error al eliminar el usuario', 'danger');
      }
    });
  }

  // ============ REPORTES ============
  inicializarFechas(): void {
    const hoy = new Date();
    const primerDiaMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    this.reporteFechaInicio = this.formatLocalDate(primerDiaMes);
    this.reporteFechaFin = this.formatLocalDate(hoy);
  }

  formatLocalDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  cambiarRangoFechas(): void {
    const hoy = new Date();
    let startDate: Date = new Date();
    let endDate: Date = new Date();

    switch(this.reporteRango) {
      case 'hoy':
        startDate = hoy;
        endDate = hoy;
        break;
      case 'ayer':
        startDate = new Date(hoy);
        startDate.setDate(hoy.getDate() - 1);
        endDate = startDate;
        break;
      case 'semana':
        startDate = new Date(hoy);
        startDate.setDate(hoy.getDate() - hoy.getDay());
        endDate = hoy;
        break;
      case 'mes':
        startDate = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
        endDate = hoy;
        break;
      case 'personalizado':
        return;
      default:
        return;
    }

    this.reporteFechaInicio = this.formatLocalDate(startDate);
    this.reporteFechaFin = this.formatLocalDate(endDate);
  }

  cambiarTipoReporte(): void {
    const titulos: { [key: string]: { grafico: string, tabla: string } } = {
      'ventas': { grafico: 'Ventas por Día', tabla: 'Detalle de Ventas' },
      'productos': { grafico: 'Productos Más Vendidos', tabla: 'Productos Más Vendidos' },
      'usuarios': { grafico: 'Actividad de Usuarios', tabla: 'Actividad de Usuarios' },
      'pedidos': { grafico: 'Estadísticas de Pedidos', tabla: 'Estadísticas de Pedidos' }
    };

    if (titulos[this.reporteTipo]) {
      this.reporteTituloGrafico = titulos[this.reporteTipo].grafico;
      this.reporteTituloTabla = titulos[this.reporteTipo].tabla;
    }
  }

  generarReporte(): void {
    if (!this.reporteFechaInicio || !this.reporteFechaFin) {
      this.mostrarAlerta('Por favor seleccione un rango de fechas válido', 'warning');
      return;
    }

    if (new Date(this.reporteFechaInicio) > new Date(this.reporteFechaFin)) {
      this.mostrarAlerta('La fecha de inicio no puede ser mayor a la fecha fin', 'warning');
      return;
    }

    this.mostrarAlerta('Generando reporte...', 'info');

    const url = `${this.apiUrl}/admin-menu/reportes/${this.reporteTipo}?fechaInicio=${this.reporteFechaInicio}&fechaFin=${this.reporteFechaFin}`;

    this.http.get(url, {
      headers: this.getHeaders(),
      withCredentials: true
    }).subscribe({
      next: (data: any) => {
        if (data && data.success) {
          this.reporteMetricas = data.metricas || {
            totalVentas: 0,
            totalPedidos: 0,
            productosVendidos: 0,
            crecimiento: 0
          };
          this.reporteDatosGrafico = data.datosGrafico || null;
          this.reporteDatosTabla = data.tablaDatos || [];
          this.reporteColumnas = data.columnas || [];

          setTimeout(() => {
            this.actualizarGraficosReporte();
          }, 300);

          this.mostrarAlerta('Reporte generado exitosamente', 'success');
        } else {
          this.mostrarAlerta(data.error || 'Error al generar reporte', 'danger');
        }
      },
      error: (error) => {
        console.error('❌ Error generando reporte:', error);
        this.mostrarAlerta('Error al generar reporte', 'danger');
      }
    });
  }

  actualizarGraficosReporte(): void {
    // Report Chart
    const reportCtx = document.getElementById('reportChart') as HTMLCanvasElement;
    if (reportCtx && this.reporteDatosGrafico) {
      if (this.reportChartInstance) {
        this.reportChartInstance.destroy();
        this.reportChartInstance = null;
      }
      const existingReport = Chart.getChart(reportCtx);
      if (existingReport) {
        existingReport.destroy();
      }

      const isBar = this.reporteTipo === 'productos';
      this.reportChartInstance = new Chart(reportCtx, {
        type: isBar ? 'bar' : 'line',
        data: {
          labels: this.reporteDatosGrafico.labels || [],
          datasets: [{
            label: this.reporteTituloGrafico,
            data: this.reporteDatosGrafico.datos || [],
            backgroundColor: isBar ? 'rgba(54, 162, 235, 0.5)' : 'rgba(75, 192, 192, 0.2)',
            borderColor: isBar ? 'rgba(54, 162, 235, 1)' : 'rgba(75, 192, 192, 1)',
            borderWidth: 2,
            fill: !isBar
          }]
        },
        options: {
          responsive: true,
          plugins: {
            legend: {
              display: true,
              position: 'top'
            }
          },
          scales: {
            y: {
              beginAtZero: true
            }
          }
        }
      });
    }

    // Category Chart
    const categoryCtx = document.getElementById('categoryChart') as HTMLCanvasElement;
    if (categoryCtx && this.reporteDatosGrafico?.categorias) {
      if (this.categoryChartInstance) {
        this.categoryChartInstance.destroy();
        this.categoryChartInstance = null;
      }
      const existingCategory = Chart.getChart(categoryCtx);
      if (existingCategory) {
        existingCategory.destroy();
      }

      this.categoryChartInstance = new Chart(categoryCtx, {
        type: 'doughnut',
        data: {
          labels: this.reporteDatosGrafico.categorias.labels || [],
          datasets: [{
            data: this.reporteDatosGrafico.categorias.datos || [],
            backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40', '#FF6384', '#C9CBCF']
          }]
        },
        options: {
          responsive: true,
          plugins: {
            legend: {
              position: 'bottom'
            }
          }
        }
      });
    }
  }

  exportarPDF(): void {
    if (!this.reporteDatosTabla || this.reporteDatosTabla.length === 0) {
      this.mostrarAlerta('Primero debe generar un reporte', 'warning');
      return;
    }
    this.mostrarAlerta('Generando PDF...', 'info');
    window.open(`${this.apiUrl}/admin-menu/exportar-pdf?fechaInicio=${this.reporteFechaInicio}&fechaFin=${this.reporteFechaFin}&tipo=${this.reporteTipo}`, '_blank');
  }

  exportarExcel(): void {
    if (!this.reporteDatosTabla || this.reporteDatosTabla.length === 0) {
      this.mostrarAlerta('Primero debe generar un reporte', 'warning');
      return;
    }
    this.mostrarAlerta('Generando Excel...', 'info');
    window.open(`${this.apiUrl}/admin-menu/exportar-excel?fechaInicio=${this.reporteFechaInicio}&fechaFin=${this.reporteFechaFin}&tipo=${this.reporteTipo}`, '_blank');
  }

  // ============ MODALES ============
  cerrarModalConfirmacion(): void {
    if (this.confirmModal) {
      this.confirmModal.hide();
    }
    this.accionConfirmar = null;
    this.mensajeConfirmacion = '';
  }

  confirmarAccion(): void {
    if (this.accionConfirmar) {
      this.accionConfirmar();
    }
  }

  // ============ UTILIDADES ============
  getEstadoBadgeClass(estado: string): string {
    if (!estado) return 'bg-secondary';
    switch(estado.toUpperCase()) {
      case 'ENTREGADO': return 'bg-success';
      case 'PREPARACION': return 'bg-warning';
      case 'LISTO': return 'bg-info';
      case 'CANCELADO': return 'bg-danger';
      case 'PENDIENTE': return 'bg-secondary';
      default: return 'bg-secondary';
    }
  }

  getRolBadgeClass(rol: string): string {
    switch(rol.toLowerCase()) {
      case 'admin': return 'bg-danger';
      case 'cajero': return 'bg-warning';
      case 'cocinero': return 'bg-info';
      case 'delivery': return 'bg-primary';
      default: return 'bg-secondary';
    }
  }

  formatearFecha(fecha: string): string {
    if (!fecha) return 'N/A';
    try {
      const date = new Date(fecha);
      if (isNaN(date.getTime())) return 'N/A';
      return date.toLocaleDateString('es-PE', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return 'N/A';
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
