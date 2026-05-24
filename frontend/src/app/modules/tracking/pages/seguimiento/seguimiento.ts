import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { TrackingService } from '../../services/tracking.service';
import { TrackingResponse } from '../../models/tracking.models';

declare var google: any;

@Component({
  selector: 'app-seguimiento',
  standalone: false,
  templateUrl: './seguimiento.html',
  styleUrls: ['./seguimiento.css']
})
export class SeguimientoComponent implements OnInit, OnDestroy, AfterViewInit {

  numeroPedido = '';
  tracking: TrackingResponse | null = null;
  error = '';
  cargando = true;

  private map: any;
  private markerRepartidor: any;
  private markerCliente: any;
  private markerLocal: any;
  private directionsRenderer: any;
  private directionsService: any;
  private pollingSubscription?: Subscription;

  readonly GOOGLE_MAPS_KEY = 'TU_API_KEY_PUBLICA_AQUI'; // misma key que en properties

  constructor(
    private route: ActivatedRoute,
    private trackingService: TrackingService
  ) {}

  ngOnInit(): void {
    this.numeroPedido = this.route.snapshot.paramMap.get('numeroPedido') || '';
    this.cargarGoogleMaps();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
  }

  private cargarGoogleMaps(): void {
    if (typeof google !== 'undefined') {
      this.iniciarTracking();
      return;
    }
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${this.GOOGLE_MAPS_KEY}&libraries=geometry`;
    script.async = true;
    script.defer = true;
    script.onload = () => this.iniciarTracking();
    document.head.appendChild(script);
  }

  private iniciarTracking(): void {
    this.trackingService.obtenerTracking(this.numeroPedido).subscribe({
      next: (data) => {
        if (!data.id) {
          this.error = data.mensaje || 'No se encontró tracking para este pedido.';
          this.cargando = false;
          return;
        }
        this.tracking = data;
        this.cargando = false;
        this.inicializarMapa();

        // Solo hacer polling si el delivery está activo
        if (data.estado === 'ACTIVE' || data.estado === 'READY') {
          this.iniciarPolling();
        }
      },
      error: () => {
        this.error = 'Error al conectar con el servidor.';
        this.cargando = false;
      }
    });
  }

  private inicializarMapa(): void {
    if (!this.tracking) return;

    const centro = {
      lat: this.tracking.latLocal || -14.0678,
      lng: this.tracking.lngLocal || -75.7356
    };

    this.map = new google.maps.Map(document.getElementById('mapa-tracking'), {
      zoom: 14,
      center: centro,
      mapTypeId: 'roadmap',
      disableDefaultUI: false,
      zoomControl: true
    });

    this.directionsService = new google.maps.DirectionsService();
    this.directionsRenderer = new google.maps.DirectionsRenderer({
      suppressMarkers: true,
      polylineOptions: { strokeColor: '#E63946', strokeWeight: 4 }
    });
    this.directionsRenderer.setMap(this.map);

    // Marcador: Local (origen)
    this.markerLocal = new google.maps.Marker({
      position: { lat: this.tracking.latLocal, lng: this.tracking.lngLocal },
      map: this.map,
      title: 'Pollería Apollo',
      icon: {
        url: 'https://maps.google.com/mapfiles/ms/icons/red-dot.png',
        scaledSize: new google.maps.Size(40, 40)
      },
      label: { text: '🍗', fontSize: '22px' }
    });

    // Marcador: Cliente (destino)
    if (this.tracking.latCliente) {
      this.markerCliente = new google.maps.Marker({
        position: { lat: this.tracking.latCliente, lng: this.tracking.lngCliente },
        map: this.map,
        title: 'Tu dirección',
        icon: {
          url: 'https://maps.google.com/mapfiles/ms/icons/blue-dot.png',
          scaledSize: new google.maps.Size(40, 40)
        }
      });
    }

    // Marcador: Repartidor (móvil)
    if (this.tracking.latRepartidor) {
      this.markerRepartidor = new google.maps.Marker({
        position: { lat: this.tracking.latRepartidor, lng: this.tracking.lngRepartidor },
        map: this.map,
        title: this.tracking.nombreRepartidor || 'Repartidor',
        icon: {
          url: 'https://maps.google.com/mapfiles/ms/icons/motorcycling.png',
          scaledSize: new google.maps.Size(40, 40)
        }
      });
      this.dibujarRuta();
    }
  }

  private dibujarRuta(): void {
    if (!this.tracking?.latRepartidor || !this.tracking?.latCliente) return;

    const request = {
      origin: { lat: this.tracking.latRepartidor, lng: this.tracking.lngRepartidor },
      destination: { lat: this.tracking.latCliente, lng: this.tracking.lngCliente },
      travelMode: google.maps.TravelMode.DRIVING
    };

    this.directionsService.route(request, (result: any, status: any) => {
      if (status === 'OK') {
        this.directionsRenderer.setDirections(result);
      }
    });
  }

  private iniciarPolling(): void {
    this.pollingSubscription = this.trackingService
      .trackingEnVivo(this.numeroPedido, 6000)
      .subscribe({
        next: (data) => {
          this.tracking = data;
          this.actualizarMarcadorRepartidor(data);
        },
        error: () => { /* silencioso, seguirá intentando */ }
      });
  }

  private actualizarMarcadorRepartidor(data: TrackingResponse): void {
    if (!data.latRepartidor || !this.map) return;

    const nuevaPos = { lat: data.latRepartidor, lng: data.lngRepartidor };

    if (this.markerRepartidor) {
      this.markerRepartidor.setPosition(nuevaPos);
    } else {
      this.markerRepartidor = new google.maps.Marker({
        position: nuevaPos,
        map: this.map,
        title: data.nombreRepartidor || 'Repartidor',
        icon: {
          url: 'https://maps.google.com/mapfiles/ms/icons/motorcycling.png',
          scaledSize: new google.maps.Size(40, 40)
        }
      });
    }

    this.dibujarRuta();

    if (data.estado === 'COMPLETED') {
      this.pollingSubscription?.unsubscribe();
    }
  }

  get etaTexto(): string {
    if (!this.tracking?.etaMinutos) return 'Calculando...';
    if (this.tracking.etaMinutos <= 1) return 'Llegando ahora';
    return `~${this.tracking.etaMinutos} min`;
  }

  get estadoTexto(): string {
    const map: Record<string, string> = {
      READY: '🟡 Preparando salida',
      ACTIVE: '🛵 En camino',
      COMPLETED: '✅ Entregado',
      CANCELLED: '❌ Cancelado'
    };
    return map[this.tracking?.estado || ''] || 'Desconocido';
  }
}