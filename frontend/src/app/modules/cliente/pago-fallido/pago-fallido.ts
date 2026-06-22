// src/app/modules/cliente/pago-fallido/pago-fallido.ts
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-pago-fallido',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './pago-fallido.html',
  styleUrls: ['./pago-fallido.css'],
  encapsulation: ViewEncapsulation.None
})
export class PagoFallidoComponent implements OnInit {
  mensajeError: string = '';
  codigoError: string = '';
  isAuthenticated: boolean = false;
  username: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    // Obtener parámetros de la URL (MercadoPago envía información del error)
    this.route.queryParams.subscribe(params => {
      this.mensajeError = params['message'] || params['mensaje'] || 'Hubo un problema al procesar tu pago.';
      this.codigoError = params['status'] || params['codigo'] || '';

      console.log('❌ Error en pago:', {
        mensaje: this.mensajeError,
        codigo: this.codigoError
      });
    });
  }

  irAlCarrito(): void {
    this.router.navigate(['/carrito']);
  }

  irAlMenu(): void {
    this.router.navigate(['/menu']);
  }

  irAlInicio(): void {
    this.router.navigate(['/']);
  }

  intentarNuevamente(): void {
    this.router.navigate(['/checkout']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
