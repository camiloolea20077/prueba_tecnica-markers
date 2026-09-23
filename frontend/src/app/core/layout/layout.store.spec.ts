import { provideHttpClient } from '@angular/common/http'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { TestBed } from '@angular/core/testing'
import { provideRouter } from '@angular/router'
import { Confirmation, ConfirmationService } from 'primeng/api'
import { AuthService } from '../auth/services/auth.service'
import { LayoutStore } from './layout.store'

describe('LayoutStore', () => {
  let confirmation: ConfirmationService

  beforeEach(() => {
    localStorage.clear()
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        ConfirmationService,
      ],
    })
    confirmation = TestBed.inject(ConfirmationService)
  })

  it('recuerda la barra lateral colapsada', () => {
    TestBed.inject(LayoutStore).toggleCollapsed()
    expect(localStorage.getItem('dc_sidebar_collapsed')).toBe('true')

    TestBed.resetTestingModule()
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        ConfirmationService,
      ],
    })
    expect(TestBed.inject(LayoutStore).collapsed()).toBeTrue()
  })

  it('abre y cierra el panel móvil', () => {
    const layout = TestBed.inject(LayoutStore)
    layout.toggleMobile()
    expect(layout.mobileOpen()).toBeTrue()
    layout.closeMobile()
    expect(layout.mobileOpen()).toBeFalse()
  })

  it('cierra sesión solo si el usuario confirma', () => {
    const auth = TestBed.inject(AuthService)
    const logout = spyOn(auth, 'logout')
    let captured: Confirmation | undefined
    spyOn(confirmation, 'confirm').and.callFake((c: Confirmation) => {
      captured = c
      return confirmation
    })

    TestBed.inject(LayoutStore).confirmLogout()
    expect(logout).not.toHaveBeenCalled()

    captured!.accept!()
    expect(logout).toHaveBeenCalled()
  })
})
