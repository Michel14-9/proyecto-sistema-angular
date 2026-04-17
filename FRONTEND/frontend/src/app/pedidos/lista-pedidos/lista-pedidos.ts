import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PedidoService } from '../../core/services/pedido';

interface Pedido {
  id_pedido: number;
  fecha_pedido: string;
  estado: string;
  total: number;
  cantidad_items: number;
}

@Component({
  selector: 'app-lista-pedidos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lista-pedidos.html',
  styleUrls: ['./lista-pedidos.css']
})
export class ListaPedidosComponent implements OnInit {
  pedidos: Pedido[] = [];
  estados: string[] = ['todos', 'pendiente', 'en_cocina', 'listo', 'entregado', 'cancelado'];
  estadoSeleccionado: string = 'todos';
  isLoading: boolean = true;

  constructor(
    private pedidoService: PedidoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarPedidos();
  }

  cargarPedidos(): void {
    this.pedidoService.getMisPedidos().subscribe({
      next: (data) => {
        this.pedidos = data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error cargando pedidos:', error);
        this.isLoading = false;
      }
    });
  }

  get pedidosFiltrados(): Pedido[] {
    if (this.estadoSeleccionado === 'todos') {
      return this.pedidos;
    }
    return this.pedidos.filter(p => p.estado === this.estadoSeleccionado);
  }

  verDetalle(id: number): void {
    this.router.navigate(['/pedidos', id]);
  }

  getEstadoClass(estado: string): string {
    const clases: { [key: string]: string } = {
      'pendiente': 'badge-warning',
      'en_cocina': 'badge-info',
      'listo': 'badge-success',
      'entregado': 'badge-primary',
      'cancelado': 'badge-danger'
    };
    return clases[estado] || 'badge-secondary';
  }
}