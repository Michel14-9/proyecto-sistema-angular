import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrlJava = 'http://localhost:8080';
  private baseUrlPython = 'http://localhost:5000';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders().set('Content-Type', 'application/json');
  }

  // ============ JAVA ============
  javaGet<T>(endpoint: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrlJava}${endpoint}`, {
      headers: this.getHeaders(),
      withCredentials: true
    });
  }

  javaPost<T>(endpoint: string, body: any): Observable<T> {
    return this.http.post<T>(`${this.baseUrlJava}${endpoint}`, body, {
      headers: this.getHeaders(),
      withCredentials: true
    });
  }

  javaPut<T>(endpoint: string, body: any): Observable<T> {
    return this.http.put<T>(`${this.baseUrlJava}${endpoint}`, body, {
      headers: this.getHeaders(),
      withCredentials: true
    });
  }

  javaDelete<T>(endpoint: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrlJava}${endpoint}`, {
      withCredentials: true
    });
  }

  javaPostFormData<T>(endpoint: string, formData: FormData): Observable<T> {
    return this.http.post<T>(`${this.baseUrlJava}${endpoint}`, formData, {
      withCredentials: true
    });
  }

  // ============ PYTHON ============
  pythonGet<T>(endpoint: string): Observable<T> {
    return this.http.get<T>(`${this.baseUrlPython}${endpoint}`, {
      headers: this.getHeaders(),
      withCredentials: true
    });
  }

  pythonPost<T>(endpoint: string, body: any): Observable<T> {
    return this.http.post<T>(`${this.baseUrlPython}${endpoint}`, body, {
      headers: this.getHeaders(),
      withCredentials: true
    });
  }
}
