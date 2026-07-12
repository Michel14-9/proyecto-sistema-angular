// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { AdminGuard } from './core/guards/admin.guard';
import { CajeroGuard } from './core/guards/cajero.guard';
import { CocineroGuard } from './core/guards/cocinero.guard';
import { DeliveryGuard } from './core/guards/delivery.guard';

export const routes: Routes = [

  {
    path: '',
    loadComponent: () => import('./modules/publico/index/index').then(m => m.IndexComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./modules/auth/login/login').then(m => m.LoginComponent)
  },
  {
    path: 'registrate',
    loadComponent: () => import('./modules/auth/registrate/registrate').then(m => m.RegistrateComponent)
  },
  {
    path: 'menu',
    loadComponent: () => import('./modules/publico/menu/menu.component').then(m => m.MenuComponent)
  },
  {
    path: 'nuestros-locales',
    loadComponent: () => import('./modules/publico/locales/locales').then(m => m.LocalesComponent)
  },

  //  RUTAS DE CLIENTE (requieren autenticación)
  {
    path: 'favoritos',
    loadComponent: () => import('./modules/cliente/mis-favoritos/mis-favoritos').then(m => m.MisFavoritosComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'carrito',
    loadComponent: () => import('./modules/cliente/carrito/carrito').then(m => m.CarritoComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'checkout',
    loadComponent: () => import('./modules/cliente/checkout/checkout').then(m => m.CheckoutComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'pago-exitoso',
    loadComponent: () => import('./modules/cliente/pago-exitoso/pago-exitoso').then(m => m.PagoExitosoComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'pago-fallido',
    loadComponent: () => import('./modules/cliente/pago-fallido/pago-fallido').then(m => m.PagoFallidoComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'mis-pedidos',
    loadComponent: () => import('./modules/cliente/mis-pedidos/mis-pedidos').then(m => m.MisPedidosComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'mis-direcciones',
    loadComponent: () => import('./modules/cliente/mis-direcciones/mis-direcciones').then(m => m.MisDireccionesComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'mis-datos',
    loadComponent: () => import('./modules/cliente/mis-datos/mis-datos').then(m => m.MisDatosComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'mis-cuentas',
    loadComponent: () => import('./modules/cliente/mis-cuentas/mis-cuentas').then(m => m.MisCuentasComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'sigue-tu-pedido',
    loadComponent: () => import('./modules/cliente/sigue-tu-pedido/sigue-tu-pedido').then(m => m.SigueTuPedidoComponent),
    canActivate: [AuthGuard]
  },

  //  RUTAS DE ADMIN (solo administradores)
  {
    path: 'admin',
    loadChildren: () => import('./modules/admin/admin.routes').then(m => m.ADMIN_ROUTES),
    canActivate: [AdminGuard]
  },

  //  RUTAS DE CAJERO (solo cajeros o administradores)
  {
    path: 'cajero',
    loadComponent: () => import('./modules/cajero/cajero/cajero').then(m => m.CajeroComponent),
    canActivate: [CajeroGuard]
  },

  //  RUTAS DE COCINERO (solo cocineros o administradores)
  {
    path: 'cocinero',
    loadComponent: () => import('./modules/cocinero/cocinero/cocinero').then(m => m.CocineroComponent),
    canActivate: [CocineroGuard]
  },

  //  RUTAS DE DELIVERY (solo delivery o administradores)
  {
    path: 'delivery',
    loadComponent: () => import('./modules/delivery/delivery/delivery').then(m => m.DeliveryComponent),
    canActivate: [DeliveryGuard]
  },

  //  PÁGINAS DE ERROR
    {
      path: '404',
      loadComponent: () => import('./modules/error/not-found/not-found.component')
        .then(m => m.NotFoundComponent)
    },
    {
      path: '403',
      loadComponent: () => import('./modules/error/forbidden/forbidden.component')
        .then(m => m.ForbiddenComponent)
    },
  {
      path: 'mantenimiento',
      loadComponent: () => import('./modules/error/maintenance/maintenance.component')
        .then(m => m.MaintenanceComponent)
    },

    //  CUALQUIER OTRA RUTA → 404
    { path: '**', redirectTo: '404' }
];
