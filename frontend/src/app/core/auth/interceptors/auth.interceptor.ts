import { HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core'
import { environment } from '../../../../environments/environment'
import { AuthStore } from '../store/auth.store'

/**
 * Adjunta el token Bearer solo a las peticiones dirigidas a nuestra API.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthStore).token()
  if (!token || !req.url.startsWith(environment.apiUrl)) {
    return next(req)
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }))
}
