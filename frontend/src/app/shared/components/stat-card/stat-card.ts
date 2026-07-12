// src/app/shared/components/stat-card/stat-card.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="stat-card" [ngClass]="colorClass">
      <div class="stat-icon">
        <!-- ✅ Si es soles, mostrar "S/" en lugar de ícono -->
        <span *ngIf="icon === 'soles'" class="icon-soles">S/</span>
        <!-- ✅ Si no es soles, mostrar el ícono de FontAwesome -->
        <i *ngIf="icon !== 'soles'" [class]="'fas fa-' + icon"></i>
      </div>
      <div class="stat-content">
        <div class="stat-number">
          <span *ngIf="prefix">{{ prefix }}</span>
          <ng-container *ngIf="isCurrency; else numberFormat">
            {{ value | number:'1.2-2' }}
          </ng-container>
          <ng-template #numberFormat>
            {{ value | number:'1.0-0' }}
          </ng-template>
          <span *ngIf="suffix">{{ suffix }}</span>
        </div>
        <div class="stat-label">{{ label }}</div>
      </div>
    </div>
  `,
  styles: [`
    .stat-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 15px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      transition: transform 0.2s;
    }
    .stat-card:hover { transform: translateY(-5px); }
    .stat-icon {
      width: 50px;
      height: 50px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      color: white;
      flex-shrink: 0;
    }
    /* ✅ Estilo para el texto "S/" en el ícono */
    .icon-soles {
      font-size: 20px;
      font-weight: 700;
      color: white;
      letter-spacing: -0.5px;
      line-height: 1;
    }
    .stat-number {
      font-size: 28px;
      font-weight: bold;
    }
    .stat-label {
      color: #6c757d;
      font-size: 14px;
    }
    .stat-card.primary .stat-icon { background: #007bff; }
    .stat-card.success .stat-icon { background: #28a745; }
    .stat-card.warning .stat-icon { background: #ffc107; }
    .stat-card.info .stat-icon { background: #17a2b8; }
    .stat-card.danger .stat-icon { background: #dc3545; }
  `]
})
export class StatCardComponent {
  @Input() value: number = 0;
  @Input() label: string = '';
  @Input() icon: string = 'chart-line';
  @Input() color: 'primary' | 'success' | 'warning' | 'info' | 'danger' = 'primary';
  @Input() prefix: string = '';
  @Input() suffix: string = '';
  @Input() isCurrency: boolean = false;

  get colorClass(): string {
    return this.color;
  }
}
