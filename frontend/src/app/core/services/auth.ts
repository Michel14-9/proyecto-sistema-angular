// src/app/core/services/auth.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);

  constructor(private http: HttpClient) {
    const token = localStorage.getItem('token');
    this.isAuthenticatedSubject.next(!!token);
  }

  login(username: string, password: string): Observable<any> {
    const body = new HttpParams()
      .set('username', username)
      .set('password', password);

    return this.http.post(`${this.apiUrl}/login`, body.toString(), {
      headers: new HttpHeaders({
        'Content-Type': 'application/x-www-form-urlencoded'
      }),
      responseType: 'text'  // ← Importante: esperar texto, no JSON
    }).pipe(
      map((response: string) => {
        // El backend devuelve texto como: "Bienvenido Nombre (Rol: CLIENTE)"
        console.log('Respuesta del servidor:', response);

        // Verificar si el login fue exitoso (no contiene "incorrecta")
        const esExitoso = !response.includes('incorrecta') && !response.includes('error');

        if (esExitoso) {
          // Extraer el rol del texto (ejemplo: "Bienvenido Michel (Rol: ADMIN)")
          const rolMatch = response.match(/Rol:\s*(\w+)/);
          const rol = rolMatch ? rolMatch[1] : 'CLIENTE';

          return {
            success: true,
            message: response,
            username: username,
            role: rol
          };
        } else {
          return {
            success: false,
            message: response
          };
        }
      })
    );
  }

  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/registro`, userData);
  }

  logout(): Observable<any> {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    this.isAuthenticatedSubject.next(false);
    return this.http.post(`${this.apiUrl}/logout`, {});
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }

  getUsername(): string {
    return localStorage.getItem('username') || '';
  }

  getRole(): string {
    return localStorage.getItem('role') || '';
  }

  saveSession(token: string, username: string, role: string): void {
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    localStorage.setItem('role', role);
    this.isAuthenticatedSubject.next(true);
  }
}
