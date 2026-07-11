import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, timer } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AlertService } from '../services/alert.service';



@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  private readonly MAX_RETRIES = 2;
  private readonly RETRY_DELAY = 1000;

  constructor(
    private router: Router,
    private alertService: AlertService

  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      retry({
        count: this.MAX_RETRIES,
        delay: (error: HttpErrorResponse) => {
          if (error.status === 0 || error.status >= 500) {
            return timer(this.RETRY_DELAY);
          }
          return throwError(() => error);
        }
      }),
      catchError((error: any) => {
        if (error instanceof HttpErrorResponse) {
          return this.handleHttpError(error, req);
        }
        console.error('🚨 Error no HTTP:', error);
        this.alertService.error('Ha ocurrido un error inesperado');
        return throwError(() => error);
      })
    );
  }

  private handleHttpError(error: HttpErrorResponse, req: HttpRequest<any>): Observable<never> {
    console.error('🚨 Error HTTP:', {
      url: req.url,
      method: req.method,
      status: error.status,
      statusText: error.statusText,
      error: error.error
    });

    let errorMessage = 'Ha ocurrido un error inesperado';
    let errorCode = 'UNKNOWN_ERROR';
    let fieldErrors: { [key: string]: string } | undefined;

    if (error.error) {
      if (error.error.message) {
        errorMessage = error.error.message;
        errorCode = error.error.code || 'UNKNOWN_ERROR';
        fieldErrors = error.error.fieldErrors;
      } else if (error.error.message || error.error.mensaje) {
        errorMessage = error.error.message || error.error.mensaje || errorMessage;
      } else if (typeof error.error === 'string') {
        errorMessage = error.error;
      }
    }

    switch (error.status) {
      case 0:
        this.alertService.error('No se puede conectar al servidor. Verifica tu conexión.');
        break;

      case 401:
        this.handleUnauthorized(req);
        return throwError(() => error);

      case 403:
        errorMessage = error.error?.message || 'No tienes permisos para acceder a este recurso';
        this.alertService.error(errorMessage);
        break;

      case 404:
        if (!req.url.includes('favicon.ico')) {
          this.alertService.error(errorMessage);
        }
        break;

      case 400:
        if (fieldErrors && Object.keys(fieldErrors).length > 0) {
          const firstError = Object.values(fieldErrors)[0];
          this.alertService.error(firstError);
        } else {
          this.alertService.error(errorMessage);
        }
        break;

      case 405:
        this.alertService.error('El método HTTP utilizado no es soportado');
        break;

      case 409:
        this.alertService.error(errorMessage);
        break;

      case 422:
        if (fieldErrors && Object.keys(fieldErrors).length > 0) {
          const firstError = Object.values(fieldErrors)[0];
          this.alertService.error(firstError);
        } else {
          this.alertService.error(errorMessage);
        }
        break;

      case 500:
        this.alertService.error('Error interno del servidor. Intenta más tarde.');
        break;

      case 503:
        this.alertService.error('El servicio no está disponible. Intenta más tarde.');
        break;

      default:
        if (error.status >= 400) {
          this.alertService.error(errorMessage);
        }
        break;
    }

    if (fieldErrors && Object.keys(fieldErrors).length > 0) {
      console.warn('📋 Errores de validación:', fieldErrors);
    }

    return throwError(() => ({
      ...error,
      userMessage: errorMessage,
      code: errorCode,
      fieldErrors: fieldErrors
    }));
  }

  private handleUnauthorized(req: HttpRequest<any>): void {
    const currentUrl = this.router.url;

    console.warn('🔐 Sesión no autorizada:', {
      url: req.url,
      currentUrl: currentUrl
    });

    //  Limpiar sesión directamente (sin AuthService)
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('user');

    if (!currentUrl.includes('/login')) {
      this.alertService.warning('Tu sesión ha expirado. Inicia sesión nuevamente.');
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: currentUrl }
      });
    }
  }
}
