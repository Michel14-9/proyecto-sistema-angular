// src/app/modules/admin/admin-estadisticas/admin-estadisticas.ts
import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { KeyValuePipe } from '../../../shared/pipes/key-value.pipe';
import { ReportsService } from '../../../core/services/reports.service';
import { AlertService } from '../../../core/services/alert.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-admin-estadisticas',
  standalone: true,
  imports: [CommonModule, FormsModule, KeyValuePipe],
  templateUrl: './admin-estadisticas.html',
  styleUrls: ['./admin-estadisticas.css']
})
export class AdminEstadisticasComponent implements OnInit, AfterViewInit, OnDestroy {
  reporteCompleto: any = null;
  reporteCompletoCargado: boolean = false;

  // ✅ Filtros de fecha
  filtroPeriodo: string = 'mes'; // 'hoy', 'semana', 'mes', 'todo'
  fechaInicio: string = '';
  fechaFin: string = '';

  // ✅ Referencias a gráficos
  private chartTopCantidad: any = null;
  private chartTopMonto: any = null;
  private chartCategorias: any = null;
  private chartVentasDia: any = null;
  private chartEstados: any = null;

  constructor(
    private reportsService: ReportsService,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    this.inicializarFechas();
    this.cargarReporteCompleto();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  // ============================================================
  // ✅ INICIALIZAR FECHAS
  // ============================================================
  inicializarFechas(): void {
    const hoy = new Date();
    const primerDiaMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    this.fechaInicio = this.formatLocalDate(primerDiaMes);
    this.fechaFin = this.formatLocalDate(hoy);
  }

  formatLocalDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // ============================================================
  // ✅ CAMBIAR PERÍODO
  // ============================================================
  cambiarPeriodo(): void {
    const hoy = new Date();
    let inicio: Date = new Date();
    let fin: Date = new Date();

    switch (this.filtroPeriodo) {
      case 'hoy':
        inicio = hoy;
        fin = hoy;
        break;
      case 'semana':
        inicio = new Date(hoy);
        inicio.setDate(hoy.getDate() - 7);
        fin = hoy;
        break;
      case 'mes':
        inicio = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
        fin = hoy;
        break;
      case 'todo':
        // Fechas muy antiguas para abarcar todo
        inicio = new Date(2020, 0, 1);
        fin = hoy;
        break;
      default:
        return;
    }

    this.fechaInicio = this.formatLocalDate(inicio);
    this.fechaFin = this.formatLocalDate(fin);
    this.cargarReporteCompleto();
  }

  // ============================================================
  // ✅ CARGAR REPORTE CON FILTROS
  // ============================================================
  cargarReporteCompleto(): void {
    this.reporteCompletoCargado = false;

    // ✅ Enviar fechas al backend
    this.reportsService.getReporteCompletoConFechas(
      this.fechaInicio,
      this.fechaFin
    ).subscribe({
      next: (data: any) => {
        if (data && data.success) {
          this.reporteCompleto = data;
          this.reporteCompletoCargado = true;
          console.log('📊 Reporte completo cargado:', this.reporteCompleto);
          setTimeout(() => this.renderizarGraficos(), 300);
        } else {
          console.warn('⚠️ Error en reporte completo:', data?.error);
          this.alertService.mostrar('Error al cargar estadísticas avanzadas', 'danger');
        }
      },
      error: (error: any) => {
        this.reporteCompletoCargado = true;
        console.error('❌ Error cargando reporte completo:', error);
        this.alertService.mostrar('Error al cargar estadísticas avanzadas', 'danger');
      }
    });
  }

  // ============================================================
  // ✅ RENDERIZAR GRÁFICOS (igual que antes)
  // ============================================================
  renderizarGraficos(): void {
    if (!this.reporteCompleto) return;
    this.renderizarTopCantidad();
    this.renderizarTopMonto();
    this.renderizarCategorias();
    this.renderizarVentasDia();
    this.renderizarEstados();
  }

  renderizarTopCantidad(): void {
    const ctx = document.getElementById('chartTopCantidad') as HTMLCanvasElement;
    if (!ctx || !this.reporteCompleto?.top_productos_cantidad?.length) return;
    this.destroyChart('chartTopCantidad');

    const data = this.reporteCompleto.top_productos_cantidad;
    const labels = data.map((item: any) => item.nombre);
    const values = data.map((item: any) => item.cantidad);
    const colores = ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40', '#FF6384', '#C9CBCF', '#FF6B6B', '#4ECDC4'];

    this.chartTopCantidad = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Cantidad Vendida',
          data: values,
          backgroundColor: colores.slice(0, data.length),
          borderColor: colores.slice(0, data.length),
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
      }
    });
  }

  renderizarTopMonto(): void {
    const ctx = document.getElementById('chartTopMonto') as HTMLCanvasElement;
    if (!ctx || !this.reporteCompleto?.top_productos_monto?.length) return;
    this.destroyChart('chartTopMonto');

    const data = this.reporteCompleto.top_productos_monto;
    const labels = data.map((item: any) => item.nombre);
    const values = data.map((item: any) => item.monto);
    const colores = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9'];

    this.chartTopMonto = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Monto Total (S/)',
          data: values,
          backgroundColor: colores.slice(0, data.length),
          borderColor: colores.slice(0, data.length),
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { callback: (value: any) => 'S/ ' + value.toFixed(2) }
          }
        }
      }
    });
  }

  renderizarCategorias(): void {
    const ctx = document.getElementById('chartCategorias') as HTMLCanvasElement;
    if (!ctx || !this.reporteCompleto?.ventas_categoria) return;
    this.destroyChart('chartCategorias');

    const entries = Object.entries(this.reporteCompleto.ventas_categoria);
    if (entries.length === 0) return;

    const labels = entries.map(([key]) => key);
    const values = entries.map(([, value]) => value as number);
    const colores = ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40', '#FF6384', '#C9CBCF', '#FF6B6B', '#4ECDC4'];

    this.chartCategorias = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: colores.slice(0, labels.length),
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 10, font: { size: 11 } } }
        }
      }
    });
  }

  renderizarVentasDia(): void {
    const ctx = document.getElementById('chartVentasDia') as HTMLCanvasElement;
    if (!ctx || !this.reporteCompleto?.ventas_por_dia) return;
    this.destroyChart('chartVentasDia');

    const entries = Object.entries(this.reporteCompleto.ventas_por_dia);
    if (entries.length === 0) return;

    const labels = entries.map(([key]) => key);
    const values = entries.map(([, value]) => value as number);

    this.chartVentasDia = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Ventas (S/)',
          data: values,
          borderColor: '#36A2EB',
          backgroundColor: 'rgba(54, 162, 235, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.4,
          pointBackgroundColor: '#36A2EB',
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
          pointRadius: 5
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { callback: (value: any) => 'S/ ' + value.toFixed(2) }
          }
        }
      }
    });
  }

  renderizarEstados(): void {
    const ctx = document.getElementById('chartEstados') as HTMLCanvasElement;
    if (!ctx || !this.reporteCompleto?.estados) return;
    this.destroyChart('chartEstados');

    const entries = Object.entries(this.reporteCompleto.estados);
    if (entries.length === 0) return;

    const labels = entries.map(([key]) => key);
    const values = entries.map(([, value]) => value as number);
    const colores = {
      'PAGADO': '#36A2EB',
      'ENTREGADO': '#4BC0C0',
      'PREPARACION': '#FFCE56',
      'PENDIENTE': '#FF9F40',
      'CANCELADO': '#FF6384',
      'LISTO': '#9966FF'
    };
    const backgroundColors = labels.map(label => colores[label as keyof typeof colores] || '#C9CBCF');

    this.chartEstados = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: backgroundColors,
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 10, font: { size: 11 } } }
        }
      }
    });
  }

  // ============================================================
  // ✅ DESTRUIR GRÁFICOS
  // ============================================================
  destroyChart(chartId: string): void {
    const chartMap: { [key: string]: any } = {
      'chartTopCantidad': this.chartTopCantidad,
      'chartTopMonto': this.chartTopMonto,
      'chartCategorias': this.chartCategorias,
      'chartVentasDia': this.chartVentasDia,
      'chartEstados': this.chartEstados
    };

    if (chartMap[chartId]) {
      chartMap[chartId].destroy();
      chartMap[chartId] = null;
    }
  }

  destroyCharts(): void {
    const chartIds = ['chartTopCantidad', 'chartTopMonto', 'chartCategorias', 'chartVentasDia', 'chartEstados'];
    chartIds.forEach(id => this.destroyChart(id));
  }
}
