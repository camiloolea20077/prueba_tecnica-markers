import { TestBed } from '@angular/core/testing'
import { AuthUser } from '../../auth/interfaces/auth.model'
import { AuthStore } from '../../auth/store/auth.store'
import { MenuService } from './menu.service'

const USER: AuthUser = {
  id: 1,
  fullName: 'Usuario Demo',
  email: 'usuario@test.com',
  role: 'USER',
  roleName: 'Usuario',
  permissions: ['CREDIT_REQUEST', 'CREDIT_VIEW_OWN', 'CREDIT_CANCEL_OWN', 'CREDIT_SIMULATE'],
}

const ADMIN: AuthUser = {
  id: 2,
  fullName: 'Administrador',
  email: 'admin@test.com',
  role: 'ADMIN',
  roleName: 'Administrador',
  permissions: [
    'CREDIT_SIMULATE',
    'CREDIT_VIEW_ALL',
    'CREDIT_APPROVE',
    'CREDIT_REJECT',
    'USER_MANAGE',
    'RATE_MANAGE',
  ],
}

describe('MenuService', () => {
  let store: AuthStore
  let menu: MenuService

  const loginAs = (user: AuthUser) =>
    store.setSession(
      {
        token: 't',
        tokenType: 'Bearer',
        expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
        user,
      },
      false,
    )
  const labels = () => menu.sections().flatMap((s) => s.items.map((i) => i.label))
  const sectionNames = () => menu.sections().map((s) => s.label)

  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    TestBed.configureTestingModule({})
    store = TestBed.inject(AuthStore)
    menu = TestBed.inject(MenuService)
  })

  it('USER ve sus créditos y el simulador, pero no la administración', () => {
    loginAs(USER)
    expect(labels()).toEqual(['Inicio', 'Mis créditos', 'Solicitar crédito', 'Simulador'])
    expect(sectionNames()).not.toContain('Administración')
  })

  it('ADMIN ve la administración y no las opciones de solicitante', () => {
    loginAs(ADMIN)
    expect(labels()).toEqual(['Inicio', 'Simulador', 'Solicitudes', 'Tasas de interés', 'Usuarios'])
    expect(sectionNames()).not.toContain('Mis créditos')
  })

  it('oculta secciones que quedan vacías', () => {
    loginAs({ ...USER, permissions: [] })
    expect(sectionNames()).toEqual(['General'])
  })

  it('los accesos rápidos excluyen el inicio', () => {
    loginAs(USER)
    expect(menu.shortcuts().map((i) => i.route)).not.toContain('/inicio')
  })

  it('se recalcula al cambiar de usuario', () => {
    loginAs(USER)
    expect(labels()).toContain('Solicitar crédito')
    loginAs(ADMIN)
    expect(labels()).not.toContain('Solicitar crédito')
    expect(labels()).toContain('Solicitudes')
  })
})
