import { computed, effect, inject, Injectable, signal, untracked } from '@angular/core'
import { finalize, Observable, of, shareReplay, tap } from 'rxjs'
import { AuthStore } from '../../../core/auth/store/auth.store'
import { Credit, CreditStatus, InterestRateTier, RateCatalog } from '../interfaces/credit.model'
import { CreditsService } from '../services/credits.service'

export type CreditFilter = CreditStatus | 'ALL'

/**
 * Estado de "Mis créditos" y del catálogo de tasas (signals).
 * Los créditos se cargan una vez y se filtran en el cliente.
 */
@Injectable({ providedIn: 'root' })
export class CreditsStore {
  private readonly service = inject(CreditsService)
  private readonly auth = inject(AuthStore)

  // Estado privado
  private readonly _credits = signal<Credit[]>([])
  private readonly _loading = signal(false)
  private readonly _loaded = signal(false)
  private readonly _filter = signal<CreditFilter>('ALL')
  private readonly _catalog = signal<RateCatalog | null>(null)
  private catalog$?: Observable<RateCatalog>

  // Estado público
  readonly credits = this._credits.asReadonly()
  readonly loading = this._loading.asReadonly()
  readonly loaded = this._loaded.asReadonly()
  readonly filter = this._filter.asReadonly()
  readonly catalog = this._catalog.asReadonly()

  readonly filtered = computed(() => {
    const filter = this._filter()
    const credits = this._credits()
    return filter === 'ALL' ? credits : credits.filter((c) => c.status === filter)
  })

  readonly stats = computed(() => {
    const credits = this._credits()
    const count = (s: CreditStatus) => credits.filter((c) => c.status === s).length
    const approved = credits.filter((c) => c.status === 'APPROVED')
    return {
      total: credits.length,
      pending: count('PENDING'),
      approved: approved.length,
      rejected: count('REJECTED'),
      cancelled: count('CANCELLED'),
      approvedAmount: approved.reduce((sum, c) => sum + c.amount, 0),
    }
  })

  constructor() {
    // Si cambia el usuario en sesión (logout / otro login), no mostrar créditos del anterior
    let previousUserId: number | undefined
    effect(() => {
      const userId = this.auth.user()?.id
      if (userId !== previousUserId) {
        previousUserId = userId
        untracked(() => this.reset())
      }
    })
  }

  setFilter(filter: CreditFilter): void {
    this._filter.set(filter)
  }

  load(): void {
    this._loading.set(true)
    this.service
      .myCredits()
      .pipe(finalize(() => this._loading.set(false)))
      .subscribe({
        next: (credits) => {
          this._credits.set(credits)
          this._loaded.set(true)
        },
        error: () => this._loaded.set(true),
      })
  }

  /** Inserta o reemplaza un crédito (tras solicitar o cancelar) sin recargar la lista. */
  upsert(credit: Credit): void {
    this._credits.update((list) => {
      const exists = list.some((c) => c.id === credit.id)
      return exists ? list.map((c) => (c.id === credit.id ? credit : c)) : [credit, ...list]
    })
  }

  /** Catálogo de tasas y límites; se pide una sola vez por sesión. */
  loadCatalog(): Observable<RateCatalog> {
    const cached = this._catalog()
    if (cached) return of(cached)
    this.catalog$ ??= this.service.rateCatalog().pipe(
      tap((catalog) => this._catalog.set(catalog)),
      shareReplay(1),
    )
    return this.catalog$
  }

  tierFor(termMonths: number | null | undefined): InterestRateTier | undefined {
    if (!termMonths) return undefined
    return this._catalog()?.tiers.find(
      (t) => termMonths >= t.minTermMonths && termMonths <= t.maxTermMonths,
    )
  }

  /** Descarta el catálogo cacheado (tras cambiar tramos de tasa). */
  invalidateCatalog(): void {
    this._catalog.set(null)
    this.catalog$ = undefined
  }

  /** Limpia el estado al cerrar sesión. */
  reset(): void {
    this._credits.set([])
    this._loaded.set(false)
    this._filter.set('ALL')
  }
}
