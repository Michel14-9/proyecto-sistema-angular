// src/app/app.routes.ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./modules/publico/index/index').then(m => m.IndexComponent) },
  { path: 'login', loadComponent: () => import('./modules/auth/login/login').then(m => m.LoginComponent) },
  { path: 'registrate', loadComponent: () => import('./modules/auth/registrate/registrate').then(m => m.RegistrateComponent) },
  { path: 'admin-menu', loadComponent: () => import('./modules/admin/admin-menu/admin-menu').then(m => m.AdminMenuComponent) },
  { path: 'cocinero', loadComponent: () => import('./modules/cocinero/cocinero/cocinero').then(m => m.CocineroComponent) },
  { path: 'menu', loadComponent: () => import('./modules/publico/menu/menu').then(m => m.MenuComponent) },
  { path: 'nuestros-locales', loadComponent: () => import('./modules/publico/locales/locales').then(m => m.LocalesComponent) },
  { path: '**', redirectTo: '' }
];
