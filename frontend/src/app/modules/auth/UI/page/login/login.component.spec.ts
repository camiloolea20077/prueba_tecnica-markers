import { HttpErrorResponse } from '@angular/common/http'
import { ComponentFixture, TestBed } from '@angular/core/testing'
import { provideNoopAnimations } from '@angular/platform-browser/animations'
import { provideRouter, Router } from '@angular/router'
import { MessageService } from 'primeng/api'
import { of, throwError } from 'rxjs'
import { AuthService } from '../../../../../core/auth/services/auth.service'
import { LoginComponent } from './login.component'

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>
  let component: LoginComponent
  let authService: jasmine.SpyObj<AuthService>
  let router: Router

  // Acceso a miembros protegidos solo para las pruebas
  const form = () => (component as any).form
  const submit = () => (component as any).submit()
  const serverError = () => (component as any).serverError()

  beforeEach(async () => {
    localStorage.clear()
    sessionStorage.clear()
    authService = jasmine.createSpyObj('AuthService', ['login', 'homeUrl'])
    authService.homeUrl.and.returnValue('/inicio')

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        MessageService,
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents()

    fixture = TestBed.createComponent(LoginComponent)
    component = fixture.componentInstance
    router = TestBed.inject(Router)
    spyOn(router, 'navigateByUrl').and.resolveTo(true)
    fixture.detectChanges()
  })

  it('no envía el formulario vacío y marca los campos', () => {
    submit()
    expect(authService.login).not.toHaveBeenCalled()
    expect(form().controls.email.touched).toBeTrue()
    expect(form().controls.email.hasError('required')).toBeTrue()
    expect(form().controls.password.hasError('required')).toBeTrue()
  })

  it('valida el formato del correo', () => {
    form().controls.email.setValue('no-es-correo')
    expect(form().controls.email.hasError('email')).toBeTrue()
  })

  it('muestra los mensajes de validación en pantalla', () => {
    submit()
    fixture.detectChanges()
    const text = fixture.nativeElement.textContent as string
    expect(text).toContain('El usuario es obligatorio')
    expect(text).toContain('La contraseña es obligatoria')
  })

  it('con credenciales válidas inicia sesión y navega al inicio', () => {
    authService.login.and.returnValue(
      of({
        id: 1,
        fullName: 'Usuario Demo',
        email: 'usuario@test.com',
        role: 'USER',
        roleName: 'Usuario',
        permissions: [],
      }),
    )
    form().setValue({ email: 'usuario@test.com', password: '123', remember: true })

    submit()

    expect(authService.login).toHaveBeenCalledWith(
      { email: 'usuario@test.com', password: '123' },
      true,
    )
    expect(router.navigateByUrl).toHaveBeenCalledWith('/inicio')
  })

  it('respeta returnUrl interno e ignora URLs externas', () => {
    authService.login.and.returnValue(of({} as any))
    form().setValue({ email: 'usuario@test.com', password: '123', remember: false })

    fixture.componentRef.setInput('returnUrl', '//sitio-malicioso.com')
    submit()
    expect(router.navigateByUrl).toHaveBeenCalledWith('/inicio')

    fixture.componentRef.setInput('returnUrl', '/creditos')
    submit()
    expect(router.navigateByUrl).toHaveBeenCalledWith('/creditos')
  })

  it('con credenciales incorrectas muestra el mensaje del backend y limpia la contraseña', () => {
    authService.login.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            error: {
              status: 401,
              message: 'Correo o contraseña incorrectos',
              error: true,
              data: null,
            },
          }),
      ),
    )
    form().setValue({ email: 'admin@test.com', password: 'mala', remember: false })

    submit()

    expect(serverError()).toBe('Correo o contraseña incorrectos')
    expect(form().controls.password.value).toBe('')
    expect(router.navigateByUrl).not.toHaveBeenCalled()
  })
})
