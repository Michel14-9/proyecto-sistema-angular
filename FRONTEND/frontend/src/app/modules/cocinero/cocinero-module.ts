import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { CocineroRoutingModule } from './cocinero-routing-module';

// Para standalone components, NO se declaran, solo se importa el módulo de rutas
@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    CocineroRoutingModule
  ],
  exports: [],
  declarations: []
})
export class CocineroModule { }