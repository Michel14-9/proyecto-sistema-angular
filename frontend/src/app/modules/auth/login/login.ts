import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';
import { AlertService } from '../../../core/services/alert.service';

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

  //  Variable para guardar la URL de retorno
  returnUrl: string = '/';

  constructor(
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    if (window.location.pathname === '/login-success') {
      this.router.navigate(['/']);
      return;
    }

    //  OBTENER returnUrl DE LOS QUERY PARAMS
    this.route.queryParams.subscribe(params => {
      this.returnUrl = params['returnUrl'] || '/';
      console.log('🔙 Return URL capturada:', this.returnUrl);

      if (params['logout']) {
        this.logoutExitoso = true;
        setTimeout(() => this.logoutExitoso = false, 4000);
      }
    });

    if (this.authService.isAuthenticated()) {
      const rol = this.authService.getRole();
      console.log('🔍 Usuario ya autenticado, rol:', rol);
      this.redirigirPorRol(rol);
      return;
    } else {
      console.log(' Sin sesión, carrito en 0');
      this.carritoService.limpiarLocal();
      this.totalCarrito = 0;
    }

    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    this.carritoService.getTotal().subscribe(total => {
      this.totalCarrito = total;
      console.log(' Total del carrito actualizado:', total);
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
        console.log(' Respuesta login:', response);

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

          //  REDIRIGIR USANDO returnUrl PRIMERO
          console.log(` Return URL: ${this.returnUrl}`);
          console.log(` Rol del usuario: ${rol}`);

          this.redirigirDespuesDeLogin(rol);

        } else {
          this.errorLogin = true;
          this.errorMessage = response?.mensaje || 'Usuario o contraseña incorrectos';
        }
      },
      error: (err: any) => {
        this.errorLogin = true;
        this.errorMessage = err.error?.mensaje || 'Error al iniciar sesión. Intenta de nuevo.';
        this.cargando = false;
        console.error(' Error en login:', err);
      }
    });
  }

  //  Redirigir después del login
  private redirigirDespuesDeLogin(rol: string): void {
    // Si hay returnUrl y NO es la página de login, redirigir allí
    if (this.returnUrl && this.returnUrl !== '/login' && this.returnUrl !== '/') {
      console.log(` Redirigiendo a returnUrl: ${this.returnUrl}`);
      this.router.navigateByUrl(this.returnUrl);
      return;
    }

    // Si no hay returnUrl o es login, redirigir por rol
    this.redirigirPorRol(rol);
  }

  //  Método de redirección por rol - CORREGIDO
  private redirigirPorRol(rol: string | null): void {
    console.log(` Redirigiendo por rol: ${rol}`);

    if (!rol) {
      console.log('⚠ Rol es null, redirigiendo a inicio');
      this.router.navigate(['/']);
      return;
    }

    //  Normalizar el rol (quitar prefijo ROLE_ y convertir a minúsculas)
    let rolNormalizado = rol;
    if (rolNormalizado.startsWith('ROLE_')) {
      rolNormalizado = rolNormalizado.substring(5);
    }
    rolNormalizado = rolNormalizado.toLowerCase();

    console.log(` Rol normalizado: ${rolNormalizado}`);

    //  Mapeo de roles a rutas (con keys en minúsculas)
    const roleRoutes: { [key: string]: string } = {
      'admin': '/admin/dashboard',
      'cajero': '/cajero',
      'cocinero': '/cocinero',
      'delivery': '/delivery',
      'cliente': '/'
    };

    // Buscar la ruta correspondiente
    let ruta = roleRoutes[rolNormalizado] || '/';

    console.log(` Navegando a: ${ruta}`);
    this.router.navigate([ruta]);
  }

  logout(): void {
    console.log(' Cerrando sesión...');

    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('user');
    console.log(' localStorage limpiado');

    this.carritoService.limpiarLocal();
    console.log(' Carrito limpiado');

    this.isAuthenticated = false;
    this.username = '';
    this.totalCarrito = 0;

    document.cookie = 'JSESSIONID=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';

    this.http.post('http://localhost:8080/api/auth/logout', {}, {
      withCredentials: true
    }).subscribe({
      next: () => console.log(' Logout backend ok'),
      error: (err) => console.log(' Logout backend falló:', err.status)
    });

    this.logoutExitoso = true;
    setTimeout(() => {
      this.logoutExitoso = false;
    }, 4000);

    console.log(' Redirigiendo al login...');
    this.router.navigate(['/login']);
  }
}
