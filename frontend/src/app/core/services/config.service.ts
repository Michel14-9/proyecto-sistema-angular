import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface AppConfig {
  maintenanceMode: boolean;
  maintenanceMessage: string;
  maintenanceTime: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  private config: AppConfig = {
    maintenanceMode: false,
    maintenanceMessage: 'Estamos realizando mejoras.',
    maintenanceTime: '15-30 minutos'
  };

  constructor(private http: HttpClient) {}

  async loadConfig(): Promise<void> {
    try {
      this.config = await firstValueFrom(
        this.http.get<AppConfig>('/assets/config/config.json')
      );
      console.log(' Configuración cargada:', this.config);
    } catch (error) {
      console.warn('⚠ Error al cargar configuración, usando valores por defecto');
    }
  }

  getConfig(): AppConfig {
    return this.config;
  }

  isMaintenanceMode(): boolean {
    return this.config.maintenanceMode;
  }

  getMaintenanceMessage(): string {
    return this.config.maintenanceMessage;
  }

  getMaintenanceTime(): string {
    return this.config.maintenanceTime;
  }

  async reloadConfig(): Promise<void> {
    await this.loadConfig();
  }
}
