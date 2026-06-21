// src/app/core/services/carrito.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of, catchError, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CarritoService {
  private apiUrl = 'http://localhost:8080/api/carrito';
  private totalSubject = new BehaviorSubject<number>(0);
  private cantidadSubject = new BehaviorSubject<number>(0);
  private actualizando = false;

  constructor(private http: HttpClient) {
    if (localStorage.getItem('token')) {
      console.log('🔄 Token encontrado, actualizando carrito...');
      this.actualizarTotal();
    } else {
      console.log('🔄 Sin sesión, carrito en 0');
      this.totalSubject.next(0);
      this.cantidadSubject.next(0);
    }
  }

  obtenerCarrito(): Observable<any> {
    if (!localStorage.getItem('token')) {
      return of({ items: [], total: 0, cantidadItems: 0 });
    }
    return this.http.get(this.apiUrl, { withCredentials: true });
  }

  agregarProducto(productoId: number, cantidad: number = 1): Observable<any> {
    if (!localStorage.getItem('token')) {
      console.warn('⚠️ Usuario no autenticado');
      return of({ success: false, message: 'Usuario no autenticado' });
    }
    return this.http.post(`${this.apiUrl}/agregar`, { productoId, cantidad }, { withCredentials: true })
      .pipe(
        tap(() => {
          setTimeout(() => this.actualizarTotal(), 300);
        })
      );
  }

  actualizarCantidad(itemId: number, cantidad: number): Observable<any> {
    if (!localStorage.getItem('token')) {
      return of({ success: false, message: 'Usuario no autenticado' });
    }
    return this.http.put(`${this.apiUrl}/actualizar/${itemId}`, { cantidad }, { withCredentials: true })
      .pipe(
        tap(() => {
          setTimeout(() => this.actualizarTotal(), 300);
        })
      );
  }

  eliminarProducto(itemId: number): Observable<any> {
    if (!localStorage.getItem('token')) {
      return of({ success: false, message: 'Usuario no autenticado' });
    }
    return this.http.delete(`${this.apiUrl}/eliminar/${itemId}`, { withCredentials: true })
      .pipe(
        tap(() => {
          setTimeout(() => this.actualizarTotal(), 300);
        })
      );
  }

  vaciarCarrito(): Observable<any> {
    if (!localStorage.getItem('token')) {
      return of({ success: false, message: 'Usuario no autenticado' });
    }
    return this.http.delete(`${this.apiUrl}/vaciar`, { withCredentials: true })
      .pipe(
        tap(() => {
          this.totalSubject.next(0);
          this.cantidadSubject.next(0);
        })
      );
  }

  getTotal(): Observable<number> {
    return this.totalSubject.asObservable();
  }

  getCantidad(): Observable<number> {
    return this.cantidadSubject.asObservable();
  }

  limpiarLocal(): void {
    console.log('🔄 Limpiando carrito local');
    this.totalSubject.next(0);
    this.cantidadSubject.next(0);
    this.actualizando = false;
  }

  actualizarTotal(): void {
    if (!localStorage.getItem('token')) {
      console.log('🔄 Sin sesión, carrito en 0');
      this.totalSubject.next(0);
      this.cantidadSubject.next(0);
      this.actualizando = false;
      return;
    }

    if (this.actualizando) {
      console.log('⏳ Ya se está actualizando el carrito...');
      return;
    }

    this.actualizando = true;
    console.log('🔄 Actualizando total del carrito...');

    this.http.get<{ total: number, cantidadItems: number }>(`${this.apiUrl}/total`, { withCredentials: true })
      .pipe(
        catchError((err) => {
          if (err.status === 401 || err.status === 403) {
            console.log('🔄 Usuario no autenticado, carrito vacío');
            // ✅ SOLO limpiar carrito, NO el token (el interceptor lo hará)
            this.totalSubject.next(0);
            this.cantidadSubject.next(0);
          } else {
            console.error('❌ Error al actualizar total del carrito:', err);
          }
          this.actualizando = false;
          return of({ total: 0, cantidadItems: 0 });
        })
      )
      .subscribe({
        next: (data) => {
          console.log('✅ Total actualizado:', data);
          this.totalSubject.next(data?.total || 0);
          this.cantidadSubject.next(data?.cantidadItems || 0);
          this.actualizando = false;
        },
        error: () => {
          this.totalSubject.next(0);
          this.cantidadSubject.next(0);
          this.actualizando = false;
        }
      });
  }

  refrescarTotal(): void {
    this.actualizando = false;
    this.actualizarTotal();
  }
}
