import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  constructor(private api: ApiService) {}

  getEstadisticas(): Observable<any> {
    return this.api.pythonGet('/api/dashboard/estadisticas');
  }

  getVentasRecientes(): Observable<any> {
    return this.api.pythonGet('/api/dashboard/ventas-recientes');
  }

  getEstadisticasVentas(): Observable<any> {
    return this.api.pythonGet('/api/dashboard/estadisticas-ventas');
  }

  getReporteVentas(fechaInicio: string, fechaFin: string): Observable<any> {
    return this.api.pythonGet(`/api/reportes/ventas?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`);
  }
}
