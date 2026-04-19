// src/app/modules/publico/locales/locales.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-locales',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './locales.html',  // ← archivo correcto
  styleUrls: ['./locales.css']
})
export class LocalesComponent implements OnInit {

  constructor() {}

  ngOnInit(): void {
    // tu lógica aquí
  }
}
