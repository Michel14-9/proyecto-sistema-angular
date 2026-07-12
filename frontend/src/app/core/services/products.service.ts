import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

export interface Producto {
  id: number;
  nombre: string;
  tipo: string;
  precio: number;
  descripcion: string;
  imagenUrl: string;
  activo?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ProductsService {
  constructor(private api: ApiService) {}

  getAll(): Observable<Producto[]> {
    return this.api.javaGet<Producto[]>('/admin/productos');
  }

  create(formData: FormData): Observable<any> {
    return this.api.javaPostFormData('/admin/productos/guardar', formData);
  }

  update(id: number, formData: FormData): Observable<any> {
    return this.api.javaPostFormData(`/admin/productos/actualizar/${id}`, formData);
  }

  delete(id: number): Observable<any> {
    return this.api.javaDelete(`/admin/productos/eliminar/${id}`);
  }
}
