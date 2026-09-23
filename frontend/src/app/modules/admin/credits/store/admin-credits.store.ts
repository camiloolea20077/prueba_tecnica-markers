import { computed, inject, Injectable, signal } from '@angular/core'
import { finalize, Subscription } from 'rxjs'
import { PageResponse } from '../../../../core/models/page.model'
import { Credit, CreditStatus } from '../../../credits/interfaces/credit.model'
import { AdminCreditQuery, CreditSummary } from '../interfaces/admin-credit.model'
import { AdminCreditsService } from '../services/admin-credits.service'

const EMPTY_SUMMARY: CreditSummary = {
  pending: 0,
  approved: 0,
  rejected: 0,
  cancelled: 0,
  total: 0,
}

/**
 * Estado de la bandeja del analista: filtros, página actual y resumen por estado.
 * Paginación y filtros se resuelven en el servidor.
 */
@Injectable({ providedIn: 'root' })
export class AdminCreditsStore {
  private readonly service = inject(AdminCreditsService)
  private searchSub?: Subscription

  // Estado privado
  private readonly _query = signal<AdminCreditQuery>({
    status: 'PENDING',
    q: '',
    page: 0,
    size: 10,
  })
  private readonly _page = signal<PageResponse<Credit> | null>(null)
  private readonly _summary = signal<CreditSummary>(EMPTY_SUMMARY)
  private readonly _loading = signal(false)

  // Estado público
  readonly query = this._query.asReadonly()
  readonly page = this._page.asReadonly()
  readonly summary = this._summary.asReadonly()
  readonly loading = this._loading.asReadonly()
  readonly credits = computed(() => this._page()?.content ?? [])
  readonly totalRecords = computed(() => this._page()?.totalElements ?? 0)
  readonly first = computed(() => this._query().page * this._query().size)

  setStatus(status: CreditStatus | null): void {
    this._query.update((q) => ({ ...q, status, page: 0 }))
    this.load()
  }

  setSearch(text: string): void {
    this._query.update((q) => ({ ...q, q: text, page: 0 }))
    this.load()
  }

  setPage(page: number, size: number): void {
    const current = this._query()
    if (current.page === page && current.size === size && this._page()) return
    this._query.set({ ...current, page, size })
    this.load()
  }

  /** Recarga la página actual y el resumen (p. ej. tras aprobar o rechazar). */
  refresh(): void {
    this.load()
  }

  load(): void {
    this.searchSub?.unsubscribe() // descarta respuestas de filtros anteriores
    this._loading.set(true)
    this.searchSub = this.service
      .search(this._query())
      .pipe(finalize(() => this._loading.set(false)))
      .subscribe((page) => {
        // Si la página quedó vacía tras una decisión, retroceder una
        if (page.content.length === 0 && page.page > 0) {
          this._query.update((q) => ({ ...q, page: q.page - 1 }))
          this.load()
          return
        }
        this._page.set(page)
      })
    this.service.summary().subscribe((summary) => this._summary.set(summary))
  }
}
