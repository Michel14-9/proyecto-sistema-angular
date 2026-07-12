import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Alerta {
  mensaje: string;
  tipo: 'success' | 'danger' | 'warning' | 'info';
  titulo?: string;
  duracion?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private alertaSubject = new BehaviorSubject<Alerta | null>(null);
  alerta$ = this.alertaSubject.asObservable();

  private timeoutId: any;

  mostrar(mensaje: string, tipo: Alerta['tipo'] = 'success', duracion: number = 5000): void {
    this.alertaSubject.next({ mensaje, tipo, duracion });

    if (this.timeoutId) clearTimeout(this.timeoutId);
    this.timeoutId = setTimeout(() => this.limpiar(), duracion);
  }

  success(mensaje: string, titulo?: string, duracion: number = 3000): void {
    this.mostrar(mensaje, 'success', duracion);
  }

  error(mensaje: string, titulo?: string, duracion: number = 5000): void {
    this.mostrar(mensaje, 'danger', duracion);
  }

  warning(mensaje: string, titulo?: string, duracion: number = 4000): void {
    this.mostrar(mensaje, 'warning', duracion);
  }

  info(mensaje: string, titulo?: string, duracion: number = 3000): void {
    this.mostrar(mensaje, 'info', duracion);
  }

  limpiar(): void {
    this.alertaSubject.next(null);
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
  }
}
