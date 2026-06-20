import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WhatsAppRequest {
  phoneNumber: string;
  message: string;
  pedidoId?: number;
}

@Injectable({ providedIn: 'root' })
export class WhatsAppService {
  private apiUrl = 'http://localhost:8080/api/whatsapp';

  constructor(private http: HttpClient) {}

  enviarMensaje(dto: WhatsAppRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/send`, dto);
  }

  notificarConfirmado(pedidoId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/pedido/${pedidoId}/confirmado`, {});
  }

  notificarEnCamino(pedidoId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/pedido/${pedidoId}/en-camino`, {});
  }
}