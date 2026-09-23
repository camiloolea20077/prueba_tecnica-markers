import { HttpClient, HttpParams } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'
import { Observable } from 'rxjs'
import { environment } from '../../../../../environments/environment'
import { ApiResponse } from '../../../../core/models/api-response.model'
import {
  CreateCreditRequest,
  Credit,
  CreditQuote,
  CreditStatus,
  RateCatalog,
  SimulateCreditRequest,
} from '../../interfaces/credit.model'

/**
 * Acceso HTTP a los endpoints de créditos del solicitante.
 */
@Injectable({ providedIn: 'root' })
export class CreditsRepository {
  private readonly http = inject(HttpClient)
  private readonly baseUrl = `${environment.apiUrl}/credits`

  myCredits(status?: CreditStatus): Observable<ApiResponse<Credit[]>> {
    const params = status ? new HttpParams().set('status', status) : undefined
    return this.http.get<ApiResponse<Credit[]>>(`${this.baseUrl}/me`, { params })
  }

  findById(id: number): Observable<ApiResponse<Credit>> {
    return this.http.get<ApiResponse<Credit>>(`${this.baseUrl}/${id}`)
  }

  request(payload: CreateCreditRequest): Observable<ApiResponse<Credit>> {
    return this.http.post<ApiResponse<Credit>>(this.baseUrl, payload)
  }

  simulate(payload: SimulateCreditRequest): Observable<ApiResponse<CreditQuote>> {
    return this.http.post<ApiResponse<CreditQuote>>(`${this.baseUrl}/simulate`, payload)
  }

  cancel(id: number): Observable<ApiResponse<Credit>> {
    return this.http.patch<ApiResponse<Credit>>(`${this.baseUrl}/${id}/cancel`, {})
  }

  rateCatalog(): Observable<ApiResponse<RateCatalog>> {
    return this.http.get<ApiResponse<RateCatalog>>(`${environment.apiUrl}/interest-rates`)
  }
}
