import { ApplicationConfig, provideZoneChangeDetection, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { en_US, provideNzI18n } from 'ng-zorro-antd/i18n';
import { registerLocaleData } from '@angular/common';
import en from '@angular/common/locales/en';
import { FormsModule } from '@angular/forms';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

// --- FIX: Import withInterceptors and your interceptor ---
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';

// Import the icon module and the specific icons you need
import { NzIconModule } from 'ng-zorro-antd/icon';
import { MenuOutline, SunOutline, MoonOutline, UserOutline } from '@ant-design/icons-angular/icons';

registerLocaleData(en);

// Create a list of the icons to make them available in your app
const icons = [MenuOutline, SunOutline, MoonOutline, UserOutline];

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideClientHydration(),
    provideNzI18n(en_US),
    importProvidersFrom(FormsModule),
    provideAnimationsAsync(),

    // --- FIX: Register the interceptor here ---
    provideHttpClient(
      withInterceptors([jwtInterceptor])
    ),

    // Add this line to provide the icons to your entire application
    importProvidersFrom(NzIconModule.forRoot(icons))
  ]
};