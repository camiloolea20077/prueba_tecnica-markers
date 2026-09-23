import { CurrencyPipe, DecimalPipe } from '@angular/common'
import { HttpErrorResponse } from '@angular/common/http'
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  input,
  model,
  OnInit,
  output,
  signal,
  untracked,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms'
import { ButtonModule } from 'primeng/button'
import { DialogModule } from 'primeng/dialog'
import { InputNumberModule } from 'primeng/inputnumber'
import { MessageModule } from 'primeng/message'
import { catchError, debounceTime, filter, finalize, of, switchMap, tap } from 'rxjs'
import { Credit, CreditQuote, RateCatalog } from '../../../../../credits/interfaces/credit.model'
import { CreditsService } from '../../../../../credits/services/credits.service'
import { CreditsStore } from '../../../../../credits/store/credits.store'
import { QuoteSummaryComponent } from '../../../../../credits/UI/components/quote-summary/quote-summary.component'
import { AdminCreditsService } from '../../../services/admin-credits.service'

/**
 * Aprobación: el analista define la tasa EA (precargada con la sugerida) y ve la cuota definitiva en vivo.
 */
@Component({
  selector: 'app-approve-credit-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputNumberModule,
    MessageModule,
    QuoteSummaryComponent,
  ],
  templateUrl: './approve-credit-dialog.component.html',
})
export class ApproveCreditDialogComponent implements OnInit {
  private readonly creditsService = inject(CreditsService)
  private readonly creditsStore = inject(CreditsStore)
  private readonly adminService = inject(AdminCreditsService)
  private readonly destroyRef = inject(DestroyRef)

  readonly visible = model(false)
  readonly credit = input<Credit | null>(null)
  readonly approved = output<Credit>()
  /** Error 409/404: el crédito cambió; la bandeja debe recargarse. */
  readonly stale = output<string>()

  protected readonly catalog = signal<RateCatalog | null>(null)
  protected readonly quote = signal<CreditQuote | null>(null)
  protected readonly simulating = signal(false)
  protected readonly saving = signal(false)
  protected readonly serverError = signal<string | null>(null)

  protected readonly rate = new FormControl<number | null>(null, Validators.required)

  /** Diferencia de cuota frente a la estimada con la tasa sugerida. */
  protected readonly paymentDelta = computed(() => {
    const q = this.quote()
    const c = this.credit()
    return q && c ? q.monthlyPayment - c.monthlyPayment : 0
  })

  constructor() {
    // Cada vez que se abre con un crédito: precargar la tasa sugerida
    effect(() => {
      const c = this.credit()
      const open = this.visible()
      if (c && open) {
        untracked(() => {
          this.serverError.set(null)
          this.quote.set(null)
          this.rate.setValue(c.suggestedAnnualRate)
          this.rate.markAsPristine()
        })
      }
    })
  }

  ngOnInit(): void {
    this.creditsStore.loadCatalog().subscribe((catalog) => {
      this.catalog.set(catalog)
      this.rate.setValidators([
        Validators.required,
        Validators.min(catalog.minAnnualRate),
        Validators.max(catalog.maxAnnualRate),
      ])
      this.rate.updateValueAndValidity()
    })

    this.rate.valueChanges
      .pipe(
        debounceTime(300),
        tap(() => {
          if (this.rate.invalid) this.quote.set(null)
        }),
        filter(() => this.rate.valid && !!this.credit()),
        switchMap((annualRate) => {
          const c = this.credit()!
          this.simulating.set(true)
          return this.creditsService
            .simulate({
              amount: c.amount,
              termMonths: c.termMonths,
              annualRate,
              includeSchedule: false,
            })
            .pipe(
              catchError(() => of(null)),
              finalize(() => this.simulating.set(false)),
            )
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((quote) => this.quote.set(quote))
  }

  protected useSuggested(): void {
    const c = this.credit()
    if (c) this.rate.setValue(c.suggestedAnnualRate)
  }

  protected confirm(): void {
    const c = this.credit()
    if (!c || this.rate.invalid) {
      this.rate.markAsTouched()
      return
    }
    this.saving.set(true)
    this.serverError.set(null)
    this.adminService
      .approve(c.id, this.rate.value!)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (credit) => {
          this.visible.set(false)
          this.approved.emit(credit)
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 0) return
          const message = err.error?.message ?? 'No fue posible aprobar el crédito.'
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
