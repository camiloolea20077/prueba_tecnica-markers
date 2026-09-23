import { PermissionCode, RoleCode } from '../../../../core/auth/interfaces/auth.model'

export interface AdminUser {
  id: number
  fullName: string
  email: string
  role: RoleCode
  roleName: string
  active: boolean
  createdAt: string
}

export interface RoleOption {
  code: RoleCode
  name: string
  permissions: PermissionCode[]
}

export interface UserQuery {
  q: string
  role: RoleCode | null
  active: boolean | null
  page: number
  size: number
}

export interface CreateUserRequest {
  fullName: string
  email: string
  password: string
  role: RoleCode
  active: boolean
}

/** `password` null = conservar la actual. */
export interface UpdateUserRequest {
  fullName: string
  email: string
  role: RoleCode
  active: boolean
  password: string | null
}
