// src/app/modules/auth/login/login.ts

import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
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

  // ✅ Mapeo de roles a rutas (NUEVA ESTRUCTURA)
  private readonly roleRoutes: { [key: string]: string } = {
    'ROLE_ADMIN': '/admin/dashboard',    // ✅ Nueva ruta admin con dashboard
    'ADMIN': '/admin/dashboard',
    'admin': '/admin/dashboard',
    'ROLE_CAJERO': '/cajero',
    'CAJERO': '/cajero',
    'cajero': '/cajero',
    'ROLE_COCINERO': '/cocinero',
    'COCINERO': '/cocinero',
    'cocinero': '/cocinero',
    'ROLE_DELIVERY': '/delivery',
    'DELIVERY': '/delivery',
    'delivery': '/delivery',
    'ROLE_CLIENTE': '/',
    'CLIENTE': '/',
    'cliente': '/'
  };

  constructor(
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    if (window.location.pathname === '/login-success') {
      this.router.navigate(['/']);
      return;
    }

    if (this.authService.isAuthenticated()) {
      const rol = this.authService.getRole();
      console.log('🔍 Usuario ya autenticado, rol:', rol);
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
        console.log('🔍 Respuesta login:', response);

        if (response && response.status === "ok") {
          const rol = response.rol || 'ROLE_CLIENTE';
          const usuario = response.usuario || response.user || {};

          // Guardar sesión
          this.authService.saveSession(
            'session_' + Date.now(),
            usuario.username || usuario.email || this.loginEmail,
            rol
          );

          this.authService.saveUser(usuario);

          setTimeout(() => {
            this.carritoService.refrescarTotal();
          }, 500);

          // ✅ Redirigir según el rol (NUEVA ESTRUCTURA)
          console.log(`🔀 Redirigiendo con rol: ${rol}`);
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
        console.error('❌ Error en login:', err);
      }
    });
  }

  logout(): void {
    console.log('🔄 Cerrando sesión...');

    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('user');
    console.log('✅ localStorage limpiado');

    this.carritoService.limpiarLocal();
    console.log('✅ Carrito limpiado');

    this.isAuthenticated = false;
    this.username = '';
    this.totalCarrito = 0;

    document.cookie = 'JSESSIONID=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';

    this.http.post('http://localhost:8080/api/auth/logout', {}, {
      withCredentials: true
    }).subscribe({
      next: () => console.log('✅ Logout backend ok'),
      error: (err) => console.log('⚠️ Logout backend falló:', err.status)
    });

    this.logoutExitoso = true;
    setTimeout(() => {
      this.logoutExitoso = false;
    }, 4000);

    console.log('🔄 Redirigiendo al login...');
    this.router.navigate(['/login']);
  }

  // ✅ Método de redirección para la NUEVA ESTRUCTURA
  private redirigirPorRol(rol: string | null): void {
    console.log(`🔀 Redirigiendo con rol: ${rol}`);

    // Normalizar el rol
    let rolNormalizado = rol || '';

    // Si empieza con ROLE_, lo quitamos
    if (rolNormalizado.startsWith('ROLE_')) {
      rolNormalizado = rolNormalizado.substring(5);
    }

    // Buscar la ruta correspondiente
    let ruta = this.roleRoutes[rolNormalizado] || this.roleRoutes[rol || ''] || '/';

    console.log(`🔀 Navegando a: ${ruta}`);
    this.router.navigate([ruta]);
  }
}
