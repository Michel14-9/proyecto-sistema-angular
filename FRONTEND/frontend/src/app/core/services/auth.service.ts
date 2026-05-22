import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  role: string;
  id_usuario: number;
  nombres: string;
  apellidos: string;
}

export interface CurrentUser {
  id_usuario: number;
  nombres: string;
  apellidos: string;
  rol: string;
  username: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  // Cambiado: Ahora emite CurrentUser | null en lugar de string | null
  private currentUserSubject = new BehaviorSubject<CurrentUser | null>(this.getCurrentUser());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient, 
    private router: Router
  ) {}

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('username', response.username);
        localStorage.setItem('role', response.role);
        localStorage.setItem('id_usuario', response.id_usuario.toString());
        localStorage.setItem('nombres', response.nombres);
        localStorage.setItem('apellidos', response.apellidos);
        
        this.isAuthenticatedSubject.next(true);
        this.currentUserSubject.next(this.getCurrentUser());
      })
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('id_usuario');
    localStorage.removeItem('nombres');
    localStorage.removeItem('apellidos');
    
    this.isAuthenticatedSubject.next(false);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login'], { queryParams: { logout: true } });
  }

  getCurrentUser(): CurrentUser | null {
    const token = localStorage.getItem('token');
    if (!token) return null;

    const id = localStorage.getItem('id_usuario');
    const nombres = localStorage.getItem('nombres');
    const apellidos = localStorage.getItem('apellidos');
    const rol = localStorage.getItem('role');
    const username = localStorage.getItem('username');

    if (!id || !nombres || !apellidos || !rol || !username) return null;

    return {
      id_usuario: Number(id),
      nombres,
      apellidos,
      rol,
      username
    };
  }

  isAuthenticated(): boolean {
    return this.hasToken();
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user?.rol === role;
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  getUsername(): string | null { 
    return localStorage.getItem('username'); 
  }
  
  getRole(): string | null { 
    return localStorage.getItem('role'); 
  }
  
  getToken(): string | null { 
    return localStorage.getItem('token'); 
  }
  
  isLoggedIn(): boolean { 
    return this.hasToken(); 
  }
}