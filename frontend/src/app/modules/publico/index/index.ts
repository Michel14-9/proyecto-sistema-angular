// src/app/modules/publico/index/index.ts
import { Component, OnInit, AfterViewInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { CarritoService } from '../../../core/services/carrito';
import { ComboService } from '../../../core/services/combo';

declare var bootstrap: any;

@Component({
  selector: 'app-index',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './index.html',
  styleUrls: ['./index.css'],
  encapsulation: ViewEncapsulation.None  //
})
export class IndexComponent implements OnInit, AfterViewInit {
  isAuthenticated: boolean = false;
  username: string = '';
  combos: any[] = [];
  totalCarrito: number = 0;

  constructor(
    private authService: AuthService,
    private carritoService: CarritoService,
    private comboService: ComboService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    this.comboService.getCombos().subscribe({
      next: (data: any) => {
        this.combos = data;
        console.log('Combos cargados:', this.combos);
      },
      error: (err: any) => {
        console.error('Error cargando combos:', err);
      }
    });

    this.carritoService.actualizarTotal();
    this.carritoService.getTotal().subscribe({
      next: (total: number) => {
        this.totalCarrito = total;
      }
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      const carousel = document.getElementById('combosCarousel');
      if (carousel && typeof bootstrap !== 'undefined') {
        new bootstrap.Carousel(carousel, {
          interval: 5000,
          wrap: true,
          pause: 'hover'
        });
      }
    }, 100);
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
    this.authService.logout().subscribe({
      next: () => {
        window.location.href = '/login';
      }
    });
  }
}
