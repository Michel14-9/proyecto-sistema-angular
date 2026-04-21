import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PedidoCocina, EstadoColumna } from '../../models/cocinero.models';

@Component({
  selector: 'app-detalle-pedido',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './detalle-pedido.html',
  styleUrls: ['./detalle-pedido.css']
})
export class DetallePedidoComponent {
  @Input() pedido: PedidoCocina | null = null;
  @Input() tipoColumna: EstadoColumna | null = null;
  
  @Output() cerrar = new EventEmitter<void>();
  @Output() iniciarPreparacion = new EventEmitter<PedidoCocina>();
  @Output() marcarListo = new EventEmitter<PedidoCocina>();

  get numeroPedido(): string {
    return this.pedido?.numeroPedido || `#${this.pedido?.id || ''}`;
  }

  get nombreCliente(): string {
    if (!this.pedido?.cliente) return 'Cliente no especificado';
    
    if (typeof this.pedido.cliente === 'object') {
      const cliente = this.pedido.cliente;
      return `${cliente.nombres || ''} ${cliente.apellidos || ''}`.trim() || 'Cliente sin nombre';
    }
    
    return String(this.pedido.cliente);
  }

  get tipoEntrega(): string {
    return this.pedido?.tipoEntrega || 'DELIVERY';
  }

  get horaPedido(): string {
    if (!this.pedido?.fecha) return 'Cargando...';
    
    try {
      const fecha = new Date(this.pedido.fecha);
      return fecha.toLocaleString('es-PE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        timeZone: 'America/Lima'
      });
    } catch {
      return 'Fecha inválida';
    }
  }

  get tiempoTranscurrido(): { texto: string; minutosTotales: number } {
    if (!this.pedido?.fecha) {
      return { texto: 'N/A', minutosTotales: 0 };
    }

    try {
      const fechaPedido = new Date(this.pedido.fecha);
      const ahora = new Date();
      const diferenciaMs = ahora.getTime() - fechaPedido.getTime();
      const minutosTotales = Math.floor(diferenciaMs / (1000 * 60));

      if (minutosTotales < 60) {
        return { texto: `${minutosTotales}min`, minutosTotales };
      } else {
        const horas = Math.floor(minutosTotales / 60);
        const minutos = minutosTotales % 60;
        return { texto: `${horas}h ${minutos}m`, minutosTotales };
      }
    } catch {
      return { texto: 'N/A', minutosTotales: 0 };
    }
  }

  get claseTiempo(): string {
    const minutos = this.tiempoTranscurrido.minutosTotales;
    if (minutos > 30) return 'bg-danger';
    if (minutos > 15) return 'bg-warning';
    return 'bg-info';
  }

  get totalFormateado(): string {
    return `S/ ${(this.pedido?.total || 0).toFixed(2)}`;
  }

  get tieneObservaciones(): boolean {
    return !!(this.pedido?.observaciones && this.pedido.observaciones.trim() !== '');
  }

  get puedeIniciarPreparacion(): boolean {
    return this.tipoColumna === 'por-preparar';
  }

  get puedeMarcarListo(): boolean {
    return this.tipoColumna === 'en-preparacion';
  }

  get esListo(): boolean {
    return this.tipoColumna === 'listos';
  }

  calcularSubtotal(item: any): number {
    return (item.precio || 0) * (item.cantidad || 1);
  }

  onCerrar(): void {
    this.cerrar.emit();
  }

  onIniciarPreparacion(): void {
    if (this.pedido) {
      this.iniciarPreparacion.emit(this.pedido);
    }
  }

  onMarcarListo(): void {
    if (this.pedido) {
      this.marcarListo.emit(this.pedido);
    }
  }
}