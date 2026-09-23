import { CurrencyPipe } from '@angular/common'
import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { TableModule } from 'primeng/table'
import { AmortizationRow } from '../../../interfaces/credit.model'

/** Tabla de amortización paginada (12 cuotas por página = 1 año). */
@Component({
  selector: 'app-amortization-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, TableModule],
  templateUrl: './amortization-table.component.html',
})
export class AmortizationTableComponent {
  readonly rows = input<AmortizationRow[]>([])
}
