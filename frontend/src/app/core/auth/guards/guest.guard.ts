import { inject } from '@angular/core'
import { CanActivateFn, Router } from '@angular/router'
import { AuthService } from '../services/auth.service'
import { AuthStore } from '../store/auth.store'

/**
 * Solo visitantes sin sesión (p. ej. el login). Si ya hay sesión, envía al inicio.
 */
export const guestGuard: CanActivateFn = () => {
  const store = inject(AuthStore)
  if (!store.isSessionValid()) {
    return true
  }
  return inject(Router).parseUrl(inject(AuthService).homeUrl())
}
