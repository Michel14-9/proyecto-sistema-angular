// src/app/modules/publico/index/index.ts

import { Component, OnInit, AfterViewInit, ViewEncapsulation, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';
import { ComboService } from '../../../core/services/combo.service';

declare var bootstrap: any;

@Component({
  selector: 'app-index',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './index.html',  // ← Usar index.html
  styleUrls: ['./index.css'],
  encapsulation: ViewEncapsulation.None
})
export class IndexComponent implements OnInit, AfterViewInit, OnDestroy {
  isAuthenticated: boolean = false;
  username: string = '';
  combos: any[] = [];
  totalCarrito: number = 0;
  private carouselInstance: any = null;

  constructor(
    private authService: AuthService,
    private carritoService: CarritoService,
    private comboService: ComboService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    console.log('🚀 IndexComponent inicializado');

    this.comboService.getCombos().subscribe({
      next: (response: any) => {
        console.log('📦 Respuesta del servidor:', response);

        if (response && response.success) {
          this.combos = response.data || [];
          console.log('✅ Combos cargados:', this.combos.length);
          console.log('📋 Primer combo:', this.combos[0]);

          // ✅ Inicializar carrusel DESPUÉS de cargar datos
          setTimeout(() => {
            this.inicializarCarousel();
          }, 300);
        } else {
          console.warn('⚠️ Respuesta sin success:', response);
          this.combos = response || [];
        }
      },
      error: (err: any) => {
        console.error('❌ Error cargando combos:', err);
        this.combos = [];
      }
    });

    this.carritoService.actualizarTotal();
    this.carritoService.getTotal().subscribe({
      next: (total: number) => {
        this.totalCarrito = total;
      },
      error: () => {
        this.totalCarrito = 0;
      }
    });
  }

  ngAfterViewInit(): void {
    console.log('🔄 ngAfterViewInit - Verificando carrusel...');
    if (this.combos && this.combos.length > 0) {
      this.inicializarCarousel();
    } else {
      console.log('⏳ Esperando combos para inicializar carrusel...');
    }
  }

  ngOnDestroy(): void {
    // Limpiar carrusel al destruir el componente
    if (this.carouselInstance) {
      try {
        this.carouselInstance.dispose();
        console.log('♻️ Carrusel destruido al salir');
      } catch (e) {
        // Ignorar
      }
    }
  }

  inicializarCarousel(): void {
    console.log('🔍 Inicializando carrusel...');

    const carouselElement = document.getElementById('combosCarousel');
    console.log('🔍 Elemento carrusel:', carouselElement);

    if (!carouselElement) {
      console.warn('⚠️ No se encontró #combosCarousel en el DOM');
      return;
    }

    if (typeof bootstrap === 'undefined') {
      console.warn('⚠️ Bootstrap no está disponible');
      return;
    }

    try {
      // Destruir carrusel existente
      if (this.carouselInstance) {
        this.carouselInstance.dispose();
        console.log('♻️ Carrusel existente destruido');
      }

      // Crear nuevo carrusel
      this.carouselInstance = new bootstrap.Carousel(carouselElement, {
        interval: 5000,
        wrap: true,
        pause: 'hover'
      });

      console.log('✅ Carrusel creado exitosamente');

      // Iniciar el carrusel
      setTimeout(() => {
        this.carouselInstance.cycle();
        console.log('🔄 Carrusel iniciado');
      }, 100);

    } catch (error) {
      console.error('❌ Error creando carrusel:', error);
    }
  }

  agregarAlCarrito(combo: any): void {
    console.log('Agregando al carrito:', combo);
    this.carritoService.agregarProducto(combo.id, 1).subscribe({
      next: (response: any) => {
        console.log('Respuesta:', response);
        this.carritoService.actualizarTotal();
        alert(`✅ ${combo.nombre} agregado al carrito`);
      },
      error: (err: any) => {
        console.error('Error:', err);
        if (err.status === 401 || err.status === 403) {
          alert('⚠️ Debes iniciar sesión primero');
        } else {
          alert('❌ Error al agregar al carrito');
        }
      }
    });
  }

  logout(): void {
    this.authService.logout();
    window.location.href = '/login';
  }
}
