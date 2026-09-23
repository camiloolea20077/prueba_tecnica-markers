import { PermissionCode } from '../interfaces/auth.model'

/** Descripción legible de cada permiso (coincide con la tabla `permissions`). */
export const PERMISSION_LABELS: Record<PermissionCode, string> = {
  CREDIT_REQUEST: 'Solicitar créditos',
  CREDIT_VIEW_OWN: 'Consultar sus propios créditos',
  CREDIT_CANCEL_OWN: 'Cancelar sus solicitudes pendientes',
  CREDIT_SIMULATE: 'Simular cuota de un crédito',
  CREDIT_VIEW_ALL: 'Consultar todos los créditos',
  CREDIT_APPROVE: 'Aprobar créditos',
  CREDIT_REJECT: 'Rechazar créditos',
  USER_MANAGE: 'Administrar usuarios',
  RATE_MANAGE: 'Administrar tramos de tasa de interés',
}
