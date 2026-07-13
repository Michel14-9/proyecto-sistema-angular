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

  getReporteCompletoConFechas(fechaInicio: string, fechaFin: string): Observable<any> {
    return this.api.pythonGet(`/api/reportes/completo?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`);
  }

  getReporteCompleto(): Observable<any> {
    return this.api.pythonGet('/api/reportes/completo');
  }

  // 🔥 CORREGIDO: Usar la URL correcta de Java
  exportarPDF(fechaInicio: string, fechaFin: string, tipo: string): string {
    // 🔥 Verificar que las fechas existan
    let url = 'http://localhost:8080/admin/exportar-pdf';
    const params = new URLSearchParams();

    if (fechaInicio) {
      params.set('fechaInicio', fechaInicio);
    }
    if (fechaFin) {
      params.set('fechaFin', fechaFin);
    }
    if (tipo) {
      params.set('tipo', tipo);
    }

    const queryString = params.toString();
    if (queryString) {
      url += '?' + queryString;
    }

    console.log('📄 URL PDF generada:', url);
    return url;
  }

  // 🔥 CORREGIDO: Usar la URL correcta de Java
  exportarExcel(fechaInicio: string, fechaFin: string, tipo: string): string {
    let url = 'http://localhost:8080/admin/exportar-excel';
    const params = new URLSearchParams();

    if (fechaInicio) {
      params.set('fechaInicio', fechaInicio);
    }
    if (fechaFin) {
      params.set('fechaFin', fechaFin);
    }
    if (tipo) {
      params.set('tipo', tipo);
    }

    const queryString = params.toString();
    if (queryString) {
      url += '?' + queryString;
    }

    console.log('📄 URL Excel generada:', url);
    return url;
  }
}
