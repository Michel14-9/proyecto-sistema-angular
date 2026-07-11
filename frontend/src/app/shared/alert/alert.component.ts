import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { AlertService, Alerta } from '../../core/services/alert.service'; // ✅ RUTA CORRECTA

@Component({
  selector: 'app-alert',
  standalone: true, // ✅ IMPORTANTE: DEBE SER STANDALONE
  imports: [CommonModule], // ✅ IMPORTS NECESARIOS
  template: `
    <div *ngIf="alerta"
         class="alert alert-{{ alerta.tipo }} alert-dismissible fade show position-fixed top-0 end-0 m-3"
         role="alert"
         style="z-index: 9999; min-width: 300px; max-width: 500px; animation: slideIn 0.3s ease-out; box-shadow: 0 4px 12px rgba(0,0,0,0.15); border: none; border-radius: 8px;">
      <div class="d-flex align-items-start">
        <div class="flex-grow-1">
          <strong>{{ getTitulo() }}</strong>
          <span class="ms-2">{{ alerta.mensaje }}</span>
        </div>
        <button type="button" class="btn-close" (click)="cerrar()" style="padding: 0.25rem 0.5rem;"></button>
      </div>
    </div>
  `,
  styles: [`
    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
    .alert {
      border: none;
      border-radius: 8px;
      min-width: 300px;
    }
    .alert-success {
      background-color: #d4edda;
      color: #155724;
      border-left: 4px solid #28a745;
    }
    .alert-danger {
      background-color: #f8d7da;
      color: #721c24;
      border-left: 4px solid #dc3545;
    }
    .alert-warning {
      background-color: #fff3cd;
      color: #856404;
      border-left: 4px solid #ffc107;
    }
    .alert-info {
      background-color: #d1ecf1;
      color: #0c5460;
      border-left: 4px solid #17a2b8;
    }
    .btn-close {
      font-size: 0.8rem;
    }
  `]
})
export class AlertComponent implements OnInit, OnDestroy {
  alerta: Alerta | null = null;
  private subscription!: Subscription;
  private timeoutId: any;

  constructor(private alertService: AlertService) {}

  ngOnInit(): void {
    this.subscription = this.alertService.alerta$.subscribe((alerta: Alerta | null) => {
      this.alerta = alerta;

      if (this.timeoutId) {
        clearTimeout(this.timeoutId);
        this.timeoutId = null;
      }

      if (alerta?.duracion) {
        this.timeoutId = setTimeout(() => {
          this.cerrar();
        }, alerta.duracion);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
  }

  cerrar(): void {
    this.alertService.limpiar();
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
  }

  getTitulo(): string {
    if (!this.alerta) return '';
    const titulos: Record<string, string> = {
      'success': '✅ Éxito',
      'danger': '❌ Error',
      'warning': '⚠️ Advertencia',
      'info': 'ℹ️ Información'
    };
    return titulos[this.alerta.tipo] || '';
  }
}
