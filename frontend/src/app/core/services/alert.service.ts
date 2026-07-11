// src/app/core/services/alert.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Alerta {
  mensaje: string;
  tipo: 'success' | 'danger' | 'warning' | 'info';
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private alertaSubject = new BehaviorSubject<Alerta | null>(null);
  alerta$ = this.alertaSubject.asObservable();

  private timeoutId: any;

  mostrar(mensaje: string, tipo: Alerta['tipo'] = 'success'): void {
    this.alertaSubject.next({ mensaje, tipo });

    if (this.timeoutId) clearTimeout(this.timeoutId);
    this.timeoutId = setTimeout(() => this.limpiar(), 5000);
  }

  limpiar(): void {
    this.alertaSubject.next(null);
  }
}
