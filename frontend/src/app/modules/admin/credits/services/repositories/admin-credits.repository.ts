import { HttpClient, HttpParams } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'
import { Observable } from 'rxjs'
import { environment } from '../../../../../../environments/environment'
import { ApiResponse } from '../../../../../core/models/api-response.model'
import { PageResponse } from '../../../../../core/models/page.model'
import { Credit } from '../../../../credits/interfaces/credit.model'
import { AdminCreditQuery, CreditSummary } from '../../interfaces/admin-credit.model'

/**
 * Acceso HTTP a los endpoints de administración de créditos.
 */
@Injectable({ providedIn: 'root' })
export class AdminCreditsRepository {
  private readonly http = inject(HttpClient)
  private readonly baseUrl = `${environment.apiUrl}/admin/credits`

  search(query: AdminCreditQuery): Observable<ApiResponse<PageResponse<Credit>>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size)
    if (query.status) params = params.set('status', query.status)
    if (query.q.trim()) params = params.set('q', query.q.trim())
    return this.http.get<ApiResponse<PageResponse<Credit>>>(this.baseUrl, { params })
  }

  summary(): Observable<ApiResponse<CreditSummary>> {
    return this.http.get<ApiResponse<CreditSummary>>(`${this.baseUrl}/summary`)
  }

  approve(id: number, annualRate: number): Observable<ApiResponse<Credit>> {
    return this.http.patch<ApiResponse<Credit>>(`${this.baseUrl}/${id}/approve`, { annualRate })
  }

  reject(id: number, reason: string): Observable<ApiResponse<Credit>> {
    return this.http.patch<ApiResponse<Credit>>(`${this.baseUrl}/${id}/reject`, { reason })
  }
}
