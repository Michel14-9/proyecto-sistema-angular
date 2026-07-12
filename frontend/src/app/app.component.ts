import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './core/components/header/header.component';
import { FooterComponent } from './core/components/footer/footer.component';
import { LayoutService } from './core/services/layout.service';
import { CommonModule } from '@angular/common';
import { AlertComponent } from './shared/alert/alert.component';
import { ChatbotComponent } from './shared/components/chatbot/chatbot.component';
import { ConfigService } from './core/services/config.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    HeaderComponent,
    FooterComponent,
    CommonModule,
    AlertComponent,
    ChatbotComponent  // ✅ Ya debe funcionar si ChatbotComponent es standalone
  ],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class AppComponent implements OnInit {
  title = 'frontend';

  constructor(
    public layoutService: LayoutService,
    private configService: ConfigService
  ) {}

  ngOnInit(): void {
    this.configService.loadConfig().then(() => {
      console.log(' Configuración cargada en AppComponent');
    }).catch((error) => {
      console.warn(' Error al cargar configuración:', error);
    });
  }
}
