// src/app/app.config.ts
import { ApplicationConfig, importProvidersFrom } from '@angular/core'; // ✅ AGREGAR importProvidersFrom
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpClientXsrfModule } from '@angular/common/http'; // ✅ AGREGAR
import { routes } from './app.routes';
import { AuthInterceptor } from './core/interceptors/auth-interceptor';
import { ErrorInterceptor } from './core/interceptors/error-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptorsFromDi()
    ),
    // ✅ AGREGAR CONFIGURACIÓN CSRF AUTOMÁTICA
    importProvidersFrom(
      HttpClientXsrfModule.withOptions({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN'
      })
    ),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    }
  ]
};
