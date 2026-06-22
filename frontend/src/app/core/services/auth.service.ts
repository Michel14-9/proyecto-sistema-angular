// src/app/core/services/auth.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { CarritoService } from './carrito.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  private usernameSubject = new BehaviorSubject<string>('');
  private roleSubject = new BehaviorSubject<string>('');

  // ✅ Exponer como observables para que los componentes se suscriban
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public username$ = this.usernameSubject.asObservable();
  public role$ = this.roleSubject.asObservable();

  constructor(
    private http: HttpClient,
    private carritoService: CarritoService
  ) {
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('username') || '';
    const role = localStorage.getItem('role') || '';

    this.isAuthenticatedSubject.next(!!token);
    this.usernameSubject.next(username);
    this.roleSubject.next(role);
  }

  login(username: string, password: string): Observable<any> {
    const body = new HttpParams()
      .set('username', username)
      .set('password', password);

    return this.http.post(`${this.apiUrl}/login`, body.toString(), {
      headers: new HttpHeaders({
        'Content-Type': 'application/x-www-form-urlencoded'
      }),
      withCredentials: true
    }).pipe(
      tap((response: any) => {
        console.log('📥 Respuesta del servidor:', response);
      })
    );
  }

 register(userData: any): Observable<any> {
   console.log('📤 Enviando registro:', userData);

   const body = new HttpParams()
     .set('nombres', userData.nombres || '')
     .set('apellidos', userData.apellidos || '')
     .set('tipoDocumento', userData.tipoDocumento || 'DNI')
     .set('numeroDocumento', userData.numeroDocumento || '')
     .set('telefono', userData.telefono || '')
     .set('fechaNacimiento', userData.fechaNacimiento || '')
     .set('email', userData.email || '')
     .set('password', userData.password || '')
     .set('rol', userData.rol || 'CLIENTE')
     .set('g-recaptcha-response', userData['g-recaptcha-response'] || ''); // ✅ Esto debe ir dentro del método

   return this.http.post(`${this.apiUrl}/registro`, body.toString(), {
     headers: new HttpHeaders({
       'Content-Type': 'application/x-www-form-urlencoded'
     })
   });
 }

  getUserData(): Observable<any> {
    return this.http.get(`${this.apiUrl}/datos-usuario`, {
      withCredentials: true
    });
  }

  getUserRole(): Observable<any> {
    return this.http.get(`${this.apiUrl}/rol`, {
      withCredentials: true
    });
  }

  logout(): Observable<any> {
    return this.http.post(`${this.apiUrl}/logout`, {}, {
      withCredentials: true
    }).pipe(
      tap({
        next: () => {
          localStorage.removeItem('token');
          localStorage.removeItem('username');
          localStorage.removeItem('role');

          this.isAuthenticatedSubject.next(false);
          this.usernameSubject.next('');
          this.roleSubject.next('');

          this.carritoService.limpiarLocal();

          console.log('✅ Sesión cerrada correctamente');
        },
        error: (error) => {
          console.error('❌ Error al cerrar sesión:', error);
          localStorage.removeItem('token');
          localStorage.removeItem('username');
          localStorage.removeItem('role');

          this.isAuthenticatedSubject.next(false);
          this.usernameSubject.next('');
          this.roleSubject.next('');

          this.carritoService.limpiarLocal();
        }
      })
    );
  }

  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  getUsername(): string {
    return this.usernameSubject.value;
  }

  getRole(): string {
    return this.roleSubject.value;
  }

  saveSession(token: string, username: string, role: string): void {
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    localStorage.setItem('role', role);
    this.isAuthenticatedSubject.next(true);
    this.usernameSubject.next(username);
    this.roleSubject.next(role);
  }
}
