import { inject, Injectable } from '@angular/core'
import { map, Observable } from 'rxjs'
import { PageResponse } from '../../../../core/models/page.model'
import {
  AdminUser,
  CreateUserRequest,
  RoleOption,
  UpdateUserRequest,
  UserQuery,
} from '../interfaces/admin-user.model'
import { AdminUsersRepository } from './repositories/admin-users.repository'

/**
 * Casos de uso de administración de usuarios en el front.
 */
@Injectable({ providedIn: 'root' })
export class AdminUsersService {
  private readonly repository = inject(AdminUsersRepository)

  search(query: UserQuery): Observable<PageResponse<AdminUser>> {
    return this.repository.search(query).pipe(map((res) => res.data))
  }

  save(id: number | null, payload: CreateUserRequest | UpdateUserRequest): Observable<AdminUser> {
    const request$ = id
      ? this.repository.update(id, payload as UpdateUserRequest)
      : this.repository.create(payload as CreateUserRequest)
    return request$.pipe(map((res) => res.data))
  }

  changeStatus(id: number, active: boolean): Observable<AdminUser> {
    return this.repository.changeStatus(id, active).pipe(map((res) => res.data))
  }

  delete(id: number): Observable<void> {
    return this.repository.delete(id).pipe(map(() => undefined))
  }

  roles(): Observable<RoleOption[]> {
    return this.repository.roles().pipe(map((res) => res.data))
  }
}
