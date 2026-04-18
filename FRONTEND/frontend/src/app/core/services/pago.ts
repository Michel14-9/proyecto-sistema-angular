import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PagoRequest {
  tipoEntrega: string;
  direccion?: string;
  fechaEntrega: string;
  horaEntrega: string;
  instrucciones?: string;
  metodoPago: string;
  tarjeta?: {
    numeroTarjeta: string;
    fechaVencimiento: string;
    cvv: string;
    nombreTarjeta: string;
  };
  carrito: any[];
  subtotal: number;
  costoEnvio: number;
  descuento: number;
  total: number;
  idPedido?: number;
}

export interface PagoResponse {
  id_pedido: number;
  id_pago: number;
  estado: string;
  mensaje: string;
  comprobante_url?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PagoService {
  private apiUrl = 'http://localhost:8080/api/pagos';

  constructor(private http: HttpClient) {}

  procesarPago(datosPago: PagoRequest): Observable<PagoResponse> {
    return this.http.post<PagoResponse>(`${this.apiUrl}/procesar`, datosPago);
  }

  getMetodosPago(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/metodos`);
  }

  verificarPago(idPago: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${idPago}/verificar`);
  }

  getComprobante(idPedido: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/comprobante/${idPedido}`, {
      responseType: 'blob'
    });
  }
}