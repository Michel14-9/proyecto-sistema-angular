import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { PedidoService } from '../../core/services/pedido';

interface ResumenVentas {
  total_ventas: number;
  cantidad_pedidos: number;
  ticket_promedio: number;
  ventas_por_dia: { fecha: string; total: number }[];
  productos_mas_vendidos: { nombre: string; cantidad: number; total: number }[];
}

@Component({
  selector: 'app-reporte-ventas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reporte-ventas.html',
  styleUrls: ['./reporte-ventas.css']
})
export class ReporteVentasComponent implements OnInit {
  filtroForm: FormGroup;
  resumen: ResumenVentas | null = null;
  isLoading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private pedidoService: PedidoService
  ) {
    const hoy = new Date();
    const hace30Dias = new Date();
    hace30Dias.setDate(hoy.getDate() - 30);

    this.filtroForm = this.fb.group({
      fecha_inicio: [this.formatDate(hace30Dias)],
      fecha_fin: [this.formatDate(hoy)]
    });
  }

  ngOnInit(): void {
    this.generarReporte();
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  generarReporte(): void {
    if (this.filtroForm.invalid) return;

    this.isLoading = true;
    const { fecha_inicio, fecha_fin } = this.filtroForm.value;

    this.pedidoService.getReporteVentas(fecha_inicio, fecha_fin).subscribe({
      next: (data) => {
        this.resumen = data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error generando reporte:', error);
        this.isLoading = false;
        alert('Error al generar el reporte');
      }
    });
  }

  exportarExcel(): void {
    const { fecha_inicio, fecha_fin } = this.filtroForm.value;
    this.pedidoService.exportarReporteExcel(fecha_inicio, fecha_fin).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `reporte-ventas-${fecha_inicio}-${fecha_fin}.xlsx`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        alert('Error al exportar el reporte');
      }
    });
  }
}