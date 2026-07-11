import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LayoutService } from '../../../core/services/layout.service';
import { ConfigService } from '../../../core/services/config.service';

@Component({
  selector: 'app-maintenance',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './maintenance.component.html',
  styleUrls: ['./maintenance.component.css']
})
export class MaintenanceComponent implements OnInit, OnDestroy {
  mensaje: string = 'Estamos realizando mejoras en el sistema.';
  tiempoEstimado: string = '15-30 minutos';
  fechaActualizacion: string = '';

  constructor(
    private layoutService: LayoutService,
    private configService: ConfigService
  ) {}

  ngOnInit(): void {
    this.layoutService.hideHeaderAndFooter();

    const config = this.configService.getConfig();
    this.mensaje = config.maintenanceMessage;
    this.tiempoEstimado = config.maintenanceTime;

    const ahora = new Date();
    this.fechaActualizacion = ahora.toLocaleString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  ngOnDestroy(): void {
    this.layoutService.showHeaderAndFooter();
  }

  recargar(): void {
    this.configService.reloadConfig().then(() => {
      window.location.reload();
    });
  }
}
