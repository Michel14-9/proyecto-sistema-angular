// src/app/core/services/combo.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Combo {
  id: number;
  nombre: string;
  descripcion: string;
  precio: number;
  imagenUrl: string;
}

@Injectable({
  providedIn: 'root'
})
export class ComboService {
  private apiUrl = 'http://localhost:8080/api/combos';

  constructor(private http: HttpClient) {}

  getCombos(): Observable<Combo[]> {
    return this.http.get<Combo[]>(this.apiUrl);
  }

  getCombo(id: number): Observable<Combo> {
    return this.http.get<Combo>(`${this.apiUrl}/${id}`);
  }

  crearCombo(combo: FormData): Observable<Combo> {
    return this.http.post<Combo>(this.apiUrl, combo);
  }

  actualizarCombo(id: number, combo: FormData): Observable<Combo> {
    return this.http.put<Combo>(`${this.apiUrl}/${id}`, combo);
  }

  eliminarCombo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
