import { inject, Injectable } from '@angular/core'
import { map, Observable } from 'rxjs'
import {
  CreateCreditRequest,
  Credit,
  CreditQuote,
  RateCatalog,
  SimulateCreditRequest,
} from '../interfaces/credit.model'
import { CreditsRepository } from './repositories/credits.repository'

/**
 * Casos de uso de créditos del front: desenvuelve `ApiResponse` y expone solo los datos.
 */
@Injectable({ providedIn: 'root' })
export class CreditsService {
  private readonly repository = inject(CreditsRepository)

  myCredits(): Observable<Credit[]> {
    return this.repository.myCredits().pipe(map((res) => res.data))
  }

  request(payload: CreateCreditRequest): Observable<Credit> {
    return this.repository.request(payload).pipe(map((res) => res.data))
  }

  simulate(payload: SimulateCreditRequest): Observable<CreditQuote> {
    return this.repository.simulate(payload).pipe(map((res) => res.data))
  }

  cancel(id: number): Observable<Credit> {
    return this.repository.cancel(id).pipe(map((res) => res.data))
  }

  rateCatalog(): Observable<RateCatalog> {
    return this.repository.rateCatalog().pipe(map((res) => res.data))
  }
}
