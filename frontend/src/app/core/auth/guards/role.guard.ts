import { inject } from '@angular/core'
import { CanActivateFn, Router } from '@angular/router'
import { MessageService } from 'primeng/api'
import { PermissionCode, RoleCode } from '../interfaces/auth.model'
import { AuthService } from '../services/auth.service'
import { AuthStore } from '../store/auth.store'

/**
 * Restringe una ruta por rol y/o permisos declarados en `data`.
 *
 * @example
 * { path: 'admin', canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } }
 * { path: 'aprobar', canActivate: [authGuard, roleGuard], data: { permissions: ['CREDIT_APPROVE'] } }
 */
export const roleGuard: CanActivateFn = (route) => {
  const store = inject(AuthStore)
  const roles = (route.data['roles'] as RoleCode[] | undefined) ?? []
  const permissions = (route.data['permissions'] as PermissionCode[] | undefined) ?? []

  const roleOk = roles.length === 0 || store.hasRole(...roles)
  const permissionsOk = permissions.length === 0 || store.hasPermission(...permissions)
  if (roleOk && permissionsOk) {
    return true
  }

  inject(MessageService).add({
    severity: 'warn',
    summary: 'Acceso denegado',
    detail: 'No tiene permisos para acceder a esa sección.',
  })
  return inject(Router).parseUrl(inject(AuthService).homeUrl())
}
