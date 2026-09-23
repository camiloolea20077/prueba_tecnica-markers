import { DecimalPipe } from '@angular/common'
import { HttpErrorResponse } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core'
import { ConfirmationService, MessageService } from 'primeng/api'
import { ButtonModule } from 'primeng/button'
import { SkeletonModule } from 'primeng/skeleton'
import { TableModule } from 'primeng/table'
import { TagModule } from 'primeng/tag'
import { TooltipModule } from 'primeng/tooltip'
import { finalize } from 'rxjs'
import { RateCatalog } from '../../../../../credits/interfaces/credit.model'
import { CreditsStore } from '../../../../../credits/store/credits.store'
import { AdminInterestRateTier } from '../../../interfaces/interest-rate-tier.model'
import { InterestRatesService } from '../../../services/interest-rates.service'
import { TierFormDialogComponent } from '../../components/tier-form-dialog/tier-form-dialog.component'

interface CoverageSegment {
  from: number
  to: number
  /** Porcentaje del ancho total de la barra. */
  width: number
  tier: AdminInterestRateTier | null
}

/**
 * Administración de tramos de tasa EA con barra de cobertura de plazos (detecta huecos).
 */
@Component({
  selector: 'app-interest-rates',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    ButtonModule,
    SkeletonModule,
    TableModule,
    TagModule,
    TooltipModule,
    TierFormDialogComponent,
  ],
  templateUrl: './interest-rates.component.html',
})
export class InterestRatesComponent implements OnInit {
  private readonly service = inject(InterestRatesService)
  private readonly creditsStore = inject(CreditsStore)
  private readonly confirmation = inject(ConfirmationService)
  private readonly toast = inject(MessageService)

  protected readonly tiers = signal<AdminInterestRateTier[]>([])
  protected readonly catalog = signal<RateCatalog | null>(null)
  protected readonly loading = signal(true)
  protected readonly formVisible = signal(false)
  protected readonly editing = signal<AdminInterestRateTier | null>(null)

  /**
   * Divide el rango de plazos de la política en segmentos: cada tramo activo o un hueco sin tasa.
   */
  protected readonly coverage = computed<CoverageSegment[]>(() => {
    const c = this.catalog()
    if (!c) return []
    const total = c.maxTermMonths - c.minTermMonths + 1
    const active = this.tiers()
      .filter((t) => t.active)
      .sort((a, b) => a.minTermMonths - b.minTermMonths)
    const segments: CoverageSegment[] = []
    let cursor = c.minTermMonths
    const push = (from: number, to: number, tier: AdminInterestRateTier | null) =>
      segments.push({ from, to, tier, width: ((to - from + 1) / total) * 100 })

    for (const t of active) {
      const from = Math.max(t.minTermMonths, c.minTermMonths)
      const to = Math.min(t.maxTermMonths, c.maxTermMonths)
      if (from > cursor) push(cursor, from - 1, null)
      if (to >= from) push(from, to, t)
      cursor = Math.max(cursor, to + 1)
    }
    if (cursor <= c.maxTermMonths) push(cursor, c.maxTermMonths, null)
    return segments
  })

  protected readonly gaps = computed(() => this.coverage().filter((s) => !s.tier))

  ngOnInit(): void {
    this.creditsStore.loadCatalog().subscribe((c) => this.catalog.set(c))
    this.load()
  }

  protected load(): void {
    this.loading.set(true)
    this.service
      .findAll()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((tiers) => this.tiers.set(tiers))
  }

  protected openNew(): void {
    this.editing.set(null)
    this.formVisible.set(true)
  }

  protected openEdit(tier: AdminInterestRateTier): void {
    this.editing.set(tier)
    this.formVisible.set(true)
  }

  protected onSaved(tier: AdminInterestRateTier): void {
    this.toast.add({ severity: 'success', summary: 'Tramo guardado', detail: tier.name })
    this.load()
  }

  protected confirmDelete(tier: AdminInterestRateTier): void {
    this.confirmation.confirm({
      header: 'Eliminar tramo',
      message: `¿Eliminar "${tier.name}"? Los plazos de ${tier.minTermMonths} a ${tier.maxTermMonths} meses quedarán sin tasa sugerida si ningún otro tramo los cubre.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.service.delete(tier.id).subscribe({
          next: () => {
            this.toast.add({ severity: 'success', summary: 'Tramo eliminado', detail: tier.name })
            this.load()
          },
          error: (err: HttpErrorResponse) => {
            if (err.status === 0) return
            this.toast.add({
              severity: 'error',
              summary: 'No se pudo eliminar',
              detail: err.error?.message ?? 'Intenta de nuevo.',
            })
          },
        }),
    })
  }
}
