import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-cocinero',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cocinero.html',
  styleUrls: ['./cocinero.css']
})
export class CocineroComponent {
  constructor() {
    console.log('CocineroComponent cargado');
  }
}
