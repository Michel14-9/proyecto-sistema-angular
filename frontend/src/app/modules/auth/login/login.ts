// src/app/modules/auth/login/login.ts
import { Component, OnInit, AfterViewInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth';
import { CarritoService } from '../../../core/services/carrito';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  encapsulation: ViewEncapsulation.None
})
export class LoginComponent implements OnInit {

  // Formulario
  loginEmail: string = '';
  loginPassword: string = '';

  // Estados
  errorLogin: boolean = false;
  logoutExitoso: boolean = false;
  cargando: boolean = false;

  // Navbar
  isAuthenticated: boolean = false;
  username: string = '';
  totalCarrito: number = 0;

  constructor(
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    console.log('LoginComponent cargado');
  }

  ngOnInit(): void {
    // Equivalente a: if (window.location.pathname === "/login-success")
    if (window.location.pathname === '/login-success') {
      this.router.navigate(['/']);
      return;
    }

    // Si ya está logueado, redirigir al inicio
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/']);
      return;
    }

    // Verificar si viene de un logout
    this.route.queryParams.subscribe(params => {
      if (params['logout']) {
        this.logoutExitoso = true;
        setTimeout(() => this.logoutExitoso = false, 4000);
      }
    });

    // Datos navbar
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    // Total carrito
    this.carritoService.getTotal().subscribe({
      next: (total: number) => {
        this.totalCarrito = total;
      }
    });
  }

  iniciarSesion(): void {
    // Validar campos vacíos
    if (!this.loginEmail || !this.loginPassword) {
      this.errorLogin = true;
      return;
    }

    // Equivalente a: submitBtn.disabled = true + spinner
    this.cargando = true;
    this.errorLogin = false;

    this.authService.login({ username: this.loginEmail, password: this.loginPassword }).subscribe({
      next: (response: any) => {
        this.cargando = false;
        console.log('Login exitoso:', response);
        this.router.navigate(['/']);
      },
      error: (err: any) => {
        console.error('Error login:', err);
        this.errorLogin = true;

        // Equivalente al setTimeout de 5 segundos del JS original
        setTimeout(() => {
          this.cargando = false;
        }, 5000);
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login'], { queryParams: { logout: true } });
      }
    });
  }
}

