import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

export interface Usuario {
  id: number;
  nombres: string;
  apellidos: string;
  tipoDocumento: string;
  numeroDocumento: string;
  telefono: string;
  fechaNacimiento: string;
  username: string;
  rol: string;
  password?: string;
  activo?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class UsersService {
  constructor(private api: ApiService) {}

  getAll(): Observable<Usuario[]> {
    return this.api.javaGet<Usuario[]>('/admin/usuarios');
  }

  create(formData: FormData): Observable<any> {
    return this.api.javaPostFormData('/admin/usuarios/guardar', formData);
  }

  delete(id: number): Observable<any> {
    return this.api.javaDelete(`/admin/usuarios/eliminar/${id}`);
  }
}
