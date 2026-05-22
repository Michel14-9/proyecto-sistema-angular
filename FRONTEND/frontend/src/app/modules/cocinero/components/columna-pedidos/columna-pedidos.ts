import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PedidoCocina, EstadoColumna, ColumnaConfig } from '../../models/cocinero.models';
import { ItemPedidoComponent } from '../item-pedido/item-pedido';

@Component({
  selector: 'app-columna-pedidos',
  standalone: true,
  imports: [CommonModule, ItemPedidoComponent],
  templateUrl: './columna-pedidos.html',
  styleUrls: ['./columna-pedidos.css']
})
export class ColumnaPedidosComponent {
  @Input() pedidos: PedidoCocina[] = [];
  @Input() config!: ColumnaConfig;
  @Input() pedidoSeleccionado: PedidoCocina | null = null;
  
  @Output() pedidoSelect = new EventEmitter<PedidoCocina>();

  get hayPedidos(): boolean {
    return this.pedidos.length > 0;
  }

  get claseHeader(): string {
    const clases: Record<EstadoColumna, string> = {
      'por-preparar': 'bg-warning text-dark',
      'en-preparacion': 'bg-primary text-white',
      'listos': 'bg-success text-white'
    };
    return clases[this.config.tipo] || 'bg-secondary text-white';
  }

  isSelected(pedido: PedidoCocina): boolean {
    return this.pedidoSeleccionado?.id === pedido.id;
  }

  onSeleccionarPedido(pedido: PedidoCocina): void {
    this.pedidoSelect.emit(pedido);
  }
}