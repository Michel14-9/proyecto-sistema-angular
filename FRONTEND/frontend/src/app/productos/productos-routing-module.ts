import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FormProductoComponent } from './form-producto/form-producto';

const routes: Routes = [
  { path: 'nuevo', component: FormProductoComponent },
{ path: 'editar/:id', component: FormProductoComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProductosRoutingModule { }
