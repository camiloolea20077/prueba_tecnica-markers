import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { TagModule } from 'primeng/tag'
import { CreditStatus } from '../../../interfaces/credit.model'

const STATUS: Record<
  CreditStatus,
  { label: string; icon: string; severity: 'warn' | 'success' | 'danger' | 'secondary' }
> = {
  PENDING: { label: 'Pendiente', icon: 'pi pi-clock', severity: 'warn' },
  APPROVED: { label: 'Aprobado', icon: 'pi pi-check-circle', severity: 'success' },
  REJECTED: { label: 'Rechazado', icon: 'pi pi-times-circle', severity: 'danger' },
  CANCELLED: { label: 'Cancelado', icon: 'pi pi-ban', severity: 'secondary' },
}

/** Etiqueta del estado de un crédito. */
@Component({
  selector: 'app-credit-status-tag',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TagModule],
  templateUrl: './credit-status-tag.component.html',
})
export class CreditStatusTagComponent {
  readonly status = input.required<CreditStatus>()
  protected readonly config = computed(() => STATUS[this.status()])
}
