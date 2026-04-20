// src/app/modules/admin/admin-menu/admin-menu.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-menu',
  standalone: true,  // ← Agrega esto
  imports: [CommonModule],  // ← Agrega CommonModule
  templateUrl: './admin-menu.html',
  styleUrls: ['./admin-menu.css']  // ← Cambia styleUrl a styleUrls
})
export class AdminMenuComponent {  // ← Cambia el nombre de la clase
  constructor() {
    console.log('AdminMenuComponent cargado');
  }
}
