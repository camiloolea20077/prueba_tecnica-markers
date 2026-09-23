import { inject, Injectable } from '@angular/core'
import { map, Observable } from 'rxjs'
import { PageResponse } from '../../../../core/models/page.model'
import { Credit } from '../../../credits/interfaces/credit.model'
import { AdminCreditQuery, CreditSummary } from '../interfaces/admin-credit.model'
import { AdminCreditsRepository } from './repositories/admin-credits.repository'

/**
 * Casos de uso del analista en el front: consultar, aprobar y rechazar.
 */
@Injectable({ providedIn: 'root' })
export class AdminCreditsService {
  private readonly repository = inject(AdminCreditsRepository)

  search(query: AdminCreditQuery): Observable<PageResponse<Credit>> {
    return this.repository.search(query).pipe(map((res) => res.data))
  }

  summary(): Observable<CreditSummary> {
    return this.repository.summary().pipe(map((res) => res.data))
  }

  approve(id: number, annualRate: number): Observable<Credit> {
    return this.repository.approve(id, annualRate).pipe(map((res) => res.data))
  }

  reject(id: number, reason: string): Observable<Credit> {
    return this.repository.reject(id, reason).pipe(map((res) => res.data))
  }
}
