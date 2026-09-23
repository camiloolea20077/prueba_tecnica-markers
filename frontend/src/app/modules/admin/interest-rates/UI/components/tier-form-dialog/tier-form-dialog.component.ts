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
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms'
import { ButtonModule } from 'primeng/button'
import { DialogModule } from 'primeng/dialog'
import { InputNumberModule } from 'primeng/inputnumber'
import { InputTextModule } from 'primeng/inputtext'
import { MessageModule } from 'primeng/message'
import { ToggleSwitchModule } from 'primeng/toggleswitch'
import { finalize } from 'rxjs'
import { RateCatalog } from '../../../../../credits/interfaces/credit.model'
import { AdminInterestRateTier } from '../../../interfaces/interest-rate-tier.model'
import { InterestRatesService } from '../../../services/interest-rates.service'

/** El plazo mínimo no puede superar al máximo. */
function termRangeValidator(group: AbstractControl): ValidationErrors | null {
  const min = group.get('minTermMonths')?.value as number | null
  const max = group.get('maxTermMonths')?.value as number | null
  return min != null && max != null && min > max ? { termRange: true } : null
}

/**
 * Alta / edición de un tramo de tasa EA. La superposición con otros tramos la valida el backend (422).
 */
@Component({
  selector: 'app-tier-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputNumberModule,
    InputTextModule,
    MessageModule,
    ToggleSwitchModule,
  ],
  templateUrl: './tier-form-dialog.component.html',
})
export class TierFormDialogComponent {
  private readonly fb = inject(FormBuilder)
  private readonly service = inject(InterestRatesService)

  readonly visible = model(false)
  /** null = nuevo tramo. */
  readonly tier = input<AdminInterestRateTier | null>(null)
  readonly catalog = input<RateCatalog | null>(null)
  readonly saved = output<AdminInterestRateTier>()

  protected readonly saving = signal(false)
  protected readonly serverError = signal<string | null>(null)

  protected readonly form = this.fb.group(
    {
      name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(80)]),
      minTermMonths: this.fb.control<number | null>(null, Validators.required),
      maxTermMonths: this.fb.control<number | null>(null, Validators.required),
      annualEffectiveRate: this.fb.control<number | null>(null, Validators.required),
      active: this.fb.nonNullable.control(true),
    },
    { validators: termRangeValidator },
  )

  constructor() {
    // Límites de la política desde el catálogo
    effect(() => {
      const c = this.catalog()
      if (!c) return
      untracked(() => {
        const { minTermMonths, maxTermMonths, annualEffectiveRate } = this.form.controls
        const termValidators = [
          Validators.required,
          Validators.min(c.minTermMonths),
          Validators.max(c.maxTermMonths),
        ]
        minTermMonths.setValidators(termValidators)
        maxTermMonths.setValidators(termValidators)
        annualEffectiveRate.setValidators([
          Validators.required,
          Validators.min(c.minAnnualRate),
          Validators.max(c.maxAnnualRate),
        ])
        this.form.updateValueAndValidity()
      })
    })

    // Cargar datos al abrir
    effect(() => {
      if (!this.visible()) return
      const tier = this.tier()
      untracked(() => {
        this.serverError.set(null)
        this.form.reset({
          name: tier?.name ?? '',
          minTermMonths: tier?.minTermMonths ?? null,
          maxTermMonths: tier?.maxTermMonths ?? null,
          annualEffectiveRate: tier?.annualEffectiveRate ?? null,
          active: tier?.active ?? true,
        })
      })
    })
  }

  protected invalid(
    name: 'name' | 'minTermMonths' | 'maxTermMonths' | 'annualEffectiveRate',
  ): boolean {
    const c = this.form.controls[name]
    return c.invalid && (c.touched || c.dirty)
  }

  protected rangeInvalid(): boolean {
    const { minTermMonths, maxTermMonths } = this.form.controls
    return this.form.hasError('termRange') && (minTermMonths.dirty || maxTermMonths.dirty)
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }
    const v = this.form.getRawValue()
    this.saving.set(true)
    this.serverError.set(null)
    this.service
      .save(this.tier()?.id ?? null, {
        name: v.name.trim(),
        minTermMonths: v.minTermMonths!,
        maxTermMonths: v.maxTermMonths!,
        annualEffectiveRate: v.annualEffectiveRate!,
        active: v.active,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (tier) => {
          this.visible.set(false)
          this.saved.emit(tier)
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 0) return
          this.serverError.set(err.error?.message ?? 'No fue posible guardar el tramo.')
        },
      })
  }
}
