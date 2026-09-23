import { HttpErrorResponse } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { Router } from '@angular/router'
import { MessageService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { CheckboxModule } from 'primeng/checkbox'
import { DividerModule } from 'primeng/divider'
import { IconFieldModule } from 'primeng/iconfield'
import { InputIconModule } from 'primeng/inputicon'
import { InputTextModule } from 'primeng/inputtext'
import { MessageModule } from 'primeng/message'
import { PasswordModule } from 'primeng/password'
import { Popover, PopoverModule } from 'primeng/popover'
import { finalize } from 'rxjs'
import { AuthService } from '../../../../../core/auth/services/auth.service'
import { AuthStore } from '../../../../../core/auth/store/auth.store'
import { BrandLogoComponent } from '../../../../../shared/components/brand-logo/brand-logo.component'

interface DemoAccount {
  label: string
  description: string
  icon: string
  email: string
  password: string
}

/**
 * Pantalla de inicio de sesión (panel informativo + formulario).
 */
@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    CheckboxModule,
    DividerModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    MessageModule,
    PasswordModule,
    PopoverModule,
    BrandLogoComponent,
  ],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder)
  private readonly authService = inject(AuthService)
  private readonly store = inject(AuthStore)
  private readonly router = inject(Router)
  private readonly toast = inject(MessageService)

  /** URL a la que volver tras iniciar sesión (query param `returnUrl`). */
  readonly returnUrl = input<string | undefined>()

  protected readonly loading = signal(false)
  protected readonly serverError = signal<string | null>(null)
  protected readonly year = new Date().getFullYear()

  protected readonly benefits = [
    {
      icon: 'pi pi-calculator',
      title: 'Simulación al instante',
      text: 'Conoce tu cuota antes de solicitar.',
    },
    {
      icon: 'pi pi-percentage',
      title: 'Tasas transparentes',
      text: 'Tasa efectiva anual clara y sin sorpresas.',
    },
    {
      icon: 'pi pi-clock',
      title: 'Respuesta ágil',
      text: 'Sigue el estado de tu solicitud en línea.',
    },
    {
      icon: 'pi pi-shield',
      title: 'Seguridad bancaria',
      text: 'Acceso protegido con roles y permisos.',
    },
  ]

  protected readonly demoAccounts: DemoAccount[] = [
    {
      label: 'Usuario',
      description: 'Solicita y consulta créditos',
      icon: 'pi pi-user',
      email: 'usuario@test.com',
      password: '123',
    },
    {
      label: 'Administrador',
      description: 'Aprueba o rechaza solicitudes',
      icon: 'pi pi-briefcase',
      email: 'admin@test.com',
      password: '123',
    },
  ]

  protected readonly form = this.fb.nonNullable.group({
    email: [
      this.store.rememberedEmail() ?? '',
      [Validators.required, Validators.email, Validators.maxLength(150)],
    ],
    password: ['', [Validators.required, Validators.maxLength(100)]],
    remember: [!!this.store.rememberedEmail()],
  })

  /** Muestra el error de un campo solo cuando el usuario ya interactuó con él. */
  protected invalid(control: 'email' | 'password'): boolean {
    const c = this.form.controls[control]
    return c.invalid && (c.touched || c.dirty)
  }

  protected fillDemo(account: DemoAccount, popover: Popover): void {
    this.form.patchValue({ email: account.email, password: account.password })
    this.form.markAsDirty()
    this.serverError.set(null)
    popover.hide()
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }
    const { email, password, remember } = this.form.getRawValue()
    this.loading.set(true)
    this.serverError.set(null)

    this.authService
      .login({ email, password }, remember)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (user) => {
          this.toast.add({
            severity: 'success',
            summary: `¡Hola, ${user.fullName}!`,
            detail: 'Inicio de sesión exitoso.',
          })
          void this.router.navigateByUrl(this.safeReturnUrl())
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 0) return // el interceptor ya avisó
          this.serverError.set(err.error?.message ?? 'No fue posible iniciar sesión.')
          this.form.controls.password.reset()
        },
      })
  }

  /** Evita redirecciones abiertas: solo rutas internas. */
  private safeReturnUrl(): string {
    const url = this.returnUrl()
    return url && url.startsWith('/') && !url.startsWith('//') && !url.startsWith('/login')
      ? url
      : this.authService.homeUrl()
  }
}
