import { CurrencyPipe } from '@angular/common'
import { HttpErrorResponse } from '@angular/common/http'
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  model,
  output,
  signal,
  untracked,
} from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import {
  AbstractControl,
  FormControl,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms'
import { ButtonModule } from 'primeng/button'
import { DialogModule } from 'primeng/dialog'
import { MessageModule } from 'primeng/message'
import { TextareaModule } from 'primeng/textarea'
import { finalize, map, startWith } from 'rxjs'
import { Credit } from '../../../../../credits/interfaces/credit.model'
import { AdminCreditsService } from '../../../services/admin-credits.service'

export const REASON_MIN = 10
export const REASON_MAX = 500

/** Motivos frecuentes para agilizar el rechazo (se pueden editar). */
const QUICK_REASONS = [
  'Capacidad de pago insuficiente para el monto solicitado.',
  'Nivel de endeudamiento actual superior al permitido.',
  'Historial crediticio con reportes negativos.',
  'Información o documentación incompleta.',
]

/** Validador: el texto sin espacios de los extremos debe tener al menos `min` caracteres. */
function trimmedMinLength(min: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    String(control.value ?? '').trim().length >= min ? null : { trimmedMinLength: { min } }
}

/**
 * Rechazo con motivo obligatorio (lo verá el solicitante).
 */
@Component({
  selector: 'app-reject-credit-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    MessageModule,
    TextareaModule,
  ],
  templateUrl: './reject-credit-dialog.component.html',
})
export class RejectCreditDialogComponent {
  private readonly adminService = inject(AdminCreditsService)

  readonly visible = model(false)
  readonly credit = input<Credit | null>(null)
  readonly rejected = output<Credit>()
  readonly stale = output<string>()

  protected readonly quickReasons = QUICK_REASONS
  protected readonly maxLength = REASON_MAX
  protected readonly saving = signal(false)
  protected readonly serverError = signal<string | null>(null)

  protected readonly reason = new FormControl('', {
    nonNullable: true,
    validators: [
      Validators.required,
      trimmedMinLength(REASON_MIN),
      Validators.maxLength(REASON_MAX),
    ],
  })

  protected readonly length = toSignal(
    this.reason.valueChanges.pipe(
      startWith(''),
      map((v) => v.length),
    ),
    { initialValue: 0 },
  )

  constructor() {
    effect(() => {
      if (this.visible() && this.credit()) {
        untracked(() => {
          this.reason.reset('')
          this.serverError.set(null)
        })
      }
    })
  }

  protected invalid(): boolean {
    return this.reason.invalid && (this.reason.touched || this.reason.dirty)
  }

  protected pick(text: string): void {
    this.reason.setValue(text)
    this.reason.markAsDirty()
  }

  protected confirm(): void {
    const c = this.credit()
    if (!c || this.reason.invalid) {
      this.reason.markAsTouched()
      return
    }
    this.saving.set(true)
    this.serverError.set(null)
    this.adminService
      .reject(c.id, this.reason.value.trim())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (credit) => {
          this.visible.set(false)
          this.rejected.emit(credit)
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 0) return
          const message = err.error?.message ?? 'No fue posible rechazar el crédito.'
          if (err.status === 409 || err.status === 404) {
            this.visible.set(false)
            this.stale.emit(message)
          } else {
            this.serverError.set(message)
          }
        },
      })
  }
}
