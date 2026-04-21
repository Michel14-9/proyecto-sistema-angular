export interface Cliente {
  id: number;
  nombres: string;
  apellidos: string;
  telefono?: string;
  email?: string;
}

export interface ItemPedido {
  id: number;
  cantidad: number;
  precio: number;
  nombreProducto: string;
  observaciones?: string;
}

export interface PedidoCocina {
  id: number;
  numeroPedido: string;
  fecha: string;
  fechaPreparacionCompleta?: string;
  estado: 'PAGADO' | 'PREPARACION' | 'LISTO' | 'ENTREGADO' | 'CANCELADO';
  tipoEntrega: 'DELIVERY' | 'RECOJO';
  total: number;
  cliente: Cliente | string;
  items: ItemPedido[];
  observaciones?: string;
  tiempoEstimado?: number;
  esUrgente?: boolean;
}

export interface MetricasCocina {
  success: boolean;
  totalPorPreparar: number;
  totalEnPreparacion: number;
  totalListosHoy: number;
  tiempoPromedio: number;
  pedidosUrgentes?: number;
}

export interface RespuestaOperacion {
  status: 'SUCCESS' | 'ERROR';
  message: string;
  data?: any;
}

export type EstadoColumna = 'por-preparar' | 'en-preparacion' | 'listos';

export interface ColumnaConfig {
  tipo: EstadoColumna;
  titulo: string;
  icono: string;
  colorHeader: string;
  textoVacio: string;
  iconoVacio: string;
}