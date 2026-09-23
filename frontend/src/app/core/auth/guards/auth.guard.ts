import { inject } from '@angular/core'
import { CanActivateFn, Router } from '@angular/router'
import { AuthStore } from '../store/auth.store'

/**
 * Solo usuarios con sesión vigente. Si no, redirige al login conservando la URL solicitada.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const store = inject(AuthStore)
  const router = inject(Router)

  if (store.isSessionValid()) {
    return true
  }
  store.clear()
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })
}
