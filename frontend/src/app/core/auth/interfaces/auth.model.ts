export type RoleCode = 'USER' | 'ADMIN'

export type PermissionCode =
  | 'CREDIT_REQUEST'
  | 'CREDIT_VIEW_OWN'
  | 'CREDIT_CANCEL_OWN'
  | 'CREDIT_SIMULATE'
  | 'CREDIT_VIEW_ALL'
  | 'CREDIT_APPROVE'
  | 'CREDIT_REJECT'
  | 'USER_MANAGE'
  | 'RATE_MANAGE'

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthUser {
  id: number
  fullName: string
  email: string
  role: RoleCode
  roleName: string
  permissions: PermissionCode[]
}

export interface AuthResponse {
  token: string
  tokenType: 'Bearer'
  expiresAt: string
  user: AuthUser
}
