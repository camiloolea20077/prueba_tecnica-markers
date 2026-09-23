import { computed, inject, Injectable } from '@angular/core'
import { AuthStore } from '../../auth/store/auth.store'
import { APP_MENU } from './menu.config'
import { MenuItem, MenuSection } from './menu.model'

/**
 * Menú visible para el usuario en sesión (se recalcula si cambia el usuario).
 */
@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly store = inject(AuthStore)

  readonly sections = computed<MenuSection[]>(() => {
    // Dependencia explícita: recalcular cuando cambie el usuario
    this.store.user()
    return APP_MENU.map((section) => ({
      ...section,
      items: section.items.filter((item) => this.canSee(item)),
    })).filter((section) => section.items.length > 0)
  })

  /** Accesos rápidos del inicio: todo lo visible salvo el propio inicio. */
  readonly shortcuts = computed<MenuItem[]>(() =>
    this.sections()
      .flatMap((s) => s.items)
      .filter((item) => !!item.description),
  )

  private canSee(item: MenuItem): boolean {
    const roleOk = !item.roles?.length || this.store.hasRole(...item.roles)
    const permsOk = !item.permissions?.length || this.store.hasPermission(...item.permissions)
    return roleOk && permsOk
  }
}
