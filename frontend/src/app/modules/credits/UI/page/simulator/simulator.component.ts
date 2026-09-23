import { CurrencyPipe, DecimalPipe } from '@angular/common'
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core'
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop'
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'
import { RouterLink } from '@angular/router'
import { ButtonModule } from 'primeng/button'
import { InputNumberModule } from 'primeng/inputnumber'
import { ToggleSwitchModule } from 'primeng/toggleswitch'
import {
  catchError,
  debounceTime,
  filter,
  finalize,
  map,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs'
import { AuthStore } from '../../../../../core/auth/store/auth.store'
import { CreditQuote, RateCatalog } from '../../../interfaces/credit.model'
import { CreditsService } from '../../../services/credits.service'
import { CreditsStore } from '../../../store/credits.store'
import { AmortizationTableComponent } from '../../components/amortization-table/amortization-table.component'
import { QuoteSummaryComponent } from '../../components/quote-summary/quote-summary.component'

/**
 * Simulador: monto, plazo y tasa EA (la del tramo o una propia) → cuota y tabla de amortización.
 */
@Component({
  selector: 'app-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputNumberModule,
    ToggleSwitchModule,
    QuoteSummaryComponent,
    AmortizationTableComponent,
  ],
  templateUrl: './simulator.component.html',
})
export class SimulatorComponent implements OnInit {
  private readonly fb = inject(FormBuilder)
  private readonly service = inject(CreditsService)
  private readonly store = inject(CreditsStore)
  private readonly auth = inject(AuthStore)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly catalog = signal<RateCatalog | null>(null)
  protected readonly quote = signal<CreditQuote | null>(null)
  protected readonly simulating = signal(false)
  protected readonly canRequest = computed(() => this.auth.hasPermission('CREDIT_REQUEST'))

  protected readonly form = this.fb.group({
    amount: this.fb.control<number | null>(10_000_000, Validators.required),
    termMonths: this.fb.control<number | null>(12, Validators.required),
    useTierRate: this.fb.nonNullable.control(true),
    annualRate: this.fb.control<number | null>({ value: null, disabled: true }),
  })

  private readonly term = toSignal(
    this.form.controls.termMonths.valueChanges.pipe(startWith(this.form.controls.termMonths.value)),
  )
  protected readonly useTierRate = toSignal(
    this.form.controls.useTierRate.valueChanges.pipe(startWith(true)),
    { initialValue: true },
  )
  protected readonly tier = computed(() => {
    this.catalog()
    return this.store.tierFor(this.term())
  })

  ngOnInit(): void {
    this.store.loadCatalog().subscribe((catalog) => {
      this.catalog.set(catalog)
      const { amount, termMonths, annualRate } = this.form.controls
      amount.setValidators([
        Validators.required,
        Validators.min(catalog.minAmount),
        Validators.max(catalog.maxAmount),
      ])
      termMonths.setValidators([
        Validators.required,
        Validators.min(catalog.minTermMonths),
        Validators.max(catalog.maxTermMonths),
      ])
      annualRate.setValidators([
        Validators.required,
        Validators.min(catalog.minAnnualRate),
        Validators.max(catalog.maxAnnualRate),
      ])
      this.form.updateValueAndValidity()
    })

    // Tasa propia: se habilita el campo y se precarga con la del tramo
    this.form.controls.useTierRate.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((useTier) => {
        const rate = this.form.controls.annualRate
        if (useTier) {
          rate.disable()
        } else {
          rate.setValue(this.tier()?.annualEffectiveRate ?? null)
          rate.enable()
        }
      })

    this.form.valueChanges
      .pipe(
        startWith(null),
        debounceTime(350),
        tap(() => {
          if (this.form.invalid) this.quote.set(null)
        }),
        filter(() => this.form.valid && !!this.catalog()),
        map(() => this.form.getRawValue()),
        switchMap((v) => {
          this.simulating.set(true)
          return this.service
            .simulate({
              amount: v.amount!,
              termMonths: v.termMonths!,
              annualRate: v.useTierRate ? null : v.annualRate,
              includeSchedule: true,
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

  protected invalid(control: 'amount' | 'termMonths' | 'annualRate'): boolean {
    const c = this.form.controls[control]
    return c.invalid && (c.touched || c.dirty)
  }
}
