// src/app/core/interceptors/error-interceptor.ts

import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

// ✅ NO inyectar AuthService, usar solo localStorage y Router
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        // ✅ Si es 401 y es del carrito
        if (error.status === 401 && req.url.includes('/carrito')) {
          console.log('🔄 [ErrorInterceptor] Error 401 en carrito - Limpiando sesión y carrito');

          // ✅ Limpiar token y sesión SOLO con localStorage
          localStorage.removeItem('token');
          localStorage.removeItem('username');
          localStorage.removeItem('role');

          // ✅ Redirigir al login si no está ya en la página de login
          if (!this.router.url.includes('/login')) {
            this.router.navigate(['/login']);
          }

          return throwError(() => error);
        }

        console.error('❌ [ErrorInterceptor] Error HTTP:', error.status, req.url);
        return throwError(() => error);
      })
    );
  }
}
