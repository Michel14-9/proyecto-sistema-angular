// src/app/core/services/reports.service.ts
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class ReportsService {
  constructor(private api: ApiService) {}

  getReporteVentas(fechaInicio: string, fechaFin: string, tipo: string = 'ventas'): Observable<any> {
    return this.api.pythonGet(`/api/reportes/ventas?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}&tipo=${tipo}`);
  }

  // ✅ Método con fechas (para filtrar por período)
  getReporteCompletoConFechas(fechaInicio: string, fechaFin: string): Observable<any> {
    return this.api.pythonGet(`/api/reportes/completo?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`);
  }

  // ✅ Método sin fechas (histórico completo) - SOLO UNO
  getReporteCompleto(): Observable<any> {
    return this.api.pythonGet('/api/reportes/completo');
  }

  exportarPDF(fechaInicio: string, fechaFin: string, tipo: string): string {
    return `${this.api['baseUrlJava']}/admin/exportar-pdf?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}&tipo=${tipo}`;
  }

  exportarExcel(fechaInicio: string, fechaFin: string, tipo: string): string {
    return `${this.api['baseUrlJava']}/admin/exportar-excel?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}&tipo=${tipo}`;
  }
}
