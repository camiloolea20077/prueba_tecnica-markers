import { TestBed } from '@angular/core/testing'
import { AuthResponse } from '../interfaces/auth.model'
import { AuthStore } from './auth.store'

function response(overrides: Partial<AuthResponse> = {}): AuthResponse {
  return {
    token: 'jwt-token',
    tokenType: 'Bearer',
    expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    user: {
      id: 2,
      fullName: 'Administrador',
      email: 'admin@test.com',
      role: 'ADMIN',
      roleName: 'Administrador',
      permissions: ['CREDIT_APPROVE', 'CREDIT_REJECT', 'CREDIT_VIEW_ALL'],
    },
    ...overrides,
  }
}

describe('AuthStore', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    TestBed.configureTestingModule({})
  })

  it('sin "Recordar" guarda la sesión en sessionStorage', () => {
    const store = TestBed.inject(AuthStore)
    store.setSession(response(), false)

    expect(sessionStorage.getItem('dc_session')).not.toBeNull()
    expect(localStorage.getItem('dc_session')).toBeNull()
    expect(store.isSessionValid()).toBeTrue()
    expect(store.rememberedEmail()).toBeNull()
  })

  it('con "Recordar" guarda la sesión y el correo en localStorage', () => {
    const store = TestBed.inject(AuthStore)
    store.setSession(response(), true)

    expect(localStorage.getItem('dc_session')).not.toBeNull()
    expect(store.rememberedEmail()).toBe('admin@test.com')
  })

  it('expone rol y permisos', () => {
    const store = TestBed.inject(AuthStore)
    store.setSession(response(), false)

    expect(store.isAdmin()).toBeTrue()
    expect(store.hasRole('ADMIN')).toBeTrue()
    expect(store.hasRole('USER')).toBeFalse()
    expect(store.hasPermission('CREDIT_APPROVE')).toBeTrue()
    expect(store.hasPermission('CREDIT_APPROVE', 'CREDIT_REQUEST')).toBeFalse()
  })

  it('restaura una sesión vigente al crearse', () => {
    TestBed.inject(AuthStore).setSession(response(), true)
    TestBed.resetTestingModule()

    const restored = TestBed.inject(AuthStore)
    expect(restored.user()?.email).toBe('admin@test.com')
    expect(restored.isSessionValid()).toBeTrue()
  })

  it('descarta una sesión expirada al restaurar', () => {
    const expired = response({ expiresAt: new Date(Date.now() - 1000).toISOString() })
    localStorage.setItem('dc_session', JSON.stringify(expired))

    const store = TestBed.inject(AuthStore)
    expect(store.isAuthenticated()).toBeFalse()
    expect(localStorage.getItem('dc_session')).toBeNull()
  })

  it('clear() elimina estado y almacenamiento', () => {
    const store = TestBed.inject(AuthStore)
    store.setSession(response(), true)
    store.clear()

    expect(store.isAuthenticated()).toBeFalse()
    expect(store.token()).toBeNull()
    expect(localStorage.getItem('dc_session')).toBeNull()
  })
})
