import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';

declare var bootstrap: any;

@Component({
  selector: 'app-index',
  templateUrl: './index.html',
  styleUrls: ['./index.css']
})
export class Index implements OnInit, AfterViewInit, OnDestroy {
  private carouselInstance: any;
  private refreshInterval: any;

  ngOnInit(): void {
    this.cargarCombosEnCarrusel();
    this.refreshInterval = setInterval(() => {
      this.cargarCombosEnCarrusel();
    }, 300000);
  }

  ngAfterViewInit(): void {
    this.configurarCarruselAutoplay();
    this.configurarScrollSuave();
    this.configurarAnimacionesScroll();
    this.actualizarContadorCarrito();
    this.configurarHoverCategorias();
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
    if (this.carouselInstance) this.carouselInstance.dispose();
  }

  async cargarCombosEnCarrusel() {
    try {
      const response = await fetch('/api/combos');
      if (!response.ok) throw new Error(`Error HTTP: ${response.status}`);
      const combos = await response.json();
      if (combos.length > 0) {
        this.mostrarCombosEnCarrusel(combos);
      } else {
        this.mostrarCarruselFallback();
      }
    } catch (error) {
      console.error('Error:', error);
      this.mostrarCarruselFallback();
    }
  }

  mostrarCombosEnCarrusel(combos: any[]) {
    const carouselInner = document.getElementById('combos-inner');
    const carouselIndicators = document.getElementById('combos-indicators');
    if (!carouselInner || !carouselIndicators) return;

    carouselInner.innerHTML = '';
    carouselIndicators.innerHTML = '';

    const comboGroups = [];
    for (let i = 0; i < combos.length; i += 3) {
      comboGroups.push(combos.slice(i, i + 3));
    }

    comboGroups.forEach((_, index) => {
      const indicator = document.createElement('button');
      indicator.type = 'button';
      indicator.setAttribute('data-bs-target', '#combosCarousel');
      indicator.setAttribute('data-bs-slide-to', index.toString());
      if (index === 0) indicator.classList.add('active');
      carouselIndicators.appendChild(indicator);
    });

    comboGroups.forEach((group, groupIndex) => {
      const slide = document.createElement('div');
      slide.className = `carousel-item ${groupIndex === 0 ? 'active' : ''}`;
      let combosHTML = '<div class="row justify-content-center">';
      
      group.forEach((combo: any) => {
        combosHTML += `
          <div class="col-md-4 mb-4">
            <div class="combo-card h-100">
              <div class="position-relative">
                <img src="${combo.imagenUrl || '/imagenes/default-product.jpg'}" 
                     alt="${combo.nombre}" 
                     class="combo-image-carousel img-fluid">
                <div class="combo-badge">COMBO</div>
              </div>
              <div class="combo-content p-3">
                <h4 class="combo-title">${combo.nombre}</h4>
                <p class="combo-description">${combo.descripcion || ''}</p>
                <div class="price-section mb-3">
                  <span class="current-price text-success fw-bold">S/ ${combo.precio.toFixed(2)}</span>
                </div>
                <button class="btn btn-success w-100" (click)="agregarAlCarrito(${combo.id})">
                  <i class="fas fa-cart-plus me-2"></i>AGREGAR AL PEDIDO
                </button>
              </div>
            </div>
          </div>
        `;
      });
      
      combosHTML += '</div>';
      slide.innerHTML = combosHTML;
      carouselInner.appendChild(slide);
    });
  }

  mostrarCarruselFallback() {
    const carouselInner = document.getElementById('combos-inner');
    if (!carouselInner) return;
    carouselInner.innerHTML = `
      <div class="carousel-item active">
        <div class="row">
          <div class="col-12 text-center py-5">
            <div class="alert alert-info border-0">
              <h4>Próximamente Nuevos Combos</h4>
              <a href="/menu" class="btn btn-primary">Ver Menú Completo</a>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  configurarCarruselAutoplay() {
    const carousel = document.getElementById('combosCarousel');
    if (carousel && typeof bootstrap !== 'undefined') {
      this.carouselInstance = new bootstrap.Carousel(carousel, {
        interval: 5000, wrap: true, pause: 'hover'
      });
    }
  }

  async agregarAlCarrito(productoId: number) {
    try {
      const csrfToken = (document.getElementById('csrfToken') as HTMLInputElement)?.value;
      const response = await fetch('/carrito/agregar-ajax', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          'productoId': productoId.toString(),
          'cantidad': '1',
          '_csrf': csrfToken || ''
        })
      });
      const data = await response.json();
      if (response.ok && data.success) {
        this.mostrarNotificacion(data.message, 'success');
        this.actualizarContadorCarrito();
      }
    } catch (error) {
      this.mostrarNotificacion('Error al agregar', 'error');
    }
  }

  async actualizarContadorCarrito() {
    try {
      const response = await fetch('/carrito/total');
      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          const totalSpan = document.querySelector('.carrito-btn span');
          if (totalSpan) totalSpan.textContent = data.total.toFixed(2);
        }
      }
    } catch (error) {}
  }

  mostrarNotificacion(mensaje: string, tipo: string) {
    const notif = document.createElement('div');
    notif.className = `notificacion-index notificacion-flotante-index notificacion-${tipo}`;
    notif.innerHTML = `<div>${mensaje}</div>`;
    document.body.appendChild(notif);
    setTimeout(() => notif.remove(), 5000);
  }

  configurarScrollSuave() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
      anchor.addEventListener('click', (e) => {
        e.preventDefault();
        const target = document.querySelector(anchor.getAttribute('href')!);
        if (target) target.scrollIntoView({ behavior: 'smooth' });
      });
    });
  }

  configurarAnimacionesScroll() {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('animate__animated', 'animate__fadeInUp');
        }
      });
    }, { threshold: 0.1 });
    document.querySelectorAll('.category-card, .combo-card, .feature-icon').forEach(el => {
      observer.observe(el);
    });
  }

  configurarHoverCategorias() {
    document.querySelectorAll('.category-card').forEach(card => {
      card.addEventListener('mouseenter', () => card.classList.add('category-card-hover'));
      card.addEventListener('mouseleave', () => card.classList.remove('category-card-hover'));
    });
  }
}