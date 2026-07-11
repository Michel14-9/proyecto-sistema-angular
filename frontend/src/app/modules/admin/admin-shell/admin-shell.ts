// src/app/modules/admin/admin-shell/admin-shell.ts
import { Component, OnInit, OnDestroy, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { LayoutService } from '../../../core/services/layout.service';
import { AlertService } from '../../../core/services/alert.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-shell.html',
  styleUrls: ['./admin-shell.css']
})
export class AdminShellComponent implements OnInit, AfterViewInit, OnDestroy {
  username: string = '';
  seccionActual: string = 'dashboard';
  fechaActual: string = '';
  horaActual: string = '';
  private intervalId: any;

  menuItems = [
    { path: 'dashboard', icon: 'tachometer-alt', label: 'Dashboard' },
    { path: 'menu', icon: 'concierge-bell', label: 'Gestionar Menú' },
    { path: 'users', icon: 'users', label: 'Gestionar Usuarios' },
    { path: 'reports', icon: 'chart-bar', label: 'Reportes' },
    { path: 'estadisticas', icon: 'chart-pie', label: 'Estadísticas Avanzadas' }
  ];

  constructor(
    private authService: AuthService,
    private router: Router,
    private layoutService: LayoutService,
    public alertService: AlertService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    this.username = this.authService.getUsername();
    this.layoutService.hideHeaderAndFooter();
    this.actualizarHoraYFecha();
    this.intervalId = setInterval(() => this.actualizarHoraYFecha(), 1000);

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      const path = event.url.split('/').pop();
      const menuItem = this.menuItems.find(item => item.path === path);
      if (menuItem) {
        this.seccionActual = menuItem.path;
      }
    });
  }


  ngAfterViewInit(): void {
    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    this.layoutService.showHeaderAndFooter();
    if (this.intervalId) clearInterval(this.intervalId);
  }

  actualizarHoraYFecha(): void {
    const ahora = new Date();
    this.fechaActual = ahora.toLocaleDateString('es-ES', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
    this.horaActual = ahora.toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }

  logout(): void {
    if (confirm('¿Cerrar sesión?')) {
      this.authService.logout().subscribe({
        next: () => this.router.navigate(['/login']),
        error: () => this.router.navigate(['/login'])
      });
    }
  }

  isActive(path: string): boolean {
    return this.seccionActual === path;
  }
}
