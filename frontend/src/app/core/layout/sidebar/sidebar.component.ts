import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { RouterLink, RouterLinkActive } from '@angular/router'
import { AvatarModule } from 'primeng/avatar'
import { TooltipModule } from 'primeng/tooltip'
import { BrandLogoComponent } from '../../../shared/components/brand-logo/brand-logo.component'
import { AuthStore } from '../../auth/store/auth.store'
import { LayoutStore } from '../layout.store'
import { MenuService } from '../menu/menu.service'

/**
 * Barra lateral izquierda: logo, menú filtrado por permisos y tarjeta del usuario.
 * Escritorio: fija y colapsable a íconos. Móvil: panel deslizable.
 */
@Component({
  selector: 'app-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, AvatarModule, TooltipModule, BrandLogoComponent],
  templateUrl: './sidebar.component.html',
  // El <aside> debe ser el hijo flex del shell para estirarse a toda la altura
  host: { class: 'contents' },
})
export class SidebarComponent {
  protected readonly layout = inject(LayoutStore)
  protected readonly menu = inject(MenuService)
  protected readonly store = inject(AuthStore)

  /** En móvil el panel siempre se muestra expandido. */
  protected readonly compact = computed(() => this.layout.collapsed() && !this.layout.mobileOpen())

  protected readonly initials = computed(() => {
    const name = this.store.user()?.fullName ?? ''
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0]!.toUpperCase())
      .join('')
  })
}
