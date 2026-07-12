// src/app/modules/admin/admin.routes.ts
import { Routes } from '@angular/router';
import { AdminShellComponent } from './admin-shell/admin-shell';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./admin-dashboard/admin-dashboard').then(m => m.AdminDashboardComponent)
      },
      {
        path: 'menu',
        loadComponent: () => import('./admin-productos/admin-productos').then(m => m.AdminProductosComponent)
      },
      {
        path: 'users',
        loadComponent: () => import('./admin-usuarios/admin-usuarios').then(m => m.AdminUsuariosComponent)
      },
      {
        path: 'reports',
        loadComponent: () => import('./admin-reportes/admin-reportes').then(m => m.AdminReportesComponent)
      },
      {
        path: 'estadisticas',
        loadComponent: () => import('./admin-estadisticas/admin-estadisticas').then(m => m.AdminEstadisticasComponent)
      }
    ]
  }
];
