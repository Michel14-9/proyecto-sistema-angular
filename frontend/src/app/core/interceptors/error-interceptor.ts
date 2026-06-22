// src/app/core/interceptors/error-interceptor.ts

import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: any) => {
        // ✅ Solo manejar errores HTTP (no respuestas exitosas)
        if (error instanceof HttpErrorResponse) {
          // ✅ Si es 401 y es del carrito
          if (error.status === 401 && req.url.includes('/carrito')) {
            console.log('🔄 [ErrorInterceptor] Error 401 en carrito - Limpiando sesión y carrito');
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            localStorage.removeItem('role');

            if (!this.router.url.includes('/login')) {
              this.router.navigate(['/login']);
            }

            return throwError(() => error);
          }

          // ✅ Solo mostrar y propagar errores reales (códigos 400 o superiores)
          if (error.status >= 400) {
            console.error('❌ [ErrorInterceptor] Error HTTP:', error.status, req.url);
            return throwError(() => error);
          }

          // ✅ Si es 2xx (éxito), no hacer nada, dejar pasar la respuesta
          return throwError(() => error);
        }

        // ✅ Si no es un HttpErrorResponse, propagar
        return throwError(() => error);
      })
    );
  }
}
