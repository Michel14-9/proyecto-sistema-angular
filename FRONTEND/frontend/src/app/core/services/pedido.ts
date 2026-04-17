import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DetallePedido {
  id_producto: number;
  cantidad: number;
  precio_unitario: number;
}

export interface PedidoRequest {
  detalles: DetallePedido[];
  notas?: string;
  direccion_entrega?: string;
}

export interface Pedido {
  id_pedido: number;
  fecha_pedido: string;
  estado: string;
  total: number;
  detalles: any[];
}

@Injectable({
  providedIn: 'root'
})
export class PedidoService {
  private apiUrl = 'http://localhost:8080/api/pedidos';

  constructor(private http: HttpClient) {}

  getMisPedidos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.apiUrl}/mis-pedidos`);
  }

  getPedido(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.apiUrl}/${id}`);
  }

  crearPedido(pedido: PedidoRequest): Observable<Pedido> {
    return this.http.post<Pedido>(this.apiUrl, pedido);
  }

  actualizarEstado(id: number, estado: string): Observable<Pedido> {
    return this.http.patch<Pedido>(`${this.apiUrl}/${id}/estado`, { estado });
  }

  cancelarPedido(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getReporteVentas(fechaInicio: string, fechaFin: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/reportes/ventas`, {
      params: { fechaInicio, fechaFin }
    });
  }

  exportarReporteExcel(fechaInicio: string, fechaFin: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reportes/ventas/excel`, {
      params: { fechaInicio, fechaFin },
      responseType: 'blob'
    });
  }
}