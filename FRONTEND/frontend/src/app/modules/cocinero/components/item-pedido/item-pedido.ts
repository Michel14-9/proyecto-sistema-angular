import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PedidoCocina } from '../../models/cocinero.models';

@Component({
  selector: 'app-item-pedido',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './item-pedido.html',
  styleUrls: ['./item-pedido.css']
})
export class ItemPedidoComponent {
  @Input() pedido!: PedidoCocina;
  @Input() tipoColumna!: 'por-preparar' | 'en-preparacion' | 'listos';
  @Output() seleccionar = new EventEmitter<PedidoCocina>();

  get nombreCliente(): string {
    if (!this.pedido.cliente) return 'Cliente no especificado';
    
    if (typeof this.pedido.cliente === 'object') {
      const cliente = this.pedido.cliente;
      return `${cliente.nombres || ''} ${cliente.apellidos || ''}`.trim() || 'Cliente sin nombre';
    }
    
    return String(this.pedido.cliente);
  }

  get numeroPedido(): string {
    return this.pedido.numeroPedido || `#${this.pedido.id}`;
  }

  get horaPedido(): string {
    if (!this.pedido.fecha) return '';
    
    try {
      const fecha = new Date(this.pedido.fecha);
      return fecha.toLocaleTimeString('es-PE', {
        hour: '2-digit',
        minute: '2-digit',
        timeZone: 'America/Lima'
      });
    } catch {
      return '';
    }
  }

  get tiempoTranscurrido(): { texto: string; minutosTotales: number } {
    if (!this.pedido.fecha) {
      return { texto: 'N/A', minutosTotales: 0 };
    }

    try {
      const fechaPedido = new Date(this.pedido.fecha);
      if (isNaN(fechaPedido.getTime())) {
        return { texto: 'N/A', minutosTotales: 0 };
      }

      const ahora = new Date();
      const diferenciaMs = ahora.getTime() - fechaPedido.getTime();
      const minutosTotales = Math.floor(diferenciaMs / (1000 * 60));

      if (minutosTotales > 360) {
        return { texto: 'Revisar', minutosTotales };
      }

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

  get esUrgente(): boolean {
    return this.tiempoTranscurrido.minutosTotales > 30;
  }

  get claseTiempo(): string {
    const minutos = this.tiempoTranscurrido.minutosTotales;
    if (minutos > 30) return 'bg-danger';
    if (minutos > 15) return 'bg-warning';
    return 'bg-info';
  }

  onClick(): void {
    this.seleccionar.emit(this.pedido);
  }
}