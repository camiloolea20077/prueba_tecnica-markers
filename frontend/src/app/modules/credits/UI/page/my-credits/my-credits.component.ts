import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common'
import { HttpErrorResponse } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core'
import { FormsModule } from '@angular/forms'
import { RouterLink } from '@angular/router'
import { ConfirmationService, MessageService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { SelectButtonModule } from 'primeng/selectbutton'
import { SkeletonModule } from 'primeng/skeleton'
import { TableModule } from 'primeng/table'
import { TooltipModule } from 'primeng/tooltip'
import { AuthStore } from '../../../../../core/auth/store/auth.store'
import { Credit } from '../../../interfaces/credit.model'
import { CreditsService } from '../../../services/credits.service'
import { CreditFilter, CreditsStore } from '../../../store/credits.store'
import { CreditDetailDialogComponent } from '../../components/credit-detail-dialog/credit-detail-dialog.component'
import { CreditStatusTagComponent } from '../../components/credit-status-tag/credit-status-tag.component'

/**
 * "Mis créditos": resumen, filtro por estado, tabla con detalle y cancelación de pendientes.
 */
@Component({
  selector: 'app-my-credits',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    FormsModule,
    RouterLink,
    ButtonModule,
    SelectButtonModule,
    SkeletonModule,
    TableModule,
    TooltipModule,
    CreditDetailDialogComponent,
    CreditStatusTagComponent,
  ],
  templateUrl: './my-credits.component.html',
})
export class MyCreditsComponent implements OnInit {
  protected readonly store = inject(CreditsStore)
  private readonly service = inject(CreditsService)
  private readonly auth = inject(AuthStore)
  private readonly confirmation = inject(ConfirmationService)
  private readonly toast = inject(MessageService)

  protected readonly canRequest = computed(() => this.auth.hasPermission('CREDIT_REQUEST'))
  protected readonly canCancel = computed(() => this.auth.hasPermission('CREDIT_CANCEL_OWN'))

  protected readonly selected = signal<Credit | null>(null)
  protected readonly detailVisible = signal(false)
  protected readonly cancellingId = signal<number | null>(null)
  protected readonly skeletonRows = Array.from({ length: 4 })

  protected readonly filterOptions = computed(() => {
    const s = this.store.stats()
    return [
      { label: `Todos (${s.total})`, value: 'ALL' },
      { label: `Pendientes (${s.pending})`, value: 'PENDING' },
      { label: `Aprobados (${s.approved})`, value: 'APPROVED' },
      { label: `Rechazados (${s.rejected})`, value: 'REJECTED' },
      { label: `Cancelados (${s.cancelled})`, value: 'CANCELLED' },
    ]
  })

  ngOnInit(): void {
    this.store.load()
  }

  protected onFilter(value: CreditFilter | null): void {
    this.store.setFilter(value ?? 'ALL')
  }

  protected openDetail(credit: Credit): void {
    this.selected.set(credit)
    this.detailVisible.set(true)
  }

  protected confirmCancel(credit: Credit): void {
    this.confirmation.confirm({
      header: 'Cancelar solicitud',
      message: `¿Deseas cancelar la solicitud #${credit.id}? Esta acción no se puede deshacer.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, cancelar',
      rejectLabel: 'Volver',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () => this.cancel(credit),
    })
  }

  private cancel(credit: Credit): void {
    this.cancellingId.set(credit.id)
    this.service.cancel(credit.id).subscribe({
      next: (updated) => {
        this.cancellingId.set(null)
        this.store.upsert(updated)
        if (this.selected()?.id === updated.id) this.selected.set(updated)
        this.toast.add({
          severity: 'success',
          summary: 'Solicitud cancelada',
          detail: `La solicitud #${updated.id} fue cancelada.`,
        })
      },
      error: (err: HttpErrorResponse) => {
        this.cancellingId.set(null)
        if (err.status === 0) return
        this.toast.add({
          severity: 'error',
          summary: 'No se pudo cancelar',
          detail: err.error?.message ?? 'Intenta de nuevo.',
        })
        // El estado pudo cambiar (p. ej. ya fue aprobado): refrescar
        this.store.load()
      },
    })
  }
}
