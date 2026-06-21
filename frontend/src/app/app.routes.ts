// src/app/app.routes.ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./modules/publico/index/index').then(m => m.IndexComponent) },
  { path: 'login', loadComponent: () => import('./modules/auth/login/login').then(m => m.LoginComponent) },
  { path: 'registrate', loadComponent: () => import('./modules/auth/registrate/registrate').then(m => m.RegistrateComponent) },
  { path: 'admin-menu', loadComponent: () => import('./modules/admin/admin-menu/admin-menu').then(m => m.AdminMenuComponent) },
  { path: 'cocinero', loadComponent: () => import('./modules/cocinero/cocinero/cocinero').then(m => m.CocineroComponent) },
  { path: 'menu', loadComponent: () => import('./modules/publico/menu/menu.component').then(m => m.MenuComponent) },
  { path: 'nuestros-locales', loadComponent: () => import('./modules/publico/locales/locales').then(m => m.LocalesComponent) },
  { path: 'favoritos', loadComponent: () => import('./modules/cliente/mis-favoritos/mis-favoritos').then(m => m.MisFavoritosComponent) },
  { path: 'carrito', loadComponent: () => import('./modules/cliente/carrito/carrito').then(m => m.CarritoComponent) },
  { path: 'checkout', loadComponent: () => import('./modules/cliente/checkout/checkout').then(m => m.CheckoutComponent) },

  // ✅ Rutas de pago
  { path: 'pago-exitoso', loadComponent: () => import('./modules/cliente/pago-exitoso/pago-exitoso').then(m => m.PagoExitosoComponent) },
  { path: 'pago-fallido', loadComponent: () => import('./modules/cliente/pago-fallido/pago-fallido').then(m => m.PagoFallidoComponent) },

  { path: '**', redirectTo: '' }
];
