// src/app/modules/admin/admin-reportes/admin-reportes.ts
import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReportsService } from '../../../core/services/reports.service';
import { AlertService } from '../../../core/services/alert.service';
import { Chart, registerables } from 'chart.js';
import { KeyValuePipe } from '../../../shared/pipes/key-value.pipe';

Chart.register(...registerables);

@Component({
  selector: 'app-admin-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule, KeyValuePipe],
  templateUrl: './admin-reportes.html',
  styleUrls: ['./admin-reportes.css']
})
export class AdminReportesComponent implements OnInit, AfterViewInit, OnDestroy {
  reporteTipo: string = 'ventas';
  reporteRango: string = 'hoy';
  reporteFechaInicio: string = '';
  reporteFechaFin: string = '';
  reporteMetricas: any = { totalVentas: 0, totalPedidos: 0, productosVendidos: 0, crecimiento: 0 };
  reporteDatosGrafico: any = null;
  reporteDatosTabla: any[] = [];
  reporteColumnas: string[] = [];
  reporteTituloGrafico: string = 'Ventas por Día';
  reporteTituloTabla: string = 'Detalle de Ventas';
  reportChartInstance: any = null;
  categoryChartInstance: any = null;

  // ✅ Mapeo de títulos para todos los tipos
  private readonly titulosReportes: { [key: string]: { grafico: string, tabla: string } } = {
    'ventas': { grafico: 'Ventas por Día', tabla: 'Detalle de Ventas' },
    'productos': { grafico: 'Productos Más Vendidos', tabla: 'Productos Más Vendidos' },
    'usuarios': { grafico: 'Actividad de Usuarios', tabla: 'Actividad de Usuarios' },
    'pedidos': { grafico: 'Estadísticas de Pedidos', tabla: 'Estadísticas de Pedidos' },
    'metodos-pago': { grafico: 'Métodos de Pago', tabla: 'Detalle de Métodos de Pago' },
    'tipos-entrega': { grafico: 'Tipos de Entrega', tabla: 'Detalle de Tipos de Entrega' },
    'horarios': { grafico: 'Horarios Pico', tabla: 'Detalle de Horarios' },
    'favoritos': { grafico: 'Productos Favoritos', tabla: 'Productos Favoritos' }
  };

  constructor(
    private reportsService: ReportsService,
    private alertService: AlertService
  ) {
    console.log('✅ AdminReportesComponent constructor');
  }

  ngOnInit(): void {
    console.log('✅ AdminReportesComponent ngOnInit');
    this.inicializarFechas();
  }

  ngAfterViewInit(): void {
    console.log('✅ AdminReportesComponent ngAfterViewInit');
  }

  ngOnDestroy(): void {
    console.log('✅ AdminReportesComponent ngOnDestroy');
    if (this.reportChartInstance) {
      this.reportChartInstance.destroy();
      this.reportChartInstance = null;
    }
    if (this.categoryChartInstance) {
      this.categoryChartInstance.destroy();
      this.categoryChartInstance = null;
    }
  }

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
    switch (this.reporteRango) {
      case 'hoy': startDate = hoy; endDate = hoy; break;
      case 'ayer': startDate = new Date(hoy); startDate.setDate(hoy.getDate() - 1); endDate = startDate; break;
      case 'semana': startDate = new Date(hoy); startDate.setDate(hoy.getDate() - hoy.getDay()); endDate = hoy; break;
      case 'mes': startDate = new Date(hoy.getFullYear(), hoy.getMonth(), 1); endDate = hoy; break;
      case 'personalizado': return;
      default: return;
    }
    this.reporteFechaInicio = this.formatLocalDate(startDate);
    this.reporteFechaFin = this.formatLocalDate(endDate);
  }

  cambiarTipoReporte(): void {
    if (this.titulosReportes[this.reporteTipo]) {
      this.reporteTituloGrafico = this.titulosReportes[this.reporteTipo].grafico;
      this.reporteTituloTabla = this.titulosReportes[this.reporteTipo].tabla;
    }
  }

  generarReporte(): void {
    if (!this.reporteFechaInicio || !this.reporteFechaFin) {
      this.alertService.mostrar('Seleccione un rango de fechas', 'warning');
      return;
    }
    if (new Date(this.reporteFechaInicio) > new Date(this.reporteFechaFin)) {
      this.alertService.mostrar('La fecha de inicio no puede ser mayor a la fecha fin', 'warning');
      return;
    }

    this.alertService.mostrar('Generando reporte...', 'info');

    this.reportsService.getReporteVentas(
      this.reporteFechaInicio,
      this.reporteFechaFin,
      this.reporteTipo
    ).subscribe({
      next: (data: any) => {
        console.log('📊 Datos recibidos del backend:', data);

        if (data && data.success) {
          // ✅ Actualizar métricas según el tipo
          this.actualizarMetricas(data);

          // ✅ Actualizar datos del gráfico
          if (data.datos_grafico) {
            this.reporteDatosGrafico = {
              labels: data.datos_grafico.labels || [],
              datos: data.datos_grafico.datos || [],
              categorias: {
                labels: data.categorias?.labels || [],
                datos: data.categorias?.datos || []
              }
            };
          } else {
            this.reporteDatosGrafico = this.procesarDatosLocalmente(data);
          }

          // ✅ Actualizar columnas
          this.actualizarColumnas();

          this.reporteDatosTabla = data.data || [];

          setTimeout(() => this.actualizarGraficosReporte(), 300);
          this.alertService.mostrar('Reporte generado', 'success');
        } else {
          this.alertService.mostrar(data.error || 'Error al generar reporte', 'danger');
        }
      },
      error: (error: any) => {
        console.error('❌ Error generando reporte:', error);
        this.alertService.mostrar('Error al generar reporte', 'danger');
      }
    });
  }

  // ============================================================
  // ✅ ACTUALIZAR MÉTRICAS SEGÚN EL TIPO
  // ============================================================
  actualizarMetricas(data: any): void {
    const metricas = data.metricas || {};

    switch (this.reporteTipo) {
      case 'ventas':
        this.reporteMetricas = {
          totalVentas: metricas.totalVentas || 0,
          totalPedidos: metricas.totalPedidos || 0,
          productosVendidos: metricas.productosVendidos || 0,
          crecimiento: metricas.crecimiento || 0
        };
        break;

      case 'productos':
        this.reporteMetricas = {
          totalVentas: metricas.totalVentas || 0,
          totalPedidos: metricas.totalPedidos || 0,
          productosVendidos: metricas.productosVendidos || 0,
          crecimiento: 0
        };
        break;

      case 'pedidos':
        this.reporteMetricas = {
          totalVentas: metricas.totalVentas || 0,
          totalPedidos: metricas.totalPedidos || 0,
          productosVendidos: 0,
          crecimiento: 0,
          estados: data.estados || {}
        };
        break;

      case 'usuarios':
        this.reporteMetricas = {
          totalVentas: metricas.totalVentas || 0,
          totalPedidos: metricas.totalPedidos || 0,
          productosVendidos: 0,
          crecimiento: 0,
          totalUsuarios: data.data?.length || 0
        };
        break;

      case 'metodos-pago':
        this.reporteMetricas = {
          totalVentas: metricas.totalVentas || 0,
          totalPedidos: metricas.totalPedidos || 0,
          metodosUsados: metricas.metodosUsados || 0,
          crecimiento: 0
        };
        break;

      case 'tipos-entrega':
        this.reporteMetricas = {
          totalVentas: metricas.totalVentas || 0,
          totalPedidos: metricas.totalPedidos || 0,
          tiposUsados: metricas.tiposUsados || 0,
          crecimiento: 0
        };
        break;

      case 'horarios':
        this.reporteMetricas = {
          totalVentas: metricas.totalVentas || 0,
          totalPedidos: metricas.totalPedidos || 0,
          horaPico: metricas.horaPico || 'N/A',
          pedidosHoraPico: metricas.pedidosHoraPico || 0
        };
        break;

      case 'favoritos':
        this.reporteMetricas = {
          totalFavoritos: metricas.totalFavoritos || 0,
          totalProductosFavoritos: metricas.totalProductosFavoritos || 0,
          topProducto: metricas.topProducto || 'N/A',
          topCantidad: metricas.topCantidad || 0
        };
        break;

      default:
        this.reporteMetricas = metricas;
    }
  }

  // ============================================================
  // ✅ ACTUALIZAR COLUMNAS SEGÚN EL TIPO
  // ============================================================
  actualizarColumnas(): void {
    switch (this.reporteTipo) {
      case 'productos':
        this.reporteColumnas = ['producto', 'cantidad', 'monto'];
        break;
      case 'usuarios':
        this.reporteColumnas = ['usuario', 'pedidos'];
        break;
      case 'pedidos':
        this.reporteColumnas = ['estado', 'cantidad'];
        break;
      case 'metodos-pago':
        this.reporteColumnas = ['metodo', 'cantidad', 'monto'];
        break;
      case 'tipos-entrega':
        this.reporteColumnas = ['tipo', 'cantidad', 'monto'];
        break;
      case 'horarios':
        this.reporteColumnas = ['hora', 'pedidos', 'monto'];
        break;
      case 'favoritos':
        this.reporteColumnas = ['producto', 'favoritos'];
        break;
      default:
        this.reporteColumnas = ['id', 'fecha', 'cliente', 'total', 'estado'];
    }
  }

  // ============================================================
  // ✅ MÉTODO FALLBACK PARA PROCESAR DATOS LOCALMENTE
  // ============================================================
  procesarDatosLocalmente(data: any): any {
    if (this.reporteTipo === 'productos') {
      const productosMap = new Map();
      data.data.forEach((item: any) => {
        if (item.productos) {
          const nombres = item.productos.split(', ');
          nombres.forEach((nombre: string) => {
            if (nombre && !nombre.includes('...')) {
              productosMap.set(nombre, (productosMap.get(nombre) || 0) + 1);
            }
          });
        }
      });

      const productosOrdenados = Array.from(productosMap.entries())
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10);

      return {
        labels: productosOrdenados.map(([nombre]) => nombre),
        datos: productosOrdenados.map(([, cantidad]) => cantidad),
        categorias: this.calcularCategoriasProductos(data.data)
      };
    }

    if (this.reporteTipo === 'ventas') {
      const labels = data.data.map((item: any) => {
        const fecha = new Date(item.fecha);
        return fecha.toLocaleDateString('es-ES', { day: '2-digit', month: 'short' });
      });
      const values = data.data.map((item: any) => item.total || 0);

      return {
        labels: labels,
        datos: values,
        categorias: {
          labels: ['Ventas', 'Pedidos'],
          datos: [data.metricas?.totalVentas || 0, data.metricas?.totalPedidos || 0]
        }
      };
    }

    if (this.reporteTipo === 'pedidos' || this.reporteTipo === 'metodos-pago' ||
        this.reporteTipo === 'tipos-entrega' || this.reporteTipo === 'horarios' ||
        this.reporteTipo === 'favoritos') {
      return {
        labels: data.datos_grafico?.labels || [],
        datos: data.datos_grafico?.datos || [],
        categorias: data.categorias || { labels: [], datos: [] }
      };
    }

    if (this.reporteTipo === 'usuarios') {
      const usuariosMap = new Map();
      data.data.forEach((item: any) => {
        const cliente = item.cliente || 'Cliente general';
        usuariosMap.set(cliente, (usuariosMap.get(cliente) || 0) + 1);
      });

      const usuariosOrdenados = Array.from(usuariosMap.entries())
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10);

      return {
        labels: usuariosOrdenados.map(([nombre]) => nombre),
        datos: usuariosOrdenados.map(([, pedidos]) => pedidos),
        categorias: {
          labels: ['Activos', 'Inactivos'],
          datos: [usuariosMap.size, 0]
        }
      };
    }

    return {
      labels: [],
      datos: [],
      categorias: { labels: [], datos: [] }
    };
  }

  // ============================================================
  // ✅ MÉTODO PARA CALCULAR CATEGORÍAS DE PRODUCTOS
  // ============================================================
  calcularCategoriasProductos(data: any[]): any {
    const categoriasMap = new Map();

    data.forEach((item: any) => {
      if (item.productos) {
        const nombres = item.productos.split(', ');
        nombres.forEach((nombre: string) => {
          if (nombre && !nombre.includes('...')) {
            let categoria = 'Otros';
            const nombreLower = nombre.toLowerCase();
            if (nombreLower.includes('pollo') || nombreLower.includes('broaster')) {
              categoria = 'Pollos';
            } else if (nombreLower.includes('parrilla') || nombreLower.includes('churrasco')) {
              categoria = 'Parrillas';
            } else if (nombreLower.includes('chicharrón') || nombreLower.includes('chicharron')) {
              categoria = 'Chicharrón';
            } else if (nombreLower.includes('hamburguesa')) {
              categoria = 'Hamburguesas';
            } else if (nombreLower.includes('combo') || nombreLower.includes('familiar')) {
              categoria = 'Combos';
            } else if (nombreLower.includes('criollo') || nombreLower.includes('guiso')) {
              categoria = 'Criollos';
            }
            categoriasMap.set(categoria, (categoriasMap.get(categoria) || 0) + 1);
          }
        });
      }
    });

    if (categoriasMap.size === 0) {
      return {
        labels: ['Pollos', 'Parrillas', 'Chicharrón', 'Broaster', 'Hamburguesas', 'Criollos', 'Combos'],
        datos: [0, 0, 0, 0, 0, 0, 0]
      };
    }

    const labels = Array.from(categoriasMap.keys());
    const datos = Array.from(categoriasMap.values());

    return { labels, datos };
  }

  actualizarGraficosReporte(): void {
    const reportCtx = document.getElementById('reportChart') as HTMLCanvasElement;
    if (reportCtx && this.reporteDatosGrafico) {
      if (this.reportChartInstance) {
        this.reportChartInstance.destroy();
        this.reportChartInstance = null;
      }

      // ✅ Tipos que usan gráfico de barras
      const tiposBarra = ['productos', 'usuarios', 'metodos-pago', 'tipos-entrega', 'favoritos'];
      const isBar = tiposBarra.includes(this.reporteTipo);

      // ✅ Tipos que usan formato de moneda
      const tiposMoneda = ['ventas', 'pedidos', 'metodos-pago', 'tipos-entrega'];
      const isMoneda = tiposMoneda.includes(this.reporteTipo);

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
            legend: { display: true, position: 'top' }
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: {
                callback: (value: any) => {
                  if (isMoneda) {
                    return 'S/ ' + Number(value).toFixed(2);
                  }
                  return value;
                }
              }
            }
          }
        }
      });
    }

    const categoryCtx = document.getElementById('categoryChart') as HTMLCanvasElement;
    if (categoryCtx && this.reporteDatosGrafico?.categorias) {
      if (this.categoryChartInstance) {
        this.categoryChartInstance.destroy();
        this.categoryChartInstance = null;
      }

      const categorias = this.reporteDatosGrafico.categorias;
      if (categorias.labels && categorias.labels.length > 0) {
        this.categoryChartInstance = new Chart(categoryCtx, {
          type: 'doughnut',
          data: {
            labels: categorias.labels,
            datasets: [{
              data: categorias.datos,
              backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40', '#FF6384', '#C9CBCF']
            }]
          },
          options: {
            responsive: true,
            plugins: { legend: { position: 'bottom' } }
          }
        });
      }
    }
  }

  exportarPDF(): void {
    if (!this.reporteDatosTabla?.length) {
      this.alertService.mostrar('Primero genere un reporte', 'warning');
      return;
    }
    this.alertService.mostrar('Generando PDF...', 'info');
    window.open(this.reportsService.exportarPDF(this.reporteFechaInicio, this.reporteFechaFin, this.reporteTipo), '_blank');
  }

  exportarExcel(): void {
    if (!this.reporteDatosTabla?.length) {
      this.alertService.mostrar('Primero genere un reporte', 'warning');
      return;
    }
    this.alertService.mostrar('Generando Excel...', 'info');
    window.open(this.reportsService.exportarExcel(this.reporteFechaInicio, this.reporteFechaFin, this.reporteTipo), '_blank');
  }
}
