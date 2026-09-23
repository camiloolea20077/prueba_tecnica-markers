import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router'
import { MenuItem } from 'primeng/api'
import { AvatarModule } from 'primeng/avatar'
import { MenuModule } from 'primeng/menu'
import { TagModule } from 'primeng/tag'
import { TooltipModule } from 'primeng/tooltip'
import { filter, map, startWith } from 'rxjs'
import { AuthStore } from '../../auth/store/auth.store'
import { LayoutStore } from '../layout.store'

interface PageInfo {
  label: string
  section?: string
}

/**
 * Barra superior: botón de menú, título de la página actual y menú del usuario.
 */
@Component({
  selector: 'app-topbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AvatarModule, MenuModule, TagModule, TooltipModule],
  templateUrl: './topbar.component.html',
})
export class TopbarComponent {
  protected readonly layout = inject(LayoutStore)
  protected readonly store = inject(AuthStore)
  private readonly router = inject(Router)
  private readonly route = inject(ActivatedRoute)

  /** Título y sección tomados de `data.label` / `data.section` de la ruta activa. */
  protected readonly page = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      startWith(null),
      map(() => this.currentPage()),
    ),
    { initialValue: { label: '' } as PageInfo },
  )

  protected readonly initials = computed(() =>
    (this.store.user()?.fullName ?? '')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0]!.toUpperCase())
      .join(''),
  )

  protected readonly userMenu: MenuItem[] = [
    {
      label: 'Cerrar sesión',
      icon: 'pi pi-sign-out',
      command: () => this.layout.confirmLogout(),
    },
  ]

  private currentPage(): PageInfo {
    let snapshot = this.route.snapshot
    while (snapshot.firstChild) {
      snapshot = snapshot.firstChild
    }
    return {
      label: (snapshot.data['label'] as string) ?? '',
      section: snapshot.data['section'] as string | undefined,
    }
  }
}
