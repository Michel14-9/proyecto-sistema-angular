// src/app/modules/publico/menu/menu.component.ts

import { Component, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit, AfterViewInit {

  // Datos del menú
  menuData: any = {};
  categorias: string[] = ['pollos', 'parrillas', 'chicharron', 'broaster', 'hamburguesas', 'criollos', 'combos'];

  // Estado
  isLoading: boolean = true;
  errorMessage: string = '';

  // Carrito
  totalCarrito: number = 0;

  // Favoritos
  favoritos: Set<number> = new Set();

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarMenu();
    this.cargarFavoritos();

    this.carritoService.getTotal().subscribe(total => {
      this.totalCarrito = total;
    });
  }

  ngAfterViewInit(): void {
    // Inicializar animaciones después de que la vista esté lista
    this.configurarAnimaciones();
  }

  cargarMenu(): void {
    this.isLoading = true;
    this.http.get('/api/menu/completo').subscribe({
      next: (response: any) => {
        if (response.success) {
          this.menuData = response.menu;
          console.log('✅ Menú cargado:', this.menuData);
        } else {
          this.errorMessage = response.message || 'Error al cargar el menú';
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error al cargar menú:', error);
        this.errorMessage = 'Error al cargar el menú. Intenta nuevamente.';
        this.isLoading = false;
      }
    });
  }

  cargarFavoritos(): void {
    if (!this.authService.isAuthenticated()) return;

    this.http.get('/api/favoritos/listar').subscribe({
      next: (response: any) => {
        if (response.success && response.favoritos) {
          response.favoritos.forEach((fav: any) => {
            this.favoritos.add(fav.id);
          });
        }
      },
      error: (error) => console.error('Error cargando favoritos:', error)
    });
  }

  toggleFavorito(productId: number): void {
    if (!this.authService.isAuthenticated()) {
      this.mostrarNotificacion('Debes iniciar sesión para agregar favoritos', 'warning');
      return;
    }

    this.http.post('/api/favoritos/toggle', { productoId: productId }).subscribe({
      next: (response: any) => {
        if (response.success) {
          if (response.agregado) {
            this.favoritos.add(productId);
            this.mostrarNotificacion('Agregado a favoritos', 'success');
          } else {
            this.favoritos.delete(productId);
            this.mostrarNotificacion('Eliminado de favoritos', 'info');
          }
        }
      },
      error: (error) => {
        console.error('Error toggle favorito:', error);
        this.mostrarNotificacion('Error al actualizar favoritos', 'error');
      }
    });
  }

  esFavorito(productId: number): boolean {
    return this.favoritos.has(productId);
  }

  agregarAlCarrito(productoId: number): void {
    this.carritoService.agregarProducto(productoId, 1).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.mostrarNotificacion(`${response.message} - ${response.productoNombre}`, 'success');
          this.carritoService.refrescarTotal();
        }
      },
      error: (error) => {
        console.error('Error agregar al carrito:', error);
        this.mostrarNotificacion('Error al agregar al carrito', 'error');
      }
    });
  }

  mostrarNotificacion(mensaje: string, tipo: string = 'info'): void {
    // Implementar notificación - puedes usar un servicio de notificaciones
    console.log(`[${tipo}] ${mensaje}`);
    // Por ahora usamos alert
    alert(mensaje);
  }

  configurarAnimaciones(): void {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('animacion-visible');
        }
      });
    }, { threshold: 0.1 });

    document.querySelectorAll('.product-card, .category-title').forEach(el => {
      observer.observe(el);
    });
  }

  scrollToCategory(categoria: string): void {
    const element = document.getElementById(categoria);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  getCategorias(): string[] {
    return Object.keys(this.menuData);
  }

  getProductos(categoria: string): any[] {
    return this.menuData[categoria] || [];
  }
}
