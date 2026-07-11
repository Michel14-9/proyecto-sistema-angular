// src/app/app.routes.ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./modules/publico/index/index').then(m => m.IndexComponent) },
  { path: 'login', loadComponent: () => import('./modules/auth/login/login').then(m => m.LoginComponent) },
  { path: 'registrate', loadComponent: () => import('./modules/auth/registrate/registrate').then(m => m.RegistrateComponent) },
  { path: 'menu', loadComponent: () => import('./modules/publico/menu/menu.component').then(m => m.MenuComponent) },
  { path: 'nuestros-locales', loadComponent: () => import('./modules/publico/locales/locales').then(m => m.LocalesComponent) },
  { path: 'favoritos', loadComponent: () => import('./modules/cliente/mis-favoritos/mis-favoritos').then(m => m.MisFavoritosComponent) },
  { path: 'carrito', loadComponent: () => import('./modules/cliente/carrito/carrito').then(m => m.CarritoComponent) },
  { path: 'checkout', loadComponent: () => import('./modules/cliente/checkout/checkout').then(m => m.CheckoutComponent) },

  { path: 'pago-exitoso', loadComponent: () => import('./modules/cliente/pago-exitoso/pago-exitoso').then(m => m.PagoExitosoComponent) },
  { path: 'pago-fallido', loadComponent: () => import('./modules/cliente/pago-fallido/pago-fallido').then(m => m.PagoFallidoComponent) },

  { path: 'mis-pedidos', loadComponent: () => import('./modules/cliente/mis-pedidos/mis-pedidos').then(m => m.MisPedidosComponent) },
  { path: 'mis-direcciones', loadComponent: () => import('./modules/cliente/mis-direcciones/mis-direcciones').then(m => m.MisDireccionesComponent) },
  { path: 'mis-datos', loadComponent: () => import('./modules/cliente/mis-datos/mis-datos').then(m => m.MisDatosComponent) },
  { path: 'mis-cuentas', loadComponent: () => import('./modules/cliente/mis-cuentas/mis-cuentas').then(m => m.MisCuentasComponent) },

  {
    path: 'sigue-tu-pedido',
    loadComponent: () => import('./modules/cliente/sigue-tu-pedido/sigue-tu-pedido')
      .then(m => m.SigueTuPedidoComponent)
  },

  // ====== ADMIN (nueva estructura con rutas hijas) ======
  {
    path: 'admin',
    loadChildren: () => import('./modules/admin/admin.routes').then(m => m.ADMIN_ROUTES)
  },

  {
    path: 'cajero',
    loadComponent: () => import('./modules/cajero/cajero/cajero')
      .then(m => m.CajeroComponent)
  },

  {
    path: 'cocinero',
    loadComponent: () => import('./modules/cocinero/cocinero/cocinero')
      .then(m => m.CocineroComponent)
  },

  {
    path: 'delivery',
    loadComponent: () => import('./modules/delivery/delivery/delivery')
      .then(m => m.DeliveryComponent)
  },
  { path: '**', redirectTo: '' }
];
