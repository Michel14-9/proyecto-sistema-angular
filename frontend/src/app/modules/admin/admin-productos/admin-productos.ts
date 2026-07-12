// src/app/modules/admin/admin-productos/admin-productos.ts
import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductsService, Producto } from '../../../core/services/products.service';
import { AlertService } from '../../../core/services/alert.service';
import { ConfirmModalService } from '../../../core/services/confirm-modal.service';
import { DashboardService } from '../../../core/services/dashboard.service';

declare var bootstrap: any;

@Component({
  selector: 'app-admin-productos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-productos.html',
  styleUrls: ['./admin-productos.css']
})
export class AdminProductosComponent implements OnInit, AfterViewInit, OnDestroy {
  productos: Producto[] = [];
  productosFiltrados: Producto[] = [];
  busquedaProductos: string = '';
  filtroCategoria: string = '';
  productoForm: Producto = { id: 0, nombre: '', tipo: '', precio: 0, descripcion: '', imagenUrl: '' };
  productoEditando: boolean = false;
  guardandoProducto: boolean = false;
  modalProductoTitulo: string = 'Agregar Producto';
  private productModal: any = null;

  constructor(
    private productsService: ProductsService,
    private dashboardService: DashboardService,
    private alertService: AlertService,
    private confirmModal: ConfirmModalService
  ) {}

  ngOnInit(): void {
    this.cargarProductos();
  }

  ngAfterViewInit(): void {
    const productModalEl = document.getElementById('productModal');
    if (productModalEl) {
      this.productModal = new bootstrap.Modal(productModalEl);
    }
  }

  ngOnDestroy(): void {
    if (this.productModal) {
      this.productModal.dispose();
    }
  }

  cargarProductos(): void {
    this.productsService.getAll().subscribe({
      next: (data: any) => {
        if (Array.isArray(data)) {
          this.productos = data.filter((p: any) => p.activo !== false);
          this.filtrarProductos();
        }
      },
      error: () => this.alertService.mostrar('Error al cargar productos', 'warning')
    });
  }

  filtrarProductos(): void {
    let filtrados = this.productos;
    if (this.busquedaProductos.trim()) {
      const term = this.busquedaProductos.toLowerCase().trim();
      filtrados = filtrados.filter(p =>
        p.nombre.toLowerCase().includes(term) ||
        (p.descripcion && p.descripcion.toLowerCase().includes(term))
      );
    }
    if (this.filtroCategoria) {
      filtrados = filtrados.filter(p => p.tipo.toLowerCase() === this.filtroCategoria.toLowerCase());
    }
    this.productosFiltrados = filtrados;
  }

  abrirModalProducto(producto?: Producto): void {
    this.productoEditando = !!producto;
    this.modalProductoTitulo = producto ? 'Editar Producto' : 'Agregar Producto';
    this.productoForm = producto ? { ...producto } : { id: 0, nombre: '', tipo: '', precio: 0, descripcion: '', imagenUrl: '' };
    if (this.productModal) this.productModal.show();
  }

  cerrarModalProducto(): void {
    if (this.productModal) this.productModal.hide();
  }

  guardarProducto(): void {
    if (!this.productoForm.nombre.trim()) {
      this.alertService.mostrar('El nombre es requerido', 'warning');
      return;
    }
    if (!this.productoForm.tipo) {
      this.alertService.mostrar('Debe seleccionar una categoría', 'warning');
      return;
    }
    if (this.productoForm.precio <= 0) {
      this.alertService.mostrar('El precio debe ser mayor a 0', 'warning');
      return;
    }

    this.guardandoProducto = true;
    const formData = new FormData();
    formData.append('nombre', this.productoForm.nombre);
    formData.append('tipo', this.productoForm.tipo);
    formData.append('precio', this.productoForm.precio.toString());
    formData.append('descripcion', this.productoForm.descripcion || '');
    formData.append('imagenUrl', this.productoForm.imagenUrl || '');

    const action = this.productoEditando
      ? this.productsService.update(this.productoForm.id, formData)
      : this.productsService.create(formData);

    action.subscribe({
      next: () => {
        this.guardandoProducto = false;
        this.cerrarModalProducto();
        this.alertService.mostrar(this.productoEditando ? 'Producto actualizado' : 'Producto guardado', 'success');
        this.cargarProductos();
      },
      error: (error: any) => {
        this.guardandoProducto = false;
        console.error('❌ Error guardando producto:', error);
        this.alertService.mostrar('Error al guardar el producto', 'danger');
      }
    });
  }

  confirmarEliminarProducto(producto: Producto): void {
    this.confirmModal.confirmar(
      'Confirmar Eliminación',
      `¿Eliminar "${producto.nombre}"?`,
      () => this.eliminarProducto(producto.id)
    );
  }

  eliminarProducto(id: number): void {
    this.productsService.delete(id).subscribe({
      next: (response: any) => {
        this.alertService.mostrar(response.desactivado ? 'Producto desactivado' : 'Producto eliminado', 'success');
        this.cargarProductos();
      },
      error: (error: any) => {
        console.error('❌ Error eliminando producto:', error);
        this.alertService.mostrar('Error al eliminar el producto', 'danger');
      }
    });
  }
}
