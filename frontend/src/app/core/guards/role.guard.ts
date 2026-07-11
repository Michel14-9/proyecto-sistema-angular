import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AlertService } from '../services/alert.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
    private alertService: AlertService
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const expectedRoles = route.data['roles'] as Array<string>;
    const userRole = this.authService.getRole();

    //  Si no está autenticado, redirigir a login
    if (!this.authService.isAuthenticated()) {
      this.alertService.warning('Debes iniciar sesión');
      this.router.navigate(['/login']);
      return false;
    }

    //  Verificar si el usuario tiene el rol requerido
    if (expectedRoles && expectedRoles.includes(userRole || '')) {
      return true;
    }

    //  Mostrar alerta de acceso denegado
    this.alertService.error(' No tienes permisos para acceder a esta página');

    //  Redirigir a la página principal
    this.router.navigate(['/']);

    return false;
  }
}
