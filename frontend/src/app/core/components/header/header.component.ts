// src/app/core/components/header/header.component.ts

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { CarritoService } from '../../services/carrito.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit, OnDestroy {
  isAuthenticated: boolean = false;
  username: string = '';
  totalCarrito: number = 0;
  private subscriptions: Subscription[] = [];

  constructor(
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    // ✅ Suscribirse a los cambios de autenticación
    this.subscriptions.push(
      this.authService.isAuthenticated$.subscribe(auth => {
        this.isAuthenticated = auth;
        console.log('🔄 Header: isAuthenticated actualizado:', auth);
      })
    );

    this.subscriptions.push(
      this.authService.username$.subscribe(username => {
        this.username = username;
        console.log('🔄 Header: username actualizado:', username);
      })
    );

    // ✅ Suscribirse al carrito
    this.subscriptions.push(
      this.carritoService.getTotal().subscribe(total => {
        this.totalCarrito = total;
      })
    );
  }

  ngOnDestroy(): void {
    // ✅ Limpiar suscripciones
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  logout(): void {
    console.log('🔄 ========================================');
    console.log('🔄 LOGOUT DESDE EL HEADER');
    console.log('🔄 ========================================');

    // ✅ 1. Limpiar localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    console.log('✅ localStorage limpiado');

    // ✅ 2. Limpiar carrito
    this.carritoService.limpiarLocal();
    console.log('✅ Carrito limpiado');

    // ✅ 3. Actualizar estado
    this.isAuthenticated = false;
    this.username = '';
    this.totalCarrito = 0;
    console.log('✅ Estado actualizado');

    // ✅ 4. Eliminar cookies
    document.cookie = 'JSESSIONID=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    console.log('✅ Cookies eliminadas');

    // ✅ 5. Logout en backend
    this.http.post('http://localhost:8080/api/auth/logout', {}, {
      withCredentials: true
    }).subscribe({
      next: () => console.log('✅ Logout backend ok'),
      error: () => console.log('⚠️ Logout backend falló')
    });

    // ✅ 6. Redirigir al login
    this.router.navigate(['/login']);

    // ✅ 7. Recargar
    setTimeout(() => {
      window.location.reload();
    }, 200);
  }
}
