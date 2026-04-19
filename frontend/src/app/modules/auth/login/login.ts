// src/app/modules/auth/login/login.ts
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
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

  loginEmail: string = '';
  loginPassword: string = '';
  errorLogin: boolean = false;
  logoutExitoso: boolean = false;
  cargando: boolean = false;
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
    if (window.location.pathname === '/login-success') {
      this.router.navigate(['/']);
      return;
    }

    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/']);
      return;
    }

    this.route.queryParams.subscribe(params => {
      if (params['logout']) {
        this.logoutExitoso = true;
        setTimeout(() => this.logoutExitoso = false, 4000);
      }
    });

    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    this.carritoService.getTotal().subscribe({
      next: (total: number) => {
        this.totalCarrito = total;
      }
    });
  }

  iniciarSesion(): void {
    if (!this.loginEmail || !this.loginPassword) {
      this.errorLogin = true;
      return;
    }

    this.cargando = true;
    this.errorLogin = false;

    this.authService.login(this.loginEmail, this.loginPassword).subscribe({
      next: (response: any) => {
        this.cargando = false;
        console.log('Respuesta:', response);

        if (response.success) {
          this.authService.saveSession('session_' + Date.now(), response.username, response.role);
          this.router.navigate(['/']);
        } else {
          this.errorLogin = true;
        }
      },
      error: (err: any) => {
        console.error('Error login:', err);
        this.errorLogin = true;
        this.cargando = false;
      }
    });
  }

  // ✅ Agrega este método
  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login'], { queryParams: { logout: true } });
      }
    });
  }
}
