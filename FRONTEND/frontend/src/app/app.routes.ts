import { Routes } from '@angular/router';
import { AuthGuard } from './auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/productos',
    pathMatch: 'full'
  },

  { path: '', 
    loadChildren: () => import('./index/index-module').then(m => m.IndexModule) 
  },
  {
    path: 'auth',
    loadChildren: () => import('./auth/auth.module').then(m => m.AuthModule)
  },
  {
    path: 'productos',
    loadChildren: () => import('./productos/productos.module').then(m => m.ProductosModule)
  },
  {
    path: 'carrito',
    loadChildren: () => import('./carrito/carrito.module').then(m => m.CarritoModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'pedidos',
    loadChildren: () => import('./pedidos/pedidos.module').then(m => m.PedidosModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'pagos',
    loadChildren: () => import('./pagos/pagos.module').then(m => m.PagosModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard],
    data: { roles: ['administrador'] }
  },
  {
    path: 'reportes',
    loadChildren: () => import('./reportes/reportes.module').then(m => m.ReportesModule),
    canActivate: [AuthGuard],
    data: { roles: ['administrador'] }
  },
  {
    path: '**',
    redirectTo: '/productos'
  }
];