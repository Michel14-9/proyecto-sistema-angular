// src/app/modules/publico/locales/locales.ts
import { Component, OnInit, ViewEncapsulation, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../core/services/carrito.service';

declare var google: any;

interface Local {
  id: number;
  nombre: string;
  direccion: string;
  referencia: string;
  telefono: string;
  whatsapp: string;
  horarioApertura: string;
  horarioCierre: string;
  latitud: number;
  longitud: number;
  imagen: string;
  capacidad: number;
  estacionamiento: boolean;
  delivery: boolean;
  recogeTienda: boolean;
  comerAqui: boolean;
  estado: string;
}

@Component({
  selector: 'app-locales',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './locales.html',
  styleUrls: ['./locales.css'],
  encapsulation: ViewEncapsulation.None
})
export class LocalesComponent implements OnInit, AfterViewInit {
  // Datos estáticos del local (como en tu sistema anterior)
  locales: Local[] = [
    {
      id: 1,
      nombre: 'Luren Chicken - La Mar',
      direccion: 'La Mar 1141, Ica, Perú',
      referencia: 'Cerca al parque principal',
      telefono: '+51 123-456-789',
      whatsapp: '51912345678',
      horarioApertura: '4:00 PM',
      horarioCierre: '10:00 PM',
      latitud: -14.070505106029492,
      longitud: -75.72412960638542,
      imagen: 'local-ica.png',
      capacidad: 50,
      estacionamiento: false,
      delivery: true,
      recogeTienda: true,
      comerAqui: true,
      estado: 'ABIERTO'
    }
  ];

  isLoading: boolean = false;
  errorMessage: string = '';
  localSeleccionado: Local | null = null;

  // Para el header
  isAuthenticated: boolean = false;
  username: string = '';
  totalCarrito: number = 0;

  // Google Maps
  private mapa: any;
  private marcador: any;
  private infoWindow: any;

  constructor(
    private authService: AuthService,
    private carritoService: CarritoService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.username = this.authService.getUsername();

    this.carritoService.getTotal().subscribe(total => {
      this.totalCarrito = total;
    });

    // Seleccionar el primer local
    if (this.locales.length > 0) {
      this.localSeleccionado = this.locales[0];
    }
  }

  ngAfterViewInit(): void {
    // Cargar el mapa después de que la vista esté lista
    setTimeout(() => {
      this.cargarMapa();
      this.actualizarEstadoLocal();
    }, 500);
  }

  cargarMapa(): void {
    if (!this.localSeleccionado) return;

    // Verificar que Google Maps esté cargado
    if (typeof google === 'undefined') {
      console.warn('Google Maps no está cargado, intentando cargar...');
      this.cargarScriptGoogleMaps();
      return;
    }

    this.inicializarMapa();
  }

  cargarScriptGoogleMaps(): void {
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=TU_API_KEY&callback=initMap`;
    script.async = true;
    script.defer = true;
    document.head.appendChild(script);

    // @ts-ignore
    window.initMap = () => {
      this.inicializarMapa();
    };
  }

  inicializarMapa(): void {
    if (!this.localSeleccionado) return;

    const local = this.localSeleccionado;
    const position = { lat: local.latitud, lng: local.longitud };

    // Crear el mapa
    this.mapa = new google.maps.Map(document.getElementById('mapa'), {
      zoom: 18,
      center: position,
      mapTypeControl: false,
      streetViewControl: true,
      fullscreenControl: true,
      styles: [
        {
          featureType: 'poi',
          elementType: 'labels',
          stylers: [{ visibility: 'off' }]
        }
      ]
    });

    // Crear el marcador
    this.marcador = new google.maps.Marker({
      position: position,
      map: this.mapa,
      title: local.nombre,
      animation: google.maps.Animation.DROP,
      icon: {
        url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
          <svg width="40" height="40" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg">
            <circle cx="20" cy="20" r="18" fill="#ff6b00" stroke="#ffffff" stroke-width="2"/>
            <text x="20" y="26" text-anchor="middle" fill="white" font-family="Arial" font-size="14" font-weight="bold">L</text>
          </svg>
        `),
        scaledSize: new google.maps.Size(40, 40)
      }
    });

    // Crear InfoWindow
    this.infoWindow = new google.maps.InfoWindow({
      content: `
        <div style="padding: 1rem; max-width: 250px;">
          <h5 style="margin: 0 0 0.5rem 0; color: #ff6b00; font-weight: bold;">${local.nombre}</h5>
          <p style="margin: 0 0 0.5rem 0; color: #333; font-weight: 500;">${local.direccion}</p>
          <p style="margin: 0 0 0.5rem 0; color: #666; font-size: 0.9rem;">Ica, Perú</p>
          <p style="margin: 0 0 0.5rem 0; color: #666; font-size: 0.9rem;">
            <i class="fas fa-clock" style="margin-right: 0.5rem;"></i>${this.getHorario(local)}
          </p>
          <hr style="margin: 0.5rem 0;">
          <a href="https://www.google.com/maps/dir/?api=1&destination=${local.latitud},${local.longitud}"
             target="_blank"
             style="color: #ff6b00; text-decoration: none; font-size: 0.9rem; font-weight: 500;">
             <i class="fas fa-directions" style="margin-right: 0.5rem;"></i>Cómo llegar
          </a>
        </div>
      `
    });

    // Evento click del marcador
    this.marcador.addListener('click', () => {
      this.infoWindow.open(this.mapa, this.marcador);
    });

    // Abrir info window automáticamente después de 1 segundo
    setTimeout(() => {
      this.infoWindow.open(this.mapa, this.marcador);
    }, 1000);

    console.log(' Mapa inicializado correctamente');
  }

  actualizarEstadoLocal(): void {
    const ahora = new Date();
    const horaActual = ahora.getHours();
    const minutosActual = ahora.getMinutes();
    const horaDecimal = horaActual + (minutosActual / 60);

    // El local abre a las 4:00 PM (16:00) y cierra a las 10:00 PM (22:00)
    const estaAbierto = horaDecimal >= 16 && horaDecimal < 22;

    const estadoElement = document.getElementById('estadoLocal');
    if (estadoElement) {
      estadoElement.textContent = estaAbierto ? ' Abierto' : ' Cerrado';
      estadoElement.className = `badge ${estaAbierto ? 'bg-success' : 'bg-danger'}`;
    }
  }

  seleccionarLocal(local: Local): void {
    this.localSeleccionado = local;
    // Reinicializar el mapa
    setTimeout(() => {
      this.cargarMapa();
      this.actualizarEstadoLocal();
    }, 300);
  }

  getHorario(local: Local): string {
    if (local.horarioApertura && local.horarioCierre) {
      return `${local.horarioApertura} - ${local.horarioCierre}`;
    }
    return 'Horario no disponible';
  }

  getEstadoLocal(local: Local): { texto: string; clase: string } {
    const ahora = new Date();
    const horaActual = ahora.getHours();
    const estaAbierto = horaActual >= 16 && horaActual < 22;

    if (estaAbierto) {
      return { texto: ' Abierto', clase: 'bg-success' };
    } else {
      return { texto: ' Cerrado', clase: 'bg-danger' };
    }
  }

  getServicios(local: Local): string[] {
    const servicios: string[] = [];
    if (local.comerAqui) servicios.push('Comer aquí');
    if (local.recogeTienda) servicios.push('Recoger en tienda');
    if (local.delivery) servicios.push('Delivery');
    return servicios;
  }

  abrirWhatsapp(telefono: string): void {
    const numero = telefono.replace(/\s/g, '');
    window.open(`https://wa.me/${numero}`, '_blank');
  }

  llamarTelefono(telefono: string): void {
    // Verificar si el local está abierto
    const ahora = new Date();
    const horaActual = ahora.getHours();
    const estaAbierto = horaActual >= 16 && horaActual < 22;

    if (!estaAbierto) {
      if (confirm('El local está cerrado en este momento (horario: 4:00 PM - 10:00 PM). ¿Deseas llamar de todos modos?')) {
        window.location.href = `tel:${telefono}`;
      }
    } else {
      window.location.href = `tel:${telefono}`;
    }
  }

  abrirMapa(local: Local): void {
    if (local.latitud && local.longitud) {
      window.open(`https://www.google.com/maps/dir/?api=1&destination=${local.latitud},${local.longitud}`, '_blank');
    } else if (local.direccion) {
      window.open(`https://www.google.com/maps?q=${encodeURIComponent(local.direccion)}`, '_blank');
    }
  }

  logout(): void {
    this.authService.logout();
  }

  // Actualizar estado cada minuto
  iniciarActualizacionEstado(): void {
    setInterval(() => {
      this.actualizarEstadoLocal();
    }, 60000);
  }
}
