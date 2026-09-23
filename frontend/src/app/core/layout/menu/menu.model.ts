import { PermissionCode, RoleCode } from '../../auth/interfaces/auth.model'

export interface MenuItem {
  label: string
  icon: string
  route: string
  /** Descripción corta (se usa en los accesos rápidos del inicio). */
  description?: string
  /** Visible si el usuario tiene alguno de estos roles. */
  roles?: RoleCode[]
  /** Visible si el usuario tiene todos estos permisos. */
  permissions?: PermissionCode[]
  /** Activo solo con coincidencia exacta de la ruta. */
  exact?: boolean
}

export interface MenuSection {
  label: string
  items: MenuItem[]
}
