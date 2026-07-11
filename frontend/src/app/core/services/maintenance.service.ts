import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MaintenanceService {
  private maintenanceModeSubject = new BehaviorSubject<boolean>(false);
  public maintenanceMode$ = this.maintenanceModeSubject.asObservable();

  private messageSubject = new BehaviorSubject<string>('Estamos realizando mejoras en el sistema.');
  public message$ = this.messageSubject.asObservable();

  private estimatedTimeSubject = new BehaviorSubject<string>('15-30 minutos');
  public estimatedTime$ = this.estimatedTimeSubject.asObservable();

  activarMantenimiento(mensaje?: string, tiempo?: string): void {
    this.maintenanceModeSubject.next(true);
    if (mensaje) this.messageSubject.next(mensaje);
    if (tiempo) this.estimatedTimeSubject.next(tiempo);

    //  Guardar en localStorage para persistencia
    localStorage.setItem('maintenance_mode', 'true');
    localStorage.setItem('maintenance_message', mensaje || 'Estamos realizando mejoras en el sistema.');
    localStorage.setItem('maintenance_time', tiempo || '15-30 minutos');

    console.log('🔧 Modo mantenimiento ACTIVADO');
  }

  desactivarMantenimiento(): void {
    this.maintenanceModeSubject.next(false);
    localStorage.removeItem('maintenance_mode');
    localStorage.removeItem('maintenance_message');
    localStorage.removeItem('maintenance_time');
    console.log(' Modo mantenimiento DESACTIVADO');
  }

  isMaintenanceMode(): boolean {

    return this.maintenanceModeSubject.value || localStorage.getItem('maintenance_mode') === 'true';
  }

  getMessage(): string {
    return localStorage.getItem('maintenance_message') || 'Estamos realizando mejoras en el sistema.';
  }

  getEstimatedTime(): string {
    return localStorage.getItem('maintenance_time') || '15-30 minutos';
  }
}
