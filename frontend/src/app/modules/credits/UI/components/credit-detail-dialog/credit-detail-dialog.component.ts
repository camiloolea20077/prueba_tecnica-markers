import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common'
import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
  input,
  model,
  output,
  signal,
} from '@angular/core'
import { ButtonModule } from 'primeng/button'
import { DialogModule } from 'primeng/dialog'
import { finalize } from 'rxjs'
import { AmortizationRow, Credit } from '../../../interfaces/credit.model'
import { CreditsService } from '../../../services/credits.service'
import { AmortizationTableComponent } from '../amortization-table/amortization-table.component'
import { CreditStatusTagComponent } from '../credit-status-tag/credit-status-tag.component'
import { QuoteSummaryComponent } from '../quote-summary/quote-summary.component'

/**
 * Detalle de un crédito: condiciones, fechas, motivo de rechazo y tabla de amortización bajo demanda.
 */
@Component({
  selector: 'app-credit-detail-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    ButtonModule,
    DialogModule,
    AmortizationTableComponent,
    CreditStatusTagComponent,
    QuoteSummaryComponent,
  ],
  templateUrl: './credit-detail-dialog.component.html',
})
export class CreditDetailDialogComponent {
  private readonly service = inject(CreditsService)

  readonly visible = model(false)
  readonly credit = input<Credit | null>(null)
  readonly canCancel = input(false)
  /** Muestra nombre y correo del solicitante (vista del analista). */
  readonly showApplicant = input(false)
  readonly cancel = output<Credit>()

  protected readonly schedule = signal<AmortizationRow[]>([])
  protected readonly loadingSchedule = signal(false)

  constructor() {
    // Al cambiar de crédito se descarta la tabla anterior
    effect(() => {
      this.credit()
      this.schedule.set([])
    })
  }

  /** La tabla se calcula con la tasa del crédito (definitiva o sugerida). */
  protected loadSchedule(c: Credit): void {
    this.loadingSchedule.set(true)
    this.service
      .simulate({
        amount: c.amount,
        termMonths: c.termMonths,
        annualRate: c.annualRate,
        includeSchedule: true,
      })
      .pipe(finalize(() => this.loadingSchedule.set(false)))
      .subscribe((quote) => this.schedule.set(quote.schedule))
  }
}
