import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core'
import { Router } from '@angular/router'
import { MessageService } from 'primeng/api'
import { catchError, throwError } from 'rxjs'
import { AuthService } from '../services/auth.service'
import { AuthStore } from '../store/auth.store'

/**
 * Manejo transversal de errores HTTP.
 * <ul>
 *   <li>0 → servidor inaccesible.</li>
 *   <li>401 (fuera del login) → sesión expirada: cierra sesión y vuelve al login.</li>
 *   <li>403 → aviso de permisos.</li>
 * </ul>
 * Los demás errores (400, 404, 409, 422) los muestra cada pantalla.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(MessageService)
  const auth = inject(AuthService)
  const store = inject(AuthStore)
  const router = inject(Router)

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isLogin = req.url.endsWith('/auth/login')

      if (error.status === 0) {
        toast.add({
          severity: 'error',
          summary: 'Sin conexión',
          detail: 'No fue posible conectar con el servidor. Intente de nuevo.',
        })
      } else if (error.status === 401 && !isLogin && store.isAuthenticated()) {
        toast.add({
          severity: 'warn',
          summary: 'Sesión finalizada',
          detail: 'Su sesión expiró, inicie sesión nuevamente.',
        })
        auth.logout(router.url)
      } else if (error.status === 403 && !isLogin) {
        toast.add({
          severity: 'warn',
          summary: 'Acceso denegado',
          detail: error.error?.message ?? 'No tiene permisos para realizar esta acción.',
        })
      }
      return throwError(() => error)
    }),
  )
}
