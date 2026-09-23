import { CurrencyPipe, DecimalPipe } from '@angular/common'
import { HttpErrorResponse } from '@angular/common/http'
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
import { Router, RouterLink } from '@angular/router'
import { MessageService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { InputNumberModule } from 'primeng/inputnumber'
import { MessageModule } from 'primeng/message'
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  filter,
  finalize,
  map,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs'
import { CreditQuote, RateCatalog } from '../../../interfaces/credit.model'
import { CreditsService } from '../../../services/credits.service'
import { CreditsStore } from '../../../store/credits.store'
import { QuoteSummaryComponent } from '../../components/quote-summary/quote-summary.component'

const TERM_SHORTCUTS = [6, 12, 24, 36, 48, 60, 72, 84]

/**
 * Formulario de solicitud con simulación en vivo de la cuota estimada.
 * Los límites (monto, plazo) vienen de `/interest-rates`, no están quemados en el front.
 */
@Component({
  selector: 'app-request-credit',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputNumberModule,
    MessageModule,
    QuoteSummaryComponent,
  ],
  templateUrl: './request-credit.component.html',
})
export class RequestCreditComponent implements OnInit {
  private readonly fb = inject(FormBuilder)
  private readonly service = inject(CreditsService)
  protected readonly store = inject(CreditsStore)
  private readonly router = inject(Router)
  private readonly toast = inject(MessageService)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly catalog = signal<RateCatalog | null>(null)
  protected readonly quote = signal<CreditQuote | null>(null)
  protected readonly simulating = signal(false)
  protected readonly submitting = signal(false)
  protected readonly serverError = signal<string | null>(null)

  protected readonly form = this.fb.group({
    amount: this.fb.control<number | null>(10_000_000, Validators.required),
    termMonths: this.fb.control<number | null>(24, Validators.required),
  })

  private readonly term = toSignal(
    this.form.controls.termMonths.valueChanges.pipe(startWith(this.form.controls.termMonths.value)),
  )

  /** Tramo de tasa que corresponde al plazo elegido. */
  protected readonly tier = computed(() => {
    this.catalog() // recalcular cuando llegue el catálogo
    return this.store.tierFor(this.term())
  })

  protected readonly termShortcuts = computed(() => {
    const c = this.catalog()
    return c ? TERM_SHORTCUTS.filter((t) => t >= c.minTermMonths && t <= c.maxTermMonths) : []
  })

  ngOnInit(): void {
    this.store.loadCatalog().subscribe((catalog) => {
      this.catalog.set(catalog)
      const { amount, termMonths } = this.form.controls
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
      amount.updateValueAndValidity({ emitEvent: false })
      termMonths.updateValueAndValidity()
    })

    // Simulación en vivo: espera a que el usuario deje de escribir y cancela la petición anterior
    this.form.valueChanges
      .pipe(
        startWith(this.form.getRawValue()),
        debounceTime(350),
        map(() => this.form.getRawValue()),
        tap(() => {
          if (this.form.invalid) this.quote.set(null)
        }),
        filter(() => this.form.valid && !!this.catalog()),
        distinctUntilChanged((a, b) => a.amount === b.amount && a.termMonths === b.termMonths),
        switchMap(({ amount, termMonths }) => {
          this.simulating.set(true)
          return this.service
            .simulate({ amount: amount!, termMonths: termMonths!, includeSchedule: false })
            .pipe(
              catchError(() => of(null)),
              finalize(() => this.simulating.set(false)),
            )
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((quote) => this.quote.set(quote))
  }

  protected invalid(control: 'amount' | 'termMonths'): boolean {
    const c = this.form.controls[control]
    return c.invalid && (c.touched || c.dirty)
  }

  protected setTerm(months: number): void {
    this.form.controls.termMonths.setValue(months)
    this.form.controls.termMonths.markAsDirty()
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched()
      return
    }
    const { amount, termMonths } = this.form.getRawValue()
    this.submitting.set(true)
    this.serverError.set(null)

    this.service
      .request({ amount: amount!, termMonths: termMonths! })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (credit) => {
          this.store.upsert(credit)
          this.toast.add({
            severity: 'success',
            summary: 'Solicitud enviada',
            detail: `Tu solicitud #${credit.id} quedó pendiente de aprobación.`,
          })
          void this.router.navigate(['/creditos'])
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 0) return
          this.serverError.set(err.error?.message ?? 'No fue posible registrar la solicitud.')
        },
      })
  }
}
