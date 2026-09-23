import { HttpClient } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'
import { Observable } from 'rxjs'
import { environment } from '../../../../../../environments/environment'
import { ApiResponse } from '../../../../../core/models/api-response.model'
import {
  AdminInterestRateTier,
  InterestRateTierRequest,
} from '../../interfaces/interest-rate-tier.model'

/**
 * Acceso HTTP al CRUD de tramos de tasa.
 */
@Injectable({ providedIn: 'root' })
export class InterestRatesRepository {
  private readonly http = inject(HttpClient)
  private readonly baseUrl = `${environment.apiUrl}/admin/interest-rates`

  findAll(): Observable<ApiResponse<AdminInterestRateTier[]>> {
    return this.http.get<ApiResponse<AdminInterestRateTier[]>>(this.baseUrl)
  }

  create(payload: InterestRateTierRequest): Observable<ApiResponse<AdminInterestRateTier>> {
    return this.http.post<ApiResponse<AdminInterestRateTier>>(this.baseUrl, payload)
  }

  update(
    id: number,
    payload: InterestRateTierRequest,
  ): Observable<ApiResponse<AdminInterestRateTier>> {
    return this.http.put<ApiResponse<AdminInterestRateTier>>(`${this.baseUrl}/${id}`, payload)
  }

  delete(id: number): Observable<ApiResponse<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${id}`)
  }
}
