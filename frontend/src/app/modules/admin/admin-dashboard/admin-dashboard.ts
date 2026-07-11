// src/app/modules/admin/admin-dashboard/admin-dashboard.ts
import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card';
import { DashboardService } from '../../../core/services/dashboard.service';
import { PredictionsService } from '../../../core/services/predictions.service';
import { AlertService } from '../../../core/services/alert.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, StatCardComponent],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css']
})
export class AdminDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  estadisticas = {
    totalProductos: 0,
    totalUsuarios: 0,
    pedidosHoy: 0,
    ingresosHoy: 0,
    ventasMesTotal: 0,
    promedioDiario: 0,
    ventaMaxima: 0,
    totalPedidos: 0
  };
  ventasRecientes: any[] = [];
  tieneDatosVentas: boolean = false;
  salesChartInstance: any = null;

  // Predicciones
  predicciones: any[] = [];
  prediccionesCargadas: boolean = false;
  tendencias: any = null;
  modeloEntrenado: boolean = false;
  entrenandoModelo: boolean = false;
  diasPrediccion: number = 7;
  predictionChartInstance: any = null;

  constructor(
    private dashboardService: DashboardService,
    private predictionsService: PredictionsService,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    this.cargarDashboard();
    this.cargarPredicciones();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.cargarDatosGraficoVentas(), 500);
  }

  ngOnDestroy(): void {
    if (this.salesChartInstance) {
      this.salesChartInstance.destroy();
      this.salesChartInstance = null;
    }
    if (this.predictionChartInstance) {
      this.predictionChartInstance.destroy();
      this.predictionChartInstance = null;
    }
  }

  cargarDashboard(): void {
    this.cargarEstadisticas();
    this.cargarVentasRecientes();
  }

  cargarEstadisticas(): void {
    this.dashboardService.getEstadisticas().subscribe({
      next: (data: any) => {
        if (data?.success) {
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
      error: () => this.alertService.mostrar('Error al cargar estadísticas', 'danger')
    });
  }

  cargarVentasRecientes(): void {
    this.dashboardService.getVentasRecientes().subscribe({
      next: (data: any) => {
        if (data?.success) {
          this.ventasRecientes = data.data || [];
        }
      }
    });
  }

  cargarDatosGraficoVentas(): void {
    this.dashboardService.getEstadisticasVentas().subscribe({
      next: (data: any) => {
        if (data?.success && data.ventasPorDia) {
          const ventasPorDia = data.ventasPorDia;
          this.tieneDatosVentas = Object.values(ventasPorDia).some((v: any) => v > 0);
          if (this.tieneDatosVentas) {
            setTimeout(() => this.actualizarGraficoVentas(ventasPorDia), 300);
          }
        }
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

    this.salesChartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels: Object.keys(ventasPorDia),
        datasets: [{
          label: 'Ventas (S/)',
          data: Object.values(ventasPorDia),
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
          legend: { display: true, position: 'top' },
          tooltip: {
            callbacks: {
              label: (ctx: any) => `Ventas: S/ ${ctx.parsed.y.toFixed(2)}`
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { callback: (value: any) => 'S/ ' + value.toFixed(2) }
          }
        }
      }
    });
  }

  // ===== PREDICCIONES =====
  cargarPredicciones(): void {
    this.predictionsService.getPredicciones(this.diasPrediccion).subscribe({
      next: (data: any) => {
        if (data?.success) {
          this.predicciones = data.predicciones || [];
          this.prediccionesCargadas = true;
          this.modeloEntrenado = true;
          setTimeout(() => this.actualizarGraficoPredicciones(), 300);
        } else if (data?.message?.includes('no está entrenado')) {
          this.modeloEntrenado = false;
        }
      },
      error: () => {
        this.modeloEntrenado = false;
        this.prediccionesCargadas = false;
      }
    });

    this.predictionsService.getTendencias().subscribe({
      next: (data: any) => {
        if (data?.success) this.tendencias = data;
      }
    });
  }

  entrenarModelo(): void {
    if (this.entrenandoModelo) return;
    this.entrenandoModelo = true;
    this.alertService.mostrar('Entrenando modelo...', 'info');

    this.predictionsService.entrenarModelo().subscribe({
      next: (data: any) => {
        this.entrenandoModelo = false;
        if (data?.success) {
          this.modeloEntrenado = true;
          this.alertService.mostrar(
            `✅ Modelo entrenado. Precisión: ${data.precision}%`,
            'success'
          );
          this.cargarPredicciones();
        }
      },
      error: () => {
        this.entrenandoModelo = false;
        this.alertService.mostrar('Error al entrenar modelo', 'danger');
      }
    });
  }

  actualizarGraficoPredicciones(): void {
    const ctx = document.getElementById('predictionChart') as HTMLCanvasElement;
    if (!ctx || !this.predicciones?.length) return;

    if (this.predictionChartInstance) {
      this.predictionChartInstance.destroy();
      this.predictionChartInstance = null;
    }

    const labels = this.predicciones.map((p: any) => {
      const fecha = new Date(p.fecha);
      return fecha.toLocaleDateString('es-ES', { day: '2-digit', month: 'short' });
    });
    const data = this.predicciones.map((p: any) => p.ventas_estimadas || 0);

    this.predictionChartInstance = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Ventas Estimadas (S/)',
          data: data,
          backgroundColor: data.map(v =>
            v > 100 ? 'rgba(40, 167, 69, 0.7)' :
            v > 50 ? 'rgba(255, 193, 7, 0.7)' :
            'rgba(108, 117, 125, 0.7)'
          ),
          borderColor: data.map(v =>
            v > 100 ? '#28a745' :
            v > 50 ? '#ffc107' :
            '#6c757d'
          ),
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: true, position: 'top' },
          tooltip: {
            callbacks: {
              label: (ctx: any) => `S/ ${ctx.parsed.y.toFixed(2)}`
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { callback: (value: any) => 'S/ ' + value.toFixed(2) }
          }
        }
      }
    });
  }

  calcularTotalEstimado(): number {
    return this.predicciones?.reduce((sum, p) => sum + (p.ventas_estimadas || 0), 0) || 0;
  }

  calcularPromedioEstimado(): number {
    return this.predicciones?.length ? this.calcularTotalEstimado() / this.predicciones.length : 0;
  }

  formatearFecha(fecha: string): string {
    if (!fecha) return 'N/A';
    try {
      const date = new Date(fecha);
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

  getEstadoBadgeClass(estado: string): string {
    const clases: any = {
      'ENTREGADO': 'bg-success',
      'PREPARACION': 'bg-warning',
      'LISTO': 'bg-info',
      'CANCELADO': 'bg-danger',
      'PENDIENTE': 'bg-secondary'
    };
    return clases[estado?.toUpperCase()] || 'bg-secondary';
  }
}
