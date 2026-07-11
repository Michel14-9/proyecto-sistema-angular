import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { ConfigService } from '../services/config.service';

@Injectable()
export class MaintenanceInterceptor implements HttpInterceptor {
  private excludedRoutes = ['/mantenimiento', '/login', '/registrate', '/assets/'];

  constructor(
    private router: Router,
    private configService: ConfigService
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const isMaintenanceMode = this.configService.isMaintenanceMode();
    const currentUrl = this.router.url;
    const isExcluded = this.excludedRoutes.some(route => currentUrl.includes(route));

    if (isMaintenanceMode && !isExcluded && !currentUrl.includes('/mantenimiento')) {
      console.log('🔧 Redirigiendo a mantenimiento desde:', currentUrl);
      this.router.navigate(['/mantenimiento']);
    }

    return next.handle(req);
  }
}
