import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PedidoService } from '../../core/services/pedido';
import { ProductoService } from '../../core/services/producto';

interface ItemCarrito {
  id_producto: number;
  nombre: string;
  precio: number;
  cantidad: number;
  subtotal: number;
}

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './carrito.html',
  styleUrls: ['./carrito.css']
})
export class CarritoComponent implements OnInit {
  items: ItemCarrito[] = [];
  total: number = 0;
  notas: string = '';
  direccion_entrega: string = '';

  constructor(
    private pedidoService: PedidoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarCarrito();
  }

  cargarCarrito(): void {
    const carrito = JSON.parse(localStorage.getItem('carrito') || '[]');
    this.items = carrito;
    this.calcularTotal();
  }

  calcularTotal(): void {
    this.total = this.items.reduce((sum, item) => sum + item.subtotal, 0);
  }

  actualizarCantidad(id_producto: number, cantidad: number): void {
    if (cantidad <= 0) {
      this.eliminarItem(id_producto);
      return;
    }
    
    const item = this.items.find(i => i.id_producto === id_producto);
    if (item) {
      item.cantidad = cantidad;
      item.subtotal = item.precio * cantidad;
      localStorage.setItem('carrito', JSON.stringify(this.items));
      this.calcularTotal();
    }
  }

  eliminarItem(id_producto: number): void {
    this.items = this.items.filter(i => i.id_producto !== id_producto);
    localStorage.setItem('carrito', JSON.stringify(this.items));
    this.calcularTotal();
  }

  vaciarCarrito(): void {
    if (confirm('¿Está seguro de vaciar el carrito?')) {
      this.items = [];
      localStorage.removeItem('carrito');
      this.calcularTotal();
    }
  }

  confirmarPedido(): void {
    if (this.items.length === 0) {
      alert('El carrito está vacío');
      return;
    }

    const pedido = {
      detalles: this.items.map(item => ({
        id_producto: item.id_producto,
        cantidad: item.cantidad,
        precio_unitario: item.precio
      })),
      notas: this.notas,
      direccion_entrega: this.direccion_entrega
    };

    this.pedidoService.crearPedido(pedido).subscribe({
      next: (response) => {
        localStorage.removeItem('carrito');
        this.router.navigate(['/pagos/registro', response.id_pedido]);
      },
      error: (error) => {
        alert('Error al crear el pedido: ' + error.error?.mensaje);
      }
    });
  }

  continuarComprando(): void {
    this.router.navigate(['/productos']);
  }
}