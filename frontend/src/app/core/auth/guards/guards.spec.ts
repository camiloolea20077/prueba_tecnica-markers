import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router'
import { MessageService } from 'primeng/api'
import { AuthResponse, AuthUser } from '../interfaces/auth.model'
import { AuthStore } from '../store/auth.store'
import { authGuard } from './auth.guard'
import { guestGuard } from './guest.guard'
import { roleGuard } from './role.guard'

const USER: AuthUser = {
  id: 1,
  fullName: 'Usuario Demo',
  email: 'usuario@test.com',
  role: 'USER',
  roleName: 'Usuario',
  permissions: ['CREDIT_REQUEST', 'CREDIT_VIEW_OWN'],
}

function login(user: AuthUser): void {
  const res: AuthResponse = {
    token: 't',
    tokenType: 'Bearer',
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    user,
  }
  TestBed.inject(AuthStore).setSession(res, false)
}

function route(data: Record<string, unknown> = {}): ActivatedRouteSnapshot {
  return { data } as unknown as ActivatedRouteSnapshot
}

const state = { url: '/admin/creditos' } as RouterStateSnapshot

function run<T>(fn: () => T): T {
  return TestBed.runInInjectionContext(fn)
}

describe('Guards de autenticación', () => {
  let toast: jasmine.SpyObj<MessageService>

  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    toast = jasmine.createSpyObj('MessageService', ['add'])
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MessageService, useValue: toast },
      ],
    })
  })

  const serialize = (tree: unknown) => TestBed.inject(Router).serializeUrl(tree as UrlTree)

  describe('authGuard', () => {
    it('sin sesión redirige al login conservando la URL', () => {
      const result = run(() => authGuard(route(), state))
      expect(serialize(result)).toBe('/login?returnUrl=%2Fadmin%2Fcreditos')
    })

    it('con sesión permite el acceso', () => {
      login(USER)
      expect(run(() => authGuard(route(), state))).toBeTrue()
    })
  })

  describe('guestGuard', () => {
    it('sin sesión permite ver el login', () => {
      expect(run(() => guestGuard(route(), state))).toBeTrue()
    })

    it('con sesión envía al inicio', () => {
      login(USER)
      expect(serialize(run(() => guestGuard(route(), state)))).toBe('/inicio')
    })
  })

  describe('roleGuard', () => {
    it('USER no entra a una ruta de ADMIN y ve un aviso', () => {
      login(USER)
      const result = run(() => roleGuard(route({ roles: ['ADMIN'] }), state))
      expect(serialize(result)).toBe('/inicio')
      expect(toast.add).toHaveBeenCalled()
    })

    it('ADMIN entra a una ruta de ADMIN', () => {
      login({ ...USER, role: 'ADMIN', permissions: ['CREDIT_APPROVE'] })
      expect(run(() => roleGuard(route({ roles: ['ADMIN'] }), state))).toBeTrue()
    })

    it('valida permisos declarados en data.permissions', () => {
      login(USER)
      expect(run(() => roleGuard(route({ permissions: ['CREDIT_REQUEST'] }), state))).toBeTrue()
      expect(
        serialize(run(() => roleGuard(route({ permissions: ['CREDIT_APPROVE'] }), state))),
      ).toBe('/inicio')
    })
  })
})
