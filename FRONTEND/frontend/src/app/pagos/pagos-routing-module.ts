import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RegistroPagoComponent } from './registro-pago/registro-pago';

const routes: Routes = [
  { path: 'registro', component: RegistroPagoComponent },
{ path: 'registro/:idPedido', component: RegistroPagoComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PagosRoutingModule { }
