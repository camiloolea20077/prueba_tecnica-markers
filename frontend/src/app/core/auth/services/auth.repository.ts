import { HttpClient } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'
import { Observable } from 'rxjs'
import { environment } from '../../../../environments/environment'
import { ApiResponse } from '../../models/api-response.model'
import { AuthResponse, AuthUser, LoginRequest } from '../interfaces/auth.model'

/**
 * Acceso HTTP a los endpoints de autenticación.
 */
@Injectable({ providedIn: 'root' })
export class AuthRepository {
  private readonly http = inject(HttpClient)
  private readonly baseUrl = `${environment.apiUrl}/auth`

  login(payload: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.baseUrl}/login`, payload)
  }

  me(): Observable<ApiResponse<AuthUser>> {
    return this.http.get<ApiResponse<AuthUser>>(`${this.baseUrl}/me`)
  }
}
