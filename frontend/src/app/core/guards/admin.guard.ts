import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AlertService } from '../services/alert.service';

@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
    private alertService: AlertService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    if (!this.authService.isAuthenticated()) {
      const returnUrl = state.url;
      this.alertService.warning('Debes iniciar sesión como administrador');
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: returnUrl }
      });
      return false;
    }

    const userRole = this.authService.getRole();
    let rolNormalizado = userRole || '';
    if (rolNormalizado.startsWith('ROLE_')) {
      rolNormalizado = rolNormalizado.substring(5);
    }


    if (rolNormalizado.toLowerCase() === 'admin') {
      return true;
    }

    this.alertService.error(' Acceso denegado. Solo administradores pueden acceder.');
    this.router.navigate(['/403']);
    return false;
  }
}
