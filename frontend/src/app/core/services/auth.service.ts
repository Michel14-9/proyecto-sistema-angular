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
  private currentUserSubject = new BehaviorSubject<any>(null);

  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public username$ = this.usernameSubject.asObservable();
  public role$ = this.roleSubject.asObservable();
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private carritoService: CarritoService
  ) {
    this.cargarSesion();
  }

  private cargarSesion(): void {
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('username') || '';
    const role = localStorage.getItem('role') || '';
    const userStr = localStorage.getItem('user');

    this.isAuthenticatedSubject.next(!!token);
    this.usernameSubject.next(username);
    this.roleSubject.next(role);

    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        this.currentUserSubject.next(user);
      } catch (e) {
        console.error('❌ Error parsing user:', e);
      }
    }
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

        // Si el login es exitoso, guardar sesión
        if (response && response.status === "ok") {
          const rol = response.rol || 'ROLE_CLIENTE';
          const usuario = response.usuario || response.user || {};

          // Guardar en localStorage
          localStorage.setItem('token', 'session_' + Date.now());
          localStorage.setItem('username', usuario.username || usuario.email || username);
          localStorage.setItem('role', rol);
          localStorage.setItem('user', JSON.stringify(usuario));

          // Actualizar subjects
          this.isAuthenticatedSubject.next(true);
          this.usernameSubject.next(usuario.username || usuario.email || username);
          this.roleSubject.next(rol);
          this.currentUserSubject.next(usuario);

          console.log('✅ Sesión guardada correctamente');
        }
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
      .set('g-recaptcha-response', userData['g-recaptcha-response'] || '');

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
          this.limpiarSesion();
          console.log('✅ Sesión cerrada correctamente');
        },
        error: (error) => {
          console.error('❌ Error al cerrar sesión:', error);
          this.limpiarSesion();
        }
      })
    );
  }

  private limpiarSesion(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('user');

    this.isAuthenticatedSubject.next(false);
    this.usernameSubject.next('');
    this.roleSubject.next('');
    this.currentUserSubject.next(null);

    this.carritoService.limpiarLocal();
  }

  // ✅ Métodos públicos para obtener datos
  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  getUsername(): string {
    return this.usernameSubject.value;
  }

  getRole(): string {
    return this.roleSubject.value;
  }

  getCurrentUser(): any {
    return this.currentUserSubject.value;
  }

  saveSession(token: string, username: string, role: string): void {
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    localStorage.setItem('role', role);

    this.isAuthenticatedSubject.next(true);
    this.usernameSubject.next(username);
    this.roleSubject.next(role);
  }

  // ✅ Método para guardar usuario completo
  saveUser(user: any): void {
    localStorage.setItem('user', JSON.stringify(user));
    this.currentUserSubject.next(user);
  }
}
