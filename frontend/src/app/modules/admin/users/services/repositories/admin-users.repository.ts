import { HttpClient, HttpParams } from '@angular/common/http'
import { inject, Injectable } from '@angular/core'
import { Observable } from 'rxjs'
import { environment } from '../../../../../../environments/environment'
import { ApiResponse } from '../../../../../core/models/api-response.model'
import { PageResponse } from '../../../../../core/models/page.model'
import {
  AdminUser,
  CreateUserRequest,
  RoleOption,
  UpdateUserRequest,
  UserQuery,
} from '../../interfaces/admin-user.model'

/**
 * Acceso HTTP al CRUD de usuarios y a los roles.
 */
@Injectable({ providedIn: 'root' })
export class AdminUsersRepository {
  private readonly http = inject(HttpClient)
  private readonly baseUrl = `${environment.apiUrl}/admin`

  search(query: UserQuery): Observable<ApiResponse<PageResponse<AdminUser>>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size)
    if (query.q.trim()) params = params.set('q', query.q.trim())
    if (query.role) params = params.set('role', query.role)
    if (query.active !== null) params = params.set('active', query.active)
    return this.http.get<ApiResponse<PageResponse<AdminUser>>>(`${this.baseUrl}/users`, { params })
  }

  create(payload: CreateUserRequest): Observable<ApiResponse<AdminUser>> {
    return this.http.post<ApiResponse<AdminUser>>(`${this.baseUrl}/users`, payload)
  }

  update(id: number, payload: UpdateUserRequest): Observable<ApiResponse<AdminUser>> {
    return this.http.put<ApiResponse<AdminUser>>(`${this.baseUrl}/users/${id}`, payload)
  }

  changeStatus(id: number, active: boolean): Observable<ApiResponse<AdminUser>> {
    return this.http.patch<ApiResponse<AdminUser>>(`${this.baseUrl}/users/${id}/status`, { active })
  }

  delete(id: number): Observable<ApiResponse<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/users/${id}`)
  }

  roles(): Observable<ApiResponse<RoleOption[]>> {
    return this.http.get<ApiResponse<RoleOption[]>>(`${this.baseUrl}/roles`)
  }
}
