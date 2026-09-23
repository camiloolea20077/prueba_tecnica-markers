import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common'
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms'
import { MessageService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { IconFieldModule } from 'primeng/iconfield'
import { InputIconModule } from 'primeng/inputicon'
import { InputTextModule } from 'primeng/inputtext'
import { SelectButtonModule } from 'primeng/selectbutton'
import { TableLazyLoadEvent, TableModule } from 'primeng/table'
import { TooltipModule } from 'primeng/tooltip'
import { debounceTime, distinctUntilChanged } from 'rxjs'
import { AuthStore } from '../../../../../../core/auth/store/auth.store'
import { Credit, CreditStatus } from '../../../../../credits/interfaces/credit.model'
import { CreditDetailDialogComponent } from '../../../../../credits/UI/components/credit-detail-dialog/credit-detail-dialog.component'
import { CreditStatusTagComponent } from '../../../../../credits/UI/components/credit-status-tag/credit-status-tag.component'
import { AdminCreditsStore } from '../../../store/admin-credits.store'
import { ApproveCreditDialogComponent } from '../../components/approve-credit-dialog/approve-credit-dialog.component'
import { RejectCreditDialogComponent } from '../../components/reject-credit-dialog/reject-credit-dialog.component'

type StatusFilter = CreditStatus | 'ALL'

/**
 * Bandeja del analista: resumen por estado, búsqueda, tabla paginada en servidor,
 * y acciones de aprobar (con tasa EA) o rechazar (con motivo).
 */
@Component({
  selector: 'app-admin-credits',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    SelectButtonModule,
    TableModule,
    TooltipModule,
    ApproveCreditDialogComponent,
    CreditDetailDialogComponent,
    CreditStatusTagComponent,
    RejectCreditDialogComponent,
  ],
  templateUrl: './admin-credits.component.html',
})
export class AdminCreditsComponent implements OnInit {
  protected readonly store = inject(AdminCreditsStore)
  private readonly auth = inject(AuthStore)
  private readonly toast = inject(MessageService)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly canApprove = computed(() => this.auth.hasPermission('CREDIT_APPROVE'))
  protected readonly canReject = computed(() => this.auth.hasPermission('CREDIT_REJECT'))

  protected readonly search = new FormControl(this.store.query().q, { nonNullable: true })
  protected readonly selected = signal<Credit | null>(null)
  protected readonly detailVisible = signal(false)
  protected readonly approveVisible = signal(false)
  protected readonly rejectVisible = signal(false)

  protected readonly statusFilter = computed<StatusFilter>(() => this.store.query().status ?? 'ALL')

  protected readonly statusOptions = computed(() => {
    const s = this.store.summary()
    return [
      { label: `Pendientes (${s.pending})`, value: 'PENDING' },
      { label: `Aprobados (${s.approved})`, value: 'APPROVED' },
      { label: `Rechazados (${s.rejected})`, value: 'REJECTED' },
      { label: `Cancelados (${s.cancelled})`, value: 'CANCELLED' },
      { label: `Todos (${s.total})`, value: 'ALL' },
    ]
  })

  ngOnInit(): void {
    // La primera carga la dispara la tabla (onLazyLoad)
    this.search.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((text) => this.store.setSearch(text))
  }

  protected onStatus(value: StatusFilter | null): void {
    this.store.setStatus(!value || value === 'ALL' ? null : value)
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    const size = event.rows ?? 10
    this.store.setPage(Math.floor((event.first ?? 0) / size), size)
  }

  protected clearSearch(): void {
    this.search.setValue('')
  }

  protected openDetail(credit: Credit): void {
    this.selected.set(credit)
    this.detailVisible.set(true)
  }

  protected openApprove(credit: Credit): void {
    this.selected.set(credit)
    this.approveVisible.set(true)
  }

  protected openReject(credit: Credit): void {
    this.selected.set(credit)
    this.rejectVisible.set(true)
  }

  protected onApproved(credit: Credit): void {
    this.toast.add({
      severity: 'success',
      summary: `Crédito #${credit.id} aprobado`,
      detail: `${credit.applicant.fullName} · ${credit.annualRate} % EA`,
    })
    this.store.refresh()
  }

  protected onRejected(credit: Credit): void {
    this.toast.add({
      severity: 'success',
      summary: `Crédito #${credit.id} rechazado`,
      detail: `Se notificará a ${credit.applicant.fullName}.`,
    })
    this.store.refresh()
  }

  /** Otro analista decidió primero o el crédito ya no existe. */
  protected onStale(message: string): void {
    this.toast.add({ severity: 'warn', summary: 'La solicitud cambió', detail: message })
    this.store.refresh()
  }
}
