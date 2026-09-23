import { computed, Injectable, signal } from '@angular/core'
import { AuthResponse, AuthUser, PermissionCode, RoleCode } from '../interfaces/auth.model'

const SESSION_KEY = 'dc_session'
const REMEMBER_EMAIL_KEY = 'dc_remember_email'

interface PersistedSession {
  token: string
  expiresAt: string
  user: AuthUser
}

/**
 * Estado de autenticación con signals.
 * <p>
 * "Recordar" → la sesión se guarda en localStorage (sobrevive al cerrar el navegador);
 * si no, en sessionStorage (se pierde al cerrar la pestaña). Nunca se guarda la contraseña.
 * </p>
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  // Estado privado
  private readonly _token = signal<string | null>(null)
  private readonly _expiresAt = signal<number | null>(null)
  private readonly _user = signal<AuthUser | null>(null)

  // Estado público
  readonly token = this._token.asReadonly()
  readonly user = this._user.asReadonly()
  readonly isAuthenticated = computed(() => !!this._token() && !!this._user())
  readonly role = computed<RoleCode | null>(() => this._user()?.role ?? null)
  readonly isAdmin = computed(() => this.role() === 'ADMIN')
  readonly permissions = computed(() => new Set(this._user()?.permissions ?? []))

  constructor() {
    this.restore()
  }

  /** Guarda la sesión tras un login exitoso. */
  setSession(response: AuthResponse, remember: boolean): void {
    const session: PersistedSession = {
      token: response.token,
      expiresAt: response.expiresAt,
      user: response.user,
    }
    this.clearStorage()
    this.storage(remember).setItem(SESSION_KEY, JSON.stringify(session))
    if (remember) {
      this.safe(() => localStorage.setItem(REMEMBER_EMAIL_KEY, response.user.email))
    } else {
      this.safe(() => localStorage.removeItem(REMEMBER_EMAIL_KEY))
    }
    this.apply(session)
  }

  /** Actualiza los datos del usuario (p. ej. tras consultar /auth/me). */
  updateUser(user: AuthUser): void {
    this._user.set(user)
  }

  /** Cierra la sesión y limpia el almacenamiento. */
  clear(): void {
    this._token.set(null)
    this._expiresAt.set(null)
    this._user.set(null)
    this.clearStorage()
  }

  /** true si hay token y no ha expirado (la expiración se evalúa en el momento de la llamada). */
  isSessionValid(): boolean {
    const expiresAt = this._expiresAt()
    return this.isAuthenticated() && expiresAt !== null && expiresAt > Date.now()
  }

  hasRole(...roles: RoleCode[]): boolean {
    const role = this.role()
    return role !== null && roles.includes(role)
  }

  hasPermission(...permissions: PermissionCode[]): boolean {
    const granted = this.permissions()
    return permissions.every((p) => granted.has(p))
  }

  /** Correo recordado para precargar el formulario de login. */
  rememberedEmail(): string | null {
    return this.safe(() => localStorage.getItem(REMEMBER_EMAIL_KEY)) ?? null
  }

  private restore(): void {
    const raw =
      this.safe(() => localStorage.getItem(SESSION_KEY)) ??
      this.safe(() => sessionStorage.getItem(SESSION_KEY))
    if (!raw) return
    try {
      const session = JSON.parse(raw) as PersistedSession
      if (new Date(session.expiresAt).getTime() > Date.now()) {
        this.apply(session)
      } else {
        this.clearStorage()
      }
    } catch {
      this.clearStorage()
    }
  }

  private apply(session: PersistedSession): void {
    this._token.set(session.token)
    this._expiresAt.set(new Date(session.expiresAt).getTime())
    this._user.set(session.user)
  }

  private storage(remember: boolean): Storage {
    return remember ? localStorage : sessionStorage
  }

  private clearStorage(): void {
    this.safe(() => localStorage.removeItem(SESSION_KEY))
    this.safe(() => sessionStorage.removeItem(SESSION_KEY))
  }

  /** El almacenamiento puede no estar disponible (modo privado, bloqueado). */
  private safe<T>(fn: () => T): T | null {
    try {
      return fn()
    } catch {
      return null
    }
  }
}
