// src/app/modules/auth/registrate/registrate.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-registrate',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './registrate.html',
  styleUrls: ['./registrate.css']
})
export class RegistrateComponent {
  constructor() {
    console.log('RegistrateComponent cargado');
  }
}
