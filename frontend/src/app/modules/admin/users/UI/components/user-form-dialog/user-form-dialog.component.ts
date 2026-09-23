import { HttpErrorResponse } from '@angular/common/http'
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  model,
  output,
  signal,
  untracked,
} from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { ButtonModule } from 'primeng/button'
import { CheckboxModule } from 'primeng/checkbox'
import { DialogModule } from 'primeng/dialog'
import { InputTextModule } from 'primeng/inputtext'
import { MessageModule } from 'primeng/message'
import { PasswordModule } from 'primeng/password'
import { SelectModule } from 'primeng/select'
import { ToggleSwitchModule } from 'primeng/toggleswitch'
import { finalize, startWith } from 'rxjs'
import { RoleCode } from '../../../../../../core/auth/interfaces/auth.model'
import { AdminUser, RoleOption } from '../../../interfaces/admin-user.model'
import { AdminUsersService } from '../../../services/admin-users.service'

const PASSWORD_VALIDATORS = [Validators.required, Validators.minLength(6), Validators.maxLength(72)]

/**
 * Alta / edición de usuario. En edición la contraseña es opcional ("Cambiar contraseña").
 */
@Component({
  selector: 'app-user-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    CheckboxModule,
    DialogModule,
    InputTextModule,
    MessageModule,
    PasswordModule,
    SelectModule,
    ToggleSwitchModule,
  ],
  templateUrl: './user-form-dialog.component.html',
})
export class UserFormDialogComponent {
  private readonly fb = inject(FormBuilder)
  private readonly service = inject(AdminUsersService)

  readonly visible = model(false)
  /** null = nuevo usuario. */
  readonly user = input<AdminUser | null>(null)
  readonly roles = input<RoleOption[]>([])
  /** Id del administrador en sesión: no puede cambiar su propio rol ni desactivarse. */
  readonly currentUserId = input<number | null>(null)
  readonly saved = output<AdminUser>()

  protected readonly saving = signal(false)
  protected readonly serverError = signal<string | null>(null)
  protected readonly isEdit = computed(() => !!this.user())
  protected readonly isSelf = computed(
    () => !!this.user() && this.user()!.id === this.currentUserId(),
  )

  protected readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    role: this.fb.nonNullable.control<RoleCode>('USER', Validators.required),
    active: [true],
    changePassword: [false],
    password: ['', PASSWORD_VALIDATORS],
  })

  private readonly changePassword = toSignal(
    this.form.controls.changePassword.valueChanges.pipe(startWith(false)),
    { initialValue: false },
  )
  /** En alta siempre se pide contraseña; en edición solo si se marca "Cambiar contraseña". */
  protected readonly showPassword = computed(() => !this.isEdit() || this.changePassword())

  protected readonly selectedRole = toSignal(
    this.form.controls.role.valueChanges.pipe(startWith(this.form.controls.role.value)),
  )
  protected readonly rolePermissions = computed(
    () => this.roles().find((r) => r.code === this.selectedRole())?.permissions.length ?? 0,
  )

  constructor() {
    // Cargar datos al abrir
    effect(() => {
      if (!this.visible()) return
      const user = this.user()
      const self = this.isSelf()
      untracked(() => {
        this.serverError.set(null)
        this.form.reset({
          fullName: user?.fullName ?? '',
          email: user?.email ?? '',
          role: user?.role ?? 'USER',
          active: user?.active ?? true,
          changePassword: false,
          password: '',
        })
        // Autoprotección: el backend también lo impide (422)
        if (self) {
          this.form.controls.role.disable()
          this.form.controls.active.disable()
        } else {
          this.form.controls.role.enable()
          this.form.controls.active.enable()
        }
        this.syncPasswordValidators()
      })
    })

    effect(() => {
      this.showPassword()
      untracked(() => this.syncPasswordValidators())
    })
  }

  protected invalid(name: 'fullName' | 'email' | 'password'): boolean {
    const c = this.form.controls[name]
    return c.invalid && (c.touched || c.dirty)
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }
    const v = this.form.getRawValue()
    const base = {
      fullName: v.fullName.trim(),
      email: v.email.trim(),
      role: v.role,
      active: v.active,
    }
    const payload = this.isEdit()
      ? { ...base, password: this.showPassword() ? v.password : null }
      : { ...base, password: v.password }

    this.saving.set(true)
    this.serverError.set(null)
    this.service
      .save(this.user()?.id ?? null, payload)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (user) => {
          this.visible.set(false)
          this.saved.emit(user)
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 0) return
          const message = err.error?.message ?? 'No fue posible guardar el usuario.'
          this.serverError.set(message)
          if (err.status === 409) {
            this.form.controls.email.setErrors({ taken: true })
            this.form.controls.email.markAsTouched()
          }
        },
      })
  }

  private syncPasswordValidators(): void {
    const password = this.form.controls.password
    if (this.showPassword()) {
      password.setValidators(PASSWORD_VALIDATORS)
    } else {
      password.clearValidators()
      password.setValue('', { emitEvent: false })
    }
    password.updateValueAndValidity({ emitEvent: false })
  }
}
