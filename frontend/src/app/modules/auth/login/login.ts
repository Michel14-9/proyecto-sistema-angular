// src/app/modules/auth/login/login.ts

import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http'; // ✅ Agregar
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  encapsulation: ViewEncapsulation.None
})
export class LoginComponent implements OnInit {

  loginEmail: string = '';
  loginPassword: string = '';
  errorLogin: boolean = false;
  errorMessage: string = '';
  logoutExitoso: boolean = false;
  cargando: boolean = false;
  isAuthenticated: boolean = false;
  username: string = '';
  totalCarrito: number = 0;

  constructor(
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient // ✅ Agregar
  ) {}

  ngOnInit(): void {
    if (window.location.pathname === '/login-success') {
      this.router.navigate(['/']);
      return;
    }

    if (this.authService.isAuthenticated()) {
      const rol = this.authService.getRole();
      this.redirigirPorRol(rol);
      return;
    } else {
      console.log('🔄 Sin sesión, carrito en 0');
      this.carritoService.limpiarLocal();
      this.totalCarrito = 0;
    }

    this.route.queryParams.subscribe(params => {
      if (params['logout']) {
        this.logoutExitoso = true;
        setTimeout(() => this.logoutExitoso = false, 4000);
      }
    });

    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    this.carritoService.getTotal().subscribe(total => {
      this.totalCarrito = total;
      console.log('💰 Total del carrito actualizado:', total);
    });
  }

  iniciarSesion(): void {
    if (!this.loginEmail || !this.loginPassword) {
      this.errorLogin = true;
      this.errorMessage = 'Por favor, ingresa tu correo y contraseña';
      return;
    }

    this.cargando = true;
    this.errorLogin = false;
    this.errorMessage = '';

    this.authService.login(this.loginEmail, this.loginPassword).subscribe({
      next: (response: any) => {
        this.cargando = false;

        if (response && response.status === "ok") {
          const rol = response.rol || 'ROLE_CLIENTE';
          this.authService.saveSession('session_' + Date.now(), response.usuario, rol);

          setTimeout(() => {
            this.carritoService.refrescarTotal();
          }, 500);

          this.redirigirPorRol(rol);
        } else {
          this.errorLogin = true;
          this.errorMessage = response?.mensaje || 'Usuario o contraseña incorrectos';
        }
      },
      error: (err: any) => {
        this.errorLogin = true;
        this.errorMessage = err.error?.mensaje || 'Error al iniciar sesión. Intenta de nuevo.';
        this.cargando = false;
      }
    });
  }

  // ✅ MÉTODO LOGOUT CORREGIDO - CON MUCHOS LOGS
  logout(): void {
    console.log('🔄 ========================================');
    console.log('🔄 LOGOUT LLAMADO DESDE EL COMPONENTE');
    console.log('🔄 ========================================');
    console.log('🔄 isAuthenticated antes:', this.isAuthenticated);
    console.log('🔄 Token antes:', localStorage.getItem('token'));

    // ✅ 1. Limpiar localStorage inmediatamente
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    console.log('✅ localStorage limpiado');

    // ✅ 2. Limpiar carrito
    this.carritoService.limpiarLocal();
    console.log('✅ Carrito limpiado');

    // ✅ 3. Actualizar estado local
    this.isAuthenticated = false;
    this.username = '';
    this.totalCarrito = 0;
    console.log('✅ Estado local actualizado');

    // ✅ 4. Eliminar cookie manualmente
    document.cookie = 'JSESSIONID=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    console.log('✅ Cookies eliminadas');

    // ✅ 5. Intentar logout en el backend (no esperar respuesta)
    this.http.post('http://localhost:8080/api/auth/logout', {}, {
      withCredentials: true
    }).subscribe({
      next: () => console.log('✅ Logout backend ok'),
      error: (err) => console.log('⚠️ Logout backend falló:', err.status)
    });

    // ✅ 6. Mostrar mensaje de éxito
    this.logoutExitoso = true;
    setTimeout(() => {
      this.logoutExitoso = false;
    }, 4000);

    // ✅ 7. Redirigir al login
    console.log('🔄 Redirigiendo al login...');
    this.router.navigate(['/login']);

    // ✅ 8. Recargar la página para limpiar todo
    setTimeout(() => {
      console.log('🔄 Recargando página...');
      window.location.reload();
    }, 200);
  }

  private redirigirPorRol(rol: string | null): void {
    const rutas: { [key: string]: string } = {
      'ROLE_ADMIN': '/admin-menu',
      'ROLE_CAJERO': '/cajero',
      'ROLE_COCINERO': '/cocinero',
      'ROLE_DELIVERY': '/delivery',
      'ROLE_CLIENTE': '/'
    };

    const ruta = rutas[rol || ''] || '/';
    this.router.navigate([ruta]);
  }
}
