import { CurrencyPipe, DecimalPipe } from '@angular/common'
import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { SkeletonModule } from 'primeng/skeleton'

export interface QuoteView {
  monthlyPayment: number
  annualRate: number
  monthlyRate: number
  totalInterest: number
  totalPayable: number
  amount: number
  termMonths: number
}

/**
 * Resumen de condiciones: cuota destacada en negro + tasas y totales.
 */
@Component({
  selector: 'app-quote-summary',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, DecimalPipe, SkeletonModule],
  templateUrl: './quote-summary.component.html',
})
export class QuoteSummaryComponent {
  /** null = cargando (muestra skeletons). */
  readonly quote = input<QuoteView | null>(null)
  readonly estimated = input(false)
}
