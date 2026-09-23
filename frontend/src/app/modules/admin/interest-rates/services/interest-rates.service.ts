import { inject, Injectable } from '@angular/core'
import { map, Observable, tap } from 'rxjs'
import { CreditsStore } from '../../../credits/store/credits.store'
import {
  AdminInterestRateTier,
  InterestRateTierRequest,
} from '../interfaces/interest-rate-tier.model'
import { InterestRatesRepository } from './repositories/interest-rates.repository'

/**
 * CRUD de tramos. Cada cambio invalida el catálogo cacheado de créditos
 * para que simulador y solicitud usen las tasas nuevas.
 */
@Injectable({ providedIn: 'root' })
export class InterestRatesService {
  private readonly repository = inject(InterestRatesRepository)
  private readonly creditsStore = inject(CreditsStore)

  findAll(): Observable<AdminInterestRateTier[]> {
    return this.repository.findAll().pipe(map((res) => res.data))
  }

  save(id: number | null, payload: InterestRateTierRequest): Observable<AdminInterestRateTier> {
    const request$ = id ? this.repository.update(id, payload) : this.repository.create(payload)
    return request$.pipe(
      map((res) => res.data),
      tap(() => this.creditsStore.invalidateCatalog()),
    )
  }

  delete(id: number): Observable<void> {
    return this.repository.delete(id).pipe(
      map(() => undefined),
      tap(() => this.creditsStore.invalidateCatalog()),
    )
  }
}
