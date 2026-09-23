import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { RouterLink } from '@angular/router'
import { TagModule } from 'primeng/tag'
import { PERMISSION_LABELS } from '../../../../../core/auth/constants/permission-labels'
import { AuthStore } from '../../../../../core/auth/store/auth.store'
import { MenuService } from '../../../../../core/layout/menu/menu.service'

/**
 * Tablero de inicio: saludo, accesos rápidos según permisos y resumen de acceso.
 */
@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, TagModule],
  templateUrl: './home.component.html',
})
export class HomeComponent {
  protected readonly store = inject(AuthStore)
  protected readonly menu = inject(MenuService)

  protected readonly greeting = computed(() => {
    const hour = new Date().getHours()
    if (hour < 12) return 'Buenos días'
    if (hour < 19) return 'Buenas tardes'
    return 'Buenas noches'
  })

  protected readonly firstName = computed(() => this.store.user()?.fullName.split(' ')[0] ?? '')

  protected readonly permissionLabels = computed(() =>
    (this.store.user()?.permissions ?? []).map((p) => PERMISSION_LABELS[p] ?? p),
  )
}
