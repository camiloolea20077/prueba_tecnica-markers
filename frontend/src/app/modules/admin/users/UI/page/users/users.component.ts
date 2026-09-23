import { DatePipe } from '@angular/common'
import { HttpErrorResponse } from '@angular/common/http'
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms'
import { ConfirmationService, MessageService } from 'primeng/api'
import { AvatarModule } from 'primeng/avatar'
import { ButtonModule } from 'primeng/button'
import { IconFieldModule } from 'primeng/iconfield'
import { InputIconModule } from 'primeng/inputicon'
import { InputTextModule } from 'primeng/inputtext'
import { SelectModule } from 'primeng/select'
import { SelectButtonModule } from 'primeng/selectbutton'
import { TableLazyLoadEvent, TableModule } from 'primeng/table'
import { TagModule } from 'primeng/tag'
import { TooltipModule } from 'primeng/tooltip'
import { debounceTime, distinctUntilChanged } from 'rxjs'
import { RoleCode } from '../../../../../../core/auth/interfaces/auth.model'
import { AuthStore } from '../../../../../../core/auth/store/auth.store'
import { AdminUser } from '../../../interfaces/admin-user.model'
import { AdminUsersService } from '../../../services/admin-users.service'
import { AdminUsersStore } from '../../../store/admin-users.store'
import { UserFormDialogComponent } from '../../components/user-form-dialog/user-form-dialog.component'

type ActiveFilter = 'ALL' | 'ACTIVE' | 'INACTIVE'

/**
 * Administración de usuarios: búsqueda, filtros por rol y estado, alta, edición,
 * activar/desactivar y eliminación (solo sin créditos).
 */
@Component({
  selector: 'app-users',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    AvatarModule,
    ButtonModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    SelectModule,
    SelectButtonModule,
    TableModule,
    TagModule,
    TooltipModule,
    UserFormDialogComponent,
  ],
  templateUrl: './users.component.html',
})
export class UsersComponent implements OnInit {
  protected readonly store = inject(AdminUsersStore)
  private readonly service = inject(AdminUsersService)
  private readonly auth = inject(AuthStore)
  private readonly confirmation = inject(ConfirmationService)
  private readonly toast = inject(MessageService)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly currentUserId = computed(() => this.auth.user()?.id ?? null)
  protected readonly search = new FormControl(this.store.query().q, { nonNullable: true })
  protected readonly formVisible = signal(false)
  protected readonly editing = signal<AdminUser | null>(null)
  protected readonly busyId = signal<number | null>(null)

  protected readonly roleOptions = computed(() => [
    { label: 'Todos los roles', value: null },
    ...this.store.roles().map((r) => ({ label: r.name, value: r.code })),
  ])

  protected readonly activeOptions = [
    { label: 'Todos', value: 'ALL' },
    { label: 'Activos', value: 'ACTIVE' },
    { label: 'Inactivos', value: 'INACTIVE' },
  ]

  protected readonly activeFilter = computed<ActiveFilter>(() => {
    const active = this.store.query().active
    return active === null ? 'ALL' : active ? 'ACTIVE' : 'INACTIVE'
  })

  ngOnInit(): void {
    this.store.loadRoles()
    // La primera carga la dispara la tabla (onLazyLoad)
    this.search.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((text) => this.store.setSearch(text))
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    const size = event.rows ?? 10
    this.store.setPage(Math.floor((event.first ?? 0) / size), size)
  }

  protected onRole(role: RoleCode | null): void {
    this.store.setRole(role)
  }

  protected onActive(value: ActiveFilter | null): void {
    this.store.setActive(!value || value === 'ALL' ? null : value === 'ACTIVE')
  }

  protected initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0]!.toUpperCase())
      .join('')
  }

  protected isSelf(user: AdminUser): boolean {
    return user.id === this.currentUserId()
  }

  protected openNew(): void {
    this.editing.set(null)
    this.formVisible.set(true)
  }

  protected openEdit(user: AdminUser): void {
    this.editing.set(user)
    this.formVisible.set(true)
  }

  protected onSaved(user: AdminUser): void {
    this.toast.add({ severity: 'success', summary: 'Usuario guardado', detail: user.fullName })
    this.store.refresh()
  }

  protected toggleActive(user: AdminUser): void {
    const activate = !user.active
    this.confirmation.confirm({
      header: activate ? 'Activar usuario' : 'Desactivar usuario',
      message: activate
        ? `${user.fullName} podrá volver a iniciar sesión.`
        : `${user.fullName} no podrá iniciar sesión hasta que lo actives de nuevo.`,
      icon: activate ? 'pi pi-check-circle' : 'pi pi-ban',
      acceptLabel: activate ? 'Activar' : 'Desactivar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: activate ? 'contrast' : 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () => {
        this.busyId.set(user.id)
        this.service.changeStatus(user.id, activate).subscribe({
          next: (updated) => {
            this.busyId.set(null)
            this.toast.add({
              severity: 'success',
              summary: updated.active ? 'Usuario activado' : 'Usuario desactivado',
              detail: updated.fullName,
            })
            this.store.refresh()
          },
          error: (err: HttpErrorResponse) => this.fail(err, 'No se pudo cambiar el estado'),
        })
      },
    })
  }

  protected confirmDelete(user: AdminUser): void {
    this.confirmation.confirm({
      header: 'Eliminar usuario',
      message: `¿Eliminar a ${user.fullName}? Solo es posible si no tiene créditos registrados.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () => {
        this.busyId.set(user.id)
        this.service.delete(user.id).subscribe({
          next: () => {
            this.busyId.set(null)
            this.toast.add({
              severity: 'success',
              summary: 'Usuario eliminado',
              detail: user.fullName,
            })
            this.store.refresh()
          },
          error: (err: HttpErrorResponse) => this.fail(err, 'No se pudo eliminar'),
        })
      },
    })
  }

  private fail(err: HttpErrorResponse, summary: string): void {
    this.busyId.set(null)
    if (err.status === 0) return
    this.toast.add({ severity: 'warn', summary, detail: err.error?.message ?? 'Intenta de nuevo.' })
  }
}
