import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardCocinaComponent } from './pages/dashboard-cocina/dashboard-cocina';

const routes: Routes = [
  {
    path: '',
    component: DashboardCocinaComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class CocineroRoutingModule { }