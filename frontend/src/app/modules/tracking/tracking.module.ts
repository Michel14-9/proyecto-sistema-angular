import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TrackingRoutingModule } from './tracking-routing-module';
import { SeguimientoComponent } from './pages/seguimiento/seguimiento';

@NgModule({
  declarations: [SeguimientoComponent],
  imports: [CommonModule, RouterModule, TrackingRoutingModule]
})
export class TrackingModule {}