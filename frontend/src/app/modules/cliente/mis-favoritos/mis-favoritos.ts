// src/app/modules/cliente/mis-favoritos/mis-favoritos.ts
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';

interface Producto {
  id: number;
  nombre: string;
  precio: number;
  imagen: string;
  descripcion: string;
  disponible: boolean;
  categoria: string;
  tiempoPreparacion: number;
}

interface Favorito {
  id: number;
  producto: Producto;
  fechaAgregado: string;
  esActivo: boolean;
}

@Component({
  selector: 'app-mis-favoritos',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './mis-favoritos.html',
  styleUrls: ['./mis-favoritos.css'],
  encapsulation: ViewEncapsulation.None
})
export class MisFavoritosComponent implements OnInit {
  private apiUrl = 'http://localhost:8080';

  favoritos: Favorito[] = [];
  isLoading: boolean = true;
  errorMessage: string = '';

  // Para el header
  isAuthenticated: boolean = false;
  username: string = '';
  totalCarrito: number = 0;

  // Modal
  mostrarModal: boolean = false;
  favoritoAEliminar: Favorito | null = null;
  indexAEliminar: number = -1;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    this.carritoService.getTotal().subscribe(total => {
      this.totalCarrito = total;
    });

    if (this.isAuthenticated) {
      this.cargarFavoritos();
    } else {
      this.isLoading = false;
    }
  }

  private getCsrfToken(): string | null {
    const cookies = document.cookie.split(';');
    for (const cookie of cookies) {
      const [name, value] = cookie.trim().split('=');
      if (name === 'XSRF-TOKEN') {
        return decodeURIComponent(value);
      }
    }
    return null;
  }

  cargarFavoritos(): void {
    this.isLoading = true;
    this.errorMessage = '';

    console.log('🔄 Cargando favoritos...');

    this.http.get(`${this.apiUrl}/api/favoritos/listar`, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        console.log('📦 Respuesta del servidor:', response);

        if (response && response.success === true) {
          this.favoritos = response.favoritos || [];
          console.log(`✅ ${this.favoritos.length} favoritos cargados`);
        } else {
          this.errorMessage = response?.message || 'Error al cargar los favoritos';
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error al cargar favoritos:', error);

        if (error.status === 401) {
          this.errorMessage = '⚠️ Debes iniciar sesión para ver tus favoritos';
        } else {
          this.errorMessage = error.error?.message || 'Error al cargar los favoritos';
        }
        this.isLoading = false;
      }
    });
  }

  // ✅ ANIMACIÓN: Corazón flotante
  private mostrarCorazonFlotante(event: Event): void {
    const rect = (event.target as HTMLElement).getBoundingClientRect();
    const corazon = document.createElement('div');
    corazon.className = 'corazon-flotante';
    corazon.innerHTML = '❤️';
    corazon.style.left = rect.left + rect.width / 2 - 20 + 'px';
    corazon.style.top = rect.top + 'px';
    document.body.appendChild(corazon);
    setTimeout(() => corazon.remove(), 1300);
  }

  // ✅ ANIMACIÓN: Carrito flotante
  private mostrarCarritoFlotante(event: Event): void {
    const rect = (event.target as HTMLElement).getBoundingClientRect();
    const carrito = document.createElement('div');
    carrito.className = 'carrito-flotante';
    carrito.innerHTML = '🛒';
    carrito.style.left = rect.left + rect.width / 2 - 20 + 'px';
    carrito.style.top = rect.top + 'px';
    document.body.appendChild(carrito);
    setTimeout(() => carrito.remove(), 1300);
  }

  toggleFavorito(productoId: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }

    if (!this.isAuthenticated) {
      this.mostrarNotificacion('Debes iniciar sesión para gestionar favoritos', 'warning');
      return;
    }

    console.log('🔄 Toggle favorito:', productoId);

    const csrfToken = this.getCsrfToken();
    let headers = new HttpHeaders();
    if (csrfToken) {
      headers = headers.set('X-XSRF-TOKEN', csrfToken);
    }
    headers = headers.set('Content-Type', 'application/x-www-form-urlencoded');

    const body = new URLSearchParams();
    body.set('productoId', productoId.toString());

    this.http.post(`${this.apiUrl}/api/favoritos/toggle`,
      body.toString(),
      {
        headers: headers,
        withCredentials: true
      }
    ).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.cargarFavoritos();
          if (response.agregado && event) {
            this.mostrarCorazonFlotante(event);
            this.mostrarNotificacion('❤️ Agregado a favoritos', 'success');
          } else {
            this.mostrarNotificacion('💔 Eliminado de favoritos', 'info');
          }
        }
      },
      error: (error) => {
        console.error('❌ Error toggle favorito:', error);
        if (error.status === 403) {
          this.mostrarNotificacion('Error de seguridad. Recarga la página.', 'error');
        } else if (error.status === 400) {
          this.mostrarNotificacion('Error: Producto no válido', 'error');
        } else {
          this.mostrarNotificacion('Error al actualizar favoritos', 'error');
        }
      }
    });
  }

  confirmarEliminar(index: number): void {
    this.favoritoAEliminar = this.favoritos[index];
    this.indexAEliminar = index;
    this.mostrarModal = true;
  }

  eliminarFavoritoConfirmado(): void {
    if (this.favoritoAEliminar) {
      this.toggleFavorito(this.favoritoAEliminar.producto.id);
      this.mostrarModal = false;
      this.favoritoAEliminar = null;
      this.indexAEliminar = -1;
    }
  }

  limpiarFavoritos(): void {
    if (this.favoritos.length === 0) {
      this.mostrarNotificacion('No hay favoritos para limpiar', 'warning');
      return;
    }

    if (confirm('¿Estás seguro de que quieres eliminar todos tus productos favoritos?')) {
      const productosIds = this.favoritos.map(f => f.producto.id);
      let eliminados = 0;

      const csrfToken = this.getCsrfToken();
      let headers = new HttpHeaders();
      if (csrfToken) {
        headers = headers.set('X-XSRF-TOKEN', csrfToken);
      }
      headers = headers.set('Content-Type', 'application/x-www-form-urlencoded');

      productosIds.forEach(id => {
        const body = new URLSearchParams();
        body.set('productoId', id.toString());

        this.http.post(`${this.apiUrl}/api/favoritos/toggle`,
          body.toString(),
          {
            headers: headers,
            withCredentials: true
          }
        ).subscribe({
          next: (response: any) => {
            if (response.success && !response.agregado) {
              eliminados++;
            }
            if (eliminados === productosIds.length) {
              this.cargarFavoritos();
              this.mostrarNotificacion(`Se eliminaron ${eliminados} favoritos`, 'success');
            }
          },
          error: (error) => {
            console.error('❌ Error limpiando favoritos:', error);
            this.mostrarNotificacion('Error al limpiar favoritos', 'error');
          }
        });
      });
    }
  }

  agregarAlCarrito(productoId: number, event: Event): void {
    event.stopPropagation();

    if (!this.isAuthenticated) {
      this.mostrarNotificacion('Debes iniciar sesión para agregar al carrito', 'warning');
      return;
    }

    // ✅ Animación del botón
    const boton = event.target as HTMLElement;
    boton.classList.add('animando');

    const csrfToken = this.getCsrfToken();
    let headers = new HttpHeaders();
    if (csrfToken) {
      headers = headers.set('X-XSRF-TOKEN', csrfToken);
    }
    headers = headers.set('Content-Type', 'application/x-www-form-urlencoded');

    const body = new URLSearchParams();
    body.set('productoId', productoId.toString());
    body.set('cantidad', '1');

    this.http.post(`${this.apiUrl}/api/carrito/agregar`,
      body.toString(),
      {
        headers: headers,
        withCredentials: true
      }
    ).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.mostrarCarritoFlotante(event);
          boton.classList.remove('animando');
          boton.classList.add('agregado');
          boton.innerHTML = '<i class="fas fa-check me-1"></i> ¡Agregado!';

          this.mostrarNotificacion(`✅ ${response.message || 'Producto agregado al carrito'}`, 'success');
          this.carritoService.refrescarTotal();
          this.carritoService.getTotal().subscribe(total => {
            this.totalCarrito = total;
          });

          setTimeout(() => {
            boton.classList.remove('agregado');
            boton.innerHTML = '<i class="fas fa-cart-plus me-1"></i> Agregar al carrito';
          }, 2000);
        }
      },
      error: (error) => {
        console.error('❌ Error agregar al carrito:', error);
        boton.classList.remove('animando');
        if (error.status === 400) {
          this.mostrarNotificacion('Error: Producto no disponible o datos incorrectos', 'error');
        } else if (error.status === 403) {
          this.mostrarNotificacion('Error de seguridad. Recarga la página.', 'error');
        } else {
          this.mostrarNotificacion('❌ Error al agregar al carrito', 'error');
        }
      }
    });
  }

  formatearPrecio(precio: number): string {
    return `S/. ${precio.toFixed(2)}`;
  }

  formatearFecha(fechaISO: string): string {
    try {
      const fecha = new Date(fechaISO);
      return fecha.toLocaleDateString('es-ES', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    } catch (error) {
      return 'Fecha inválida';
    }
  }

  mostrarNotificacion(mensaje: string, tipo: string = 'info'): void {
    const notificacion = document.createElement('div');
    notificacion.className = `notificacion-flotante notificacion-${tipo}`;

    const iconos: { [key: string]: string } = {
      success: '✅',
      error: '❌',
      warning: '⚠️',
      info: 'ℹ️'
    };
    const icono = iconos[tipo] || '';

    notificacion.innerHTML = `
      <div class="notificacion-contenido">
        <span><span class="notificacion-icono">${icono}</span>${mensaje}</span>
        <button class="notificacion-cerrar">&times;</button>
      </div>
    `;

    document.body.appendChild(notificacion);

    setTimeout(() => {
      notificacion.classList.add('notificacion-visible');
    }, 10);

    const btnCerrar = notificacion.querySelector('.notificacion-cerrar');
    btnCerrar?.addEventListener('click', () => {
      notificacion.classList.remove('notificacion-visible');
      notificacion.classList.add('notificacion-salida');
      setTimeout(() => notificacion.remove(), 400);
    });

    setTimeout(() => {
      if (notificacion.parentNode) {
        notificacion.classList.remove('notificacion-visible');
        notificacion.classList.add('notificacion-salida');
        setTimeout(() => notificacion.remove(), 400);
      }
    }, 4000);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
