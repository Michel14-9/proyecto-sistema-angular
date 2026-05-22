import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';

import { CocineroService } from '../../services/cocinero';
import { AuthService } from '../../../../core/services/auth.service';
import {
  PedidoCocina,
  MetricasCocina,
  EstadoColumna,
  ColumnaConfig
} from '../../models/cocinero.models';

import { MetricasCocinaComponent } from '../../components/metricas-cocina/metricas-cocina';
import { ColumnaPedidosComponent } from '../../components/columna-pedidos/columna-pedidos';
import { DetallePedidoComponent } from '../../components/detalle-pedido/detalle-pedido';

declare var bootstrap: any;

interface UsuarioCocinero {
  id: number;
  nombres: string;
  apellidos: string;
  rol: string;
}

@Component({
  selector: 'app-dashboard-cocina',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MetricasCocinaComponent,
    ColumnaPedidosComponent,
    DetallePedidoComponent
  ],
  templateUrl: './dashboard-cocina.html',
  styleUrls: ['./dashboard-cocina.css']
})
export class DashboardCocinaComponent implements OnInit, OnDestroy {
  pedidosPorPreparar: PedidoCocina[] = [];
  pedidosEnPreparacion: PedidoCocina[] = [];
  pedidosListos: PedidoCocina[] = [];

  metricas: MetricasCocina = {
    success: true,
    totalPorPreparar: 0,
    totalEnPreparacion: 0,
    totalListosHoy: 0,
    tiempoPromedio: 0
  };

  pedidoSeleccionado: PedidoCocina | null = null;
  columnaSeleccionada: EstadoColumna | null = null;

  usuario: UsuarioCocinero | null = null;
  horaActual: string = '';
  fechaActual: string = '';

  isLoading: boolean = true;
  error: string | null = null;

  columnasConfig: ColumnaConfig[] = [
    {
      tipo: 'por-preparar',
      titulo: 'Por Preparar',
      icono: 'clock',
      colorHeader: 'bg-warning text-dark',
      textoVacio: 'No hay pedidos por preparar',
      iconoVacio: 'check-circle'
    },
    {
      tipo: 'en-preparacion',
      titulo: 'En Preparación',
      icono: 'egg-fried',
      colorHeader: 'bg-primary text-white',
      textoVacio: 'No hay pedidos en preparación',
      iconoVacio: 'egg'
    },
    {
      tipo: 'listos',
      titulo: 'Listos para Entrega',
      icono: 'check-circle',
      colorHeader: 'bg-success text-white',
      textoVacio: 'No hay pedidos listos',
      iconoVacio: 'truck'
    }
  ];

  private modalIniciar: any;
  private modalListo: any;
  pedidoParaModal: PedidoCocina | null = null;

  private refreshSubscription?: Subscription;
  private clockSubscription?: Subscription;
  private readonly REFRESH_INTERVAL = 30000;

  constructor(
    private cocineroService: CocineroService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.inicializarUsuario();
    this.inicializarModales();
    this.iniciarActualizacionHora();
    this.cargarDatosIniciales();
    this.iniciarActualizacionPeriodica();
  }

  ngOnDestroy(): void {
    this.refreshSubscription?.unsubscribe();
    this.clockSubscription?.unsubscribe();
  }

  private inicializarUsuario(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.usuario = {
        id: user.id_usuario,
        nombres: user.nombres,
        apellidos: user.apellidos,
        rol: user.rol
      };
    }
  }

  private inicializarModales(): void {
    setTimeout(() => {
      const modalIniciarEl = document.getElementById('modalIniciarPreparacion');
      const modalListoEl = document.getElementById('modalMarcarListo');

      if (modalIniciarEl) this.modalIniciar = new bootstrap.Modal(modalIniciarEl);
      if (modalListoEl) this.modalListo = new bootstrap.Modal(modalListoEl);
    }, 100);
  }

  private iniciarActualizacionHora(): void {
    this.actualizarHoraYFecha();
    this.clockSubscription = interval(1000).subscribe(() => {
      this.actualizarHoraYFecha();
    });
  }

  private actualizarHoraYFecha(): void {
    const ahora = new Date();
    this.horaActual = ahora.toLocaleTimeString('es-PE', {
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'America/Lima'
    });
    this.fechaActual = ahora.toLocaleDateString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      timeZone: 'America/Lima'
    });
  }

  private cargarDatosIniciales(): void {
    this.isLoading = true;
    this.error = null;

    this.cocineroService.cargarTodosLosPedidos().subscribe({
      next: (data) => {
        this.pedidosPorPreparar = data.porPreparar || [];
        this.pedidosEnPreparacion = data.enPreparacion || [];
        this.pedidosListos = data.listos || [];
        this.actualizarMetricasLocales();
        this.cargarMetricas();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error cargando pedidos:', error);
        this.error = error.message;
        this.isLoading = false;
        this.manejarErrorSesion(error);
      }
    });
  }

  private cargarMetricas(): void {
    this.cocineroService.getMetricasCocina().subscribe({
      next: (metricas) => {
        if (metricas.success) {
          this.metricas = metricas;
        } else {
          this.actualizarMetricasLocales();
        }
      },
      error: () => {
        this.actualizarMetricasLocales();
      }
    });
  }

  private actualizarMetricasLocales(): void {
    this.metricas = {
      success: true,
      totalPorPreparar: this.pedidosPorPreparar.length,
      totalEnPreparacion: this.pedidosEnPreparacion.length,
      totalListosHoy: this.pedidosListos.length,
      tiempoPromedio: this.calcularTiempoPromedioListos()
    };
  }

  private calcularTiempoPromedioListos(): number {
    if (this.pedidosListos.length === 0) return 0;

    let totalMinutos = 0;
    let pedidosConTiempo = 0;

    this.pedidosListos.forEach(pedido => {
      if (pedido.fecha && pedido.fechaPreparacionCompleta) {
        try {
          const fechaInicio = new Date(pedido.fecha);
          const fechaFin = new Date(pedido.fechaPreparacionCompleta);
          const minutos = Math.floor((fechaFin.getTime() - fechaInicio.getTime()) / 60000);
          if (minutos > 0 && minutos < 480) {
            totalMinutos += minutos;
            pedidosConTiempo++;
          }
        } catch {
          // Ignorar error
        }
      }
    });

    return pedidosConTiempo > 0 ? Math.round(totalMinutos / pedidosConTiempo) : 0;
  }

  private iniciarActualizacionPeriodica(): void {
    this.refreshSubscription = interval(this.REFRESH_INTERVAL)
      .pipe(
        startWith(0),
        switchMap(() => this.cocineroService.cargarTodosLosPedidos())
      )
      .subscribe({
        next: (data) => {
          this.pedidosPorPreparar = data.porPreparar || [];
          this.pedidosEnPreparacion = data.enPreparacion || [];
          this.pedidosListos = data.listos || [];
          this.actualizarMetricasLocales();

          if (this.pedidoSeleccionado) {
            const existe = [
              ...this.pedidosPorPreparar,
              ...this.pedidosEnPreparacion,
              ...this.pedidosListos
            ].some(p => p.id === this.pedidoSeleccionado?.id);

            if (!existe) this.cerrarDetalle();
          }
        },
        error: (error) => {
          console.error('Error en actualización periódica:', error);
          this.manejarErrorSesion(error);
        }
      });
  }

  private manejarErrorSesion(error: Error): void {
    if (
      error.message.includes('Sesión expirada') ||
      error.message.includes('401') ||
      error.message.includes('403')
    ) {
      this.mostrarToast('Sesión expirada. Redirigiendo al login...', 'error');
      setTimeout(() => {
        this.authService.logout();
        this.router.navigate(['/auth/login']);
      }, 2000);
    }
  }

  onSeleccionarPedido(pedido: PedidoCocina, columna: EstadoColumna): void {
    this.pedidoSeleccionado = pedido;
    this.columnaSeleccionada = columna;

    setTimeout(() => {
      document.getElementById('detalle-pedido-container')?.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest'
      });
    }, 100);
  }

  cerrarDetalle(): void {
    this.pedidoSeleccionado = null;
    this.columnaSeleccionada = null;
  }

  abrirModalIniciarPreparacion(pedido: PedidoCocina): void {
    this.pedidoParaModal = pedido;
    this.modalIniciar?.show();
  }

  abrirModalMarcarListo(pedido: PedidoCocina): void {
    this.pedidoParaModal = pedido;
    this.modalListo?.show();
  }

  confirmarIniciarPreparacion(): void {
    if (!this.pedidoParaModal) return;

    const pedidoId = this.pedidoParaModal.id;
    this.modalIniciar?.hide();

    this.cocineroService.iniciarPreparacion(pedidoId).subscribe({
      next: (response) => {
        if (response.status === 'SUCCESS') {
          this.mostrarToast('Preparación iniciada correctamente', 'success');
          this.cerrarDetalle();
          this.recargarDatos();
        } else {
          this.mostrarToast(response.message || 'Error al iniciar preparación', 'error');
        }
      },
      error: (error) => {
        this.mostrarToast(error.message, 'error');
        this.manejarErrorSesion(error);
      }
    });
  }

  confirmarMarcarListo(): void {
    if (!this.pedidoParaModal) return;

    const pedidoId = this.pedidoParaModal.id;
    this.modalListo?.hide();

    this.cocineroService.marcarComoListo(pedidoId).subscribe({
      next: (response) => {
        if (response.status === 'SUCCESS') {
          this.mostrarToast('Pedido marcado como LISTO correctamente', 'success');
          this.cerrarDetalle();
          this.recargarDatos();
        } else {
          this.mostrarToast(response.message || 'Error al marcar como listo', 'error');
        }
      },
      error: (error) => {
        this.mostrarToast(error.message, 'error');
        this.manejarErrorSesion(error);
      }
    });
  }

  recargarDatos(): void {
    this.cargarDatosIniciales();
  }

  private mostrarToast(mensaje: string, tipo: 'success' | 'error' | 'info' = 'info'): void {
    const toastEl = document.getElementById('liveAlert');
    if (!toastEl) return;

    const tituloEl = document.getElementById('toast-titulo');
    const mensajeEl = document.getElementById('toast-mensaje');
    const iconoEl = document.getElementById('toast-icon');
    const headerEl = toastEl.querySelector('.toast-header');

    const configs = {
      success: { titulo: 'Éxito', icono: 'bi-check-circle-fill', clase: 'bg-success' },
      error: { titulo: 'Error', icono: 'bi-exclamation-triangle-fill', clase: 'bg-danger' },
      info: { titulo: 'Información', icono: 'bi-info-circle-fill', clase: 'bg-info' }
    };

    const config = configs[tipo];

    if (tituloEl) tituloEl.textContent = config.titulo;
    if (mensajeEl) mensajeEl.textContent = mensaje;
    if (iconoEl) iconoEl.className = `bi ${config.icono} me-2`;
    if (headerEl) headerEl.className = `toast-header ${config.clase} text-white`;

    new bootstrap.Toast(toastEl).show();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  get nombreCompleto(): string {
    if (!this.usuario) return 'Cocinero';
    return `${this.usuario.nombres} ${this.usuario.apellidos}`.trim();
  }

  get estadoCocina(): string {
    return 'Cocina - Activa';
  }

  get pedidoModalNumero(): string {
    return this.pedidoParaModal?.numeroPedido || `#${this.pedidoParaModal?.id || ''}`;
  }
}