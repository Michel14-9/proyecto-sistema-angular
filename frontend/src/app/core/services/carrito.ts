// src/app/core/services/carrito.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CarritoService {
  private apiUrl = 'http://localhost:8080/api/carrito';
  private totalSubject = new BehaviorSubject<number>(0);

  constructor(private http: HttpClient) {}

  agregarProducto(productoId: number, cantidad: number = 1): Observable<any> {
    return this.http.post(`${this.apiUrl}/agregar`, { productoId, cantidad });
  }

  getTotal(): Observable<number> {
    return this.totalSubject.asObservable();
  }

  actualizarTotal(): void {
    this.http.get<{ total: number }>(`${this.apiUrl}/total`).subscribe({
      next: (data) => this.totalSubject.next(data.total),
      error: (err) => console.error('Error:', err)
    });
  }
}
