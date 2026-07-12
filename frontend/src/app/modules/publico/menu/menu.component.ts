import { Component, OnInit, AfterViewInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';
import { AlertService } from '../../../core/services/alert.service';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class MenuComponent implements OnInit, AfterViewInit {
  private apiUrl = 'http://localhost:8080';
  menuData: any = {};
  categorias: string[] = ['pollos', 'parrillas', 'chicharron', 'broaster', 'hamburguesas', 'criollos', 'combos'];
  isLoading: boolean = true;
  errorMessage: string = '';
  totalCarrito: number = 0;
  favoritos: Set<number> = new Set();

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private carritoService: CarritoService,
    private router: Router,
    private route: ActivatedRoute,
    private alertService: AlertService // ✅ INYECTADO
  ) {}

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

  ngOnInit(): void {
    this.cargarMenu();
    this.cargarFavoritos();

    this.carritoService.getTotal().subscribe(total => {
      this.totalCarrito = total;
    });

    this.route.fragment.subscribe(fragment => {
      if (fragment) {
        console.log('🔗 Fragmento recibido:', fragment);
        setTimeout(() => {
          this.scrollToCategory(fragment);
        }, 500);
      }
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.configurarAnimaciones();

      const fragment = this.route.snapshot.fragment;
      if (fragment) {
        console.log('🔗 Fragmento inicial:', fragment);
        setTimeout(() => {
          this.scrollToCategory(fragment);
        }, 800);
      }
    }, 500);
  }

  cargarMenu(): void {
    this.isLoading = true;
    this.errorMessage = '';

    console.log('🔄 Cargando menú...');

    this.http.get(`${this.apiUrl}/api/menu/completo`).subscribe({
      next: (response: any) => {
        console.log('📦 Respuesta del servidor:', response);

        if (response && response.success === true) {
          if (response.menu) {
            this.menuData = response.menu;
          } else if (response.data) {
            this.menuData = response.data;
          } else {
            this.menuData = response;
          }
          console.log('✅ Menú cargado correctamente');
          this.isLoading = false;

          setTimeout(() => {
            const fragment = this.route.snapshot.fragment;
            if (fragment) {
              console.log('🔗 Fragmento después de cargar menú:', fragment);
              this.scrollToCategory(fragment);
            }
          }, 300);

          return;
        }

        if (response && typeof response === 'object' && !response.success) {
          const categorias = Object.keys(response);
          if (categorias.length > 0 && categorias.some(c => Array.isArray(response[c]))) {
            this.menuData = response;
            console.log('✅ Menú cargado (directo)');
            this.isLoading = false;
            return;
          }
        }

        this.errorMessage = response?.message || 'Error al cargar el menú';
        console.warn('⚠️ Respuesta inesperada:', response);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error al cargar menú:', error);

        if (error.error && typeof error.error === 'object') {
          if (error.error.menu) {
            this.menuData = error.error.menu;
            this.isLoading = false;
            return;
          }
          if (error.error.data) {
            this.menuData = error.error.data;
            this.isLoading = false;
            return;
          }
          this.errorMessage = error.error.message || 'Error al cargar el menú. Intenta nuevamente.';
        } else {
          this.errorMessage = 'Error al cargar el menú. Intenta nuevamente.';
        }
        this.isLoading = false;
      }
    });
  }

  cargarFavoritos(): void {
    if (!this.authService.isAuthenticated()) return;

    this.http.get(`${this.apiUrl}/api/favoritos/listar`, {
      withCredentials: true
    }).subscribe({
      next: (response: any) => {
        if (response.success && response.favoritos) {
          response.favoritos.forEach((fav: any) => {
            if (fav.producto && fav.producto.id) {
              this.favoritos.add(fav.producto.id);
            }
          });
          console.log('❤️ Favoritos cargados:', this.favoritos);
        }
      },
      error: (error) => {
        console.error('Error cargando favoritos:', error);
        // ✅ Usar AlertService
        this.alertService.error('Error al cargar favoritos');
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

  toggleFavorito(productId: number | undefined, event: Event): void {
    event.stopPropagation();

    if (!productId) {
      console.error('❌ Error: productId es undefined');
      // ✅ Usar AlertService
      this.alertService.error('Error: Producto no válido');
      return;
    }

    if (!this.authService.isAuthenticated()) {
      // ✅ Usar AlertService
      this.alertService.warning('Debes iniciar sesión para agregar favoritos');
      return;
    }

    console.log('🔄 Toggle favorito - Producto ID:', productId);

    const boton = event.target as HTMLElement;
    boton.classList.add('animando');

    const csrfToken = this.getCsrfToken();
    let headers = new HttpHeaders();
    if (csrfToken) {
      headers = headers.set('X-XSRF-TOKEN', csrfToken);
    }
    headers = headers.set('Content-Type', 'application/x-www-form-urlencoded');

    const body = new URLSearchParams();
    body.set('productoId', productId.toString());

    this.http.post(`${this.apiUrl}/api/favoritos/toggle`,
      body.toString(),
      {
        headers: headers,
        withCredentials: true
      }
    ).subscribe({
      next: (response: any) => {
        if (response.success) {
          if (response.agregado) {
            this.favoritos.add(productId);
            this.mostrarCorazonFlotante(event);
            // ✅ Usar AlertService
            this.alertService.success('❤️ Agregado a favoritos');
          } else {
            this.favoritos.delete(productId);
            // ✅ Usar AlertService
            this.alertService.info('💔 Eliminado de favoritos');
          }
          boton.classList.remove('animando');
        }
      },
      error: (error) => {
        console.error('Error toggle favorito:', error);
        boton.classList.remove('animando');
        // ✅ Usar AlertService
        if (error.status === 403) {
          this.alertService.error('Error de seguridad. Recarga la página.');
        } else if (error.status === 400) {
          this.alertService.error('Error: Producto no válido');
        } else {
          this.alertService.error('Error al actualizar favoritos');
        }
      }
    });
  }

  esFavorito(productId: number | undefined): boolean {
    if (!productId) return false;
    return this.favoritos.has(productId);
  }

  agregarAlCarrito(productoId: number | undefined, event: Event): void {
    event.stopPropagation();

    if (!productoId) {
      console.error('❌ Error: productoId es undefined');
      // ✅ Usar AlertService
      this.alertService.error('Error: Producto no válido');
      return;
    }

    console.log('🛒 Agregando al carrito - Producto ID:', productoId);

    const boton = event.target as HTMLElement;
    const textoOriginal = boton.innerHTML;
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

          // ✅ Usar AlertService
          this.alertService.success(`✅ ${response.message || 'Producto agregado'}`);

          this.carritoService.refrescarTotal();
          this.carritoService.getTotal().subscribe(total => {
            this.totalCarrito = total;
          });

          setTimeout(() => {
            boton.classList.remove('agregado');
            boton.innerHTML = textoOriginal;
          }, 2000);
        }
      },
      error: (error) => {
        console.error('Error agregar al carrito:', error);
        boton.classList.remove('animando');
        boton.innerHTML = textoOriginal;
        // ✅ Usar AlertService
        if (error.status === 400) {
          this.alertService.error('Error: Producto no disponible o datos incorrectos');
        } else if (error.status === 403) {
          this.alertService.error('Error de seguridad. Recarga la página.');
        } else {
          this.alertService.error('❌ Error al agregar al carrito');
        }
      }
    });
  }

  // ❌ ELIMINAR este método (ya no es necesario)
  // mostrarNotificacion(mensaje: string, tipo: string = 'info'): void {
  //   ... ya no se usa
  // }

  configurarAnimaciones(): void {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('animacion-visible');
        }
      });
    }, { threshold: 0.1 });

    document.querySelectorAll('.product-card, .category-title').forEach(el => {
      el.classList.add('animacion-oculta');
      observer.observe(el);
    });
  }

  scrollToCategory(categoria: string): void {
    console.log('📜 Scrolleando a:', categoria);
    const element = document.getElementById(categoria);
    if (element) {
      console.log('✅ Elemento encontrado:', categoria);
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      console.warn('⚠️ Elemento no encontrado:', categoria);
      setTimeout(() => {
        const retryElement = document.getElementById(categoria);
        if (retryElement) {
          console.log('✅ Elemento encontrado en reintento:', categoria);
          retryElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }, 500);
    }
  }

  getProductos(categoria: string): any[] {
    return this.menuData[categoria] || [];
  }
}
