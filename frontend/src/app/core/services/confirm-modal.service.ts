// src/app/core/services/confirm-modal.service.ts
import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface ConfirmModalData {
  titulo: string;
  mensaje: string;
  accion: () => void;
}

@Injectable({
  providedIn: 'root'
})
export class ConfirmModalService {
  private confirmSubject = new Subject<ConfirmModalData>();
  confirm$ = this.confirmSubject.asObservable();

  confirmar(titulo: string, mensaje: string, accion: () => void): void {
    this.confirmSubject.next({ titulo, mensaje, accion });
  }
}
