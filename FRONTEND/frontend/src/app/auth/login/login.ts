import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { FooterComponent } from '../../shared/footer/footer.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  // Modelo del formulario
  credentials = {
    username: '',
    password: ''
  };

  // Estados de mensajes (equivalente a th:if="${param.error}" y th:if="${param.logout}")
  showError = false;
  showLogoutSuccess = false;
  isLoading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Detecta parámetros de URL: ?logout=true o ?error=true
    // Equivalente a th:if="${param.logout}" y th:if="${param.error}" en Thymeleaf
    this.route.queryParams.subscribe(params => {
      this.showLogoutSuccess = !!params['logout'];
      this.showError = !!params['error'];
    });

    // Si ya está autenticado, redirige al inicio
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/']);
    }
  }

  onSubmit(): void {
    if (!this.credentials.username || !this.credentials.password) {
      return;
    }

    this.isLoading = true;
    this.showError = false;

    this.authService.login(this.credentials).subscribe({
      next: (response) => {
        this.isLoading = false;
        // Redirige según el rol del usuario
        if (response.role === 'ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.showError = true;
        console.error('Error de login:', err);
      }
    });
  }

  dismissError(): void {
    this.showError = false;
  }

  dismissLogout(): void {
    this.showLogoutSuccess = false;
  }
}
