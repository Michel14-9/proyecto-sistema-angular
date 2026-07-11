import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class PredictionsService {
  constructor(private api: ApiService) {}

  entrenarModelo(): Observable<any> {
    return this.api.pythonPost('/api/predicciones/entrenar', {});
  }

  getPredicciones(dias: number = 7): Observable<any> {
    return this.api.pythonGet(`/api/predicciones/ventas?dias=${dias}`);
  }

  getTendencias(): Observable<any> {
    return this.api.pythonGet('/api/predicciones/tendencias');
  }
}
