import { computed, inject, Injectable, signal } from '@angular/core'
import { finalize, Subscription } from 'rxjs'
import { RoleCode } from '../../../../core/auth/interfaces/auth.model'
import { PageResponse } from '../../../../core/models/page.model'
import { AdminUser, RoleOption, UserQuery } from '../interfaces/admin-user.model'
import { AdminUsersService } from '../services/admin-users.service'

/**
 * Estado de la administración de usuarios: filtros y página (resueltos en el servidor) y roles.
 */
@Injectable({ providedIn: 'root' })
export class AdminUsersStore {
  private readonly service = inject(AdminUsersService)
  private searchSub?: Subscription

  // Estado privado
  private readonly _query = signal<UserQuery>({
    q: '',
    role: null,
    active: null,
    page: 0,
    size: 10,
  })
  private readonly _page = signal<PageResponse<AdminUser> | null>(null)
  private readonly _roles = signal<RoleOption[]>([])
  private readonly _loading = signal(false)

  // Estado público
  readonly query = this._query.asReadonly()
  readonly roles = this._roles.asReadonly()
  readonly loading = this._loading.asReadonly()
  readonly users = computed(() => this._page()?.content ?? [])
  readonly totalRecords = computed(() => this._page()?.totalElements ?? 0)
  readonly first = computed(() => this._query().page * this._query().size)

  setSearch(q: string): void {
    this.update({ q, page: 0 })
  }

  setRole(role: RoleCode | null): void {
    this.update({ role, page: 0 })
  }

  setActive(active: boolean | null): void {
    this.update({ active, page: 0 })
  }

  setPage(page: number, size: number): void {
    const current = this._query()
    if (current.page === page && current.size === size && this._page()) return
    this.update({ page, size })
  }

  refresh(): void {
    this.load()
  }

  loadRoles(): void {
    if (this._roles().length) return
    this.service.roles().subscribe((roles) => this._roles.set(roles))
  }

  private update(changes: Partial<UserQuery>): void {
    this._query.update((q) => ({ ...q, ...changes }))
    this.load()
  }

  private load(): void {
    this.searchSub?.unsubscribe()
    this._loading.set(true)
    this.searchSub = this.service
      .search(this._query())
      .pipe(finalize(() => this._loading.set(false)))
      .subscribe((page) => {
        if (page.content.length === 0 && page.page > 0) {
          this._query.update((q) => ({ ...q, page: q.page - 1 }))
          this.load()
          return
        }
        this._page.set(page)
      })
  }
}
