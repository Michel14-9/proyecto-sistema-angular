// src/app/core/interceptors/auth.interceptor.ts
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // No necesitas agregar nada manualmente, solo asegurar que se envíen cookies
    const cloned = req.clone({
      withCredentials: true  // ← Esto es lo importante: enviar cookies de sesión
    });

    return next.handle(cloned);
  }
}
