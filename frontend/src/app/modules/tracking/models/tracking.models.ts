export interface TrackingResponse {
  id: number;
  numeroPedido: string;
  estado: 'READY' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  latRepartidor: number;
  lngRepartidor: number;
  latCliente: number;
  lngCliente: number;
  latLocal: number;
  lngLocal: number;
  direccionCliente: string;
  etaMinutos: number;
  distanciaKm: number;
  nombreRepartidor: string;
  mensaje: string;
}

export interface UpdateLocationRequest {
  numeroPedido: string;
  latRepartidor: number;
  lngRepartidor: number;
}