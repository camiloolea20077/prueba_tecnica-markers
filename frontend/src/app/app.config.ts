import {
  ApplicationConfig,
  DEFAULT_CURRENCY_CODE,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core'
import { registerLocaleData } from '@angular/common'
import localeEsCo from '@angular/common/locales/es-CO'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { provideHttpClient, withInterceptors } from '@angular/common/http'
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async'
import { providePrimeNG } from 'primeng/config'
import { ConfirmationService, MessageService } from 'primeng/api'

import { routes } from './app.routes'
import { authInterceptor } from './core/auth/interceptors/auth.interceptor'
import { errorInterceptor } from './core/auth/interceptors/error.interceptor'
import { AppPreset } from './core/theme/app-preset'

registerLocaleData(localeEsCo)

export const appConfig: ApplicationConfig = {
  providers: [
    // Pesos colombianos: 15.000.000 / tasas 22,5 %
    { provide: LOCALE_ID, useValue: 'es-CO' },
    { provide: DEFAULT_CURRENCY_CODE, useValue: 'COP' },
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    provideAnimationsAsync(),
    MessageService,
    ConfirmationService,
    providePrimeNG({
      theme: {
        preset: AppPreset,
        options: { darkModeSelector: '.app-dark' },
      },
      ripple: true,
    }),
  ],
}
