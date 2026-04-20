import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, forkJoin } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { PedidoCocina, MetricasCocina, RespuestaOperacion } from '../models/cocinero.models';

@Injectable({
  providedIn: 'root'
})
export class CocineroService {
  private readonly baseUrl = '/cocinero';
  private readonly timeoutMs = 30000;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json'
    });
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('Error en CocineroService:', error);
    
    let errorMessage = 'Error de conexión con el servidor';
    
    if (error.status === 401 || error.status === 403) {
      errorMessage = 'Sesión expirada. Por favor, inicie sesión nuevamente.';
    } else if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }
    
    return throwError(() => new Error(errorMessage));
  }

  cargarTodosLosPedidos(): Observable<{
    porPreparar: PedidoCocina[];
    enPreparacion: PedidoCocina[];
    listos: PedidoCocina[];
  }> {
    return forkJoin({
      porPreparar: this.getPedidosPorPreparar(),
      enPreparacion: this.getPedidosEnPreparacion(),
      listos: this.getPedidosListosHoy()
    });
  }

  getPedidosPorPreparar(): Observable<PedidoCocina[]> {
    return this.http.get<PedidoCocina[]>(`${this.baseUrl}/pedidos-por-preparar`, {
      headers: this.getHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      catchError(this.handleError)
    );
  }

  getPedidosEnPreparacion(): Observable<PedidoCocina[]> {
    return this.http.get<PedidoCocina[]>(`${this.baseUrl}/pedidos-en-preparacion`, {
      headers: this.getHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      catchError(this.handleError)
    );
  }

  getPedidosListosHoy(): Observable<PedidoCocina[]> {
    return this.http.get<PedidoCocina[]>(`${this.baseUrl}/pedidos-listos-hoy`, {
      headers: this.getHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      catchError(this.handleError)
    );
  }

  getMetricasCocina(): Observable<MetricasCocina> {
    return this.http.get<MetricasCocina>(`${this.baseUrl}/metricas-cocina`, {
      headers: this.getHeaders()
    }).pipe(
      timeout(this.timeoutMs),
      catchError(this.handleError)
    );
  }

  iniciarPreparacion(pedidoId: number): Observable<RespuestaOperacion> {
    return this.http.post<RespuestaOperacion>(
      `${this.baseUrl}/iniciar-preparacion/${pedidoId}`,
      {},
      { headers: this.getHeaders() }
    ).pipe(
      timeout(this.timeoutMs),
      catchError(this.handleError)
    );
  }

  marcarComoListo(pedidoId: number): Observable<RespuestaOperacion> {
    return this.http.post<RespuestaOperacion>(
      `${this.baseUrl}/marcar-listo/${pedidoId}`,
      {},
      { headers: this.getHeaders() }
    ).pipe(
      timeout(this.timeoutMs),
      catchError(this.handleError)
    );
  }
}