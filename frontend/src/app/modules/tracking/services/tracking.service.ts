import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval, switchMap, takeWhile } from 'rxjs';
import { TrackingResponse, UpdateLocationRequest } from '../models/tracking.models';

@Injectable({ providedIn: 'root' })
export class TrackingService {

  private apiUrl = 'http://localhost:8080/api/tracking';

  constructor(private http: HttpClient) {}

  obtenerTracking(numeroPedido: string): Observable<TrackingResponse> {
    return this.http.get<TrackingResponse>(`${this.apiUrl}/${numeroPedido}`);
  }

  actualizarUbicacion(data: UpdateLocationRequest): Observable<TrackingResponse> {
    return this.http.post<TrackingResponse>(`${this.apiUrl}/ubicacion`, data);
  }

  /**
   * Polling: consulta el tracking cada X segundos mientras el estado sea ACTIVE.
   * Úsalo en el componente del cliente para actualizar el mapa automáticamente.
   */
  trackingEnVivo(numeroPedido: string, intervaloMs: number = 5000): Observable<TrackingResponse> {
    return interval(intervaloMs).pipe(
      switchMap(() => this.obtenerTracking(numeroPedido)),
      takeWhile(t => t.estado !== 'COMPLETED' && t.estado !== 'CANCELLED', true)
    );
  }
}