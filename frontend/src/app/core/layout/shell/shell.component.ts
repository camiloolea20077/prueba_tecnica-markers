import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { RouterOutlet } from '@angular/router'
import { LayoutStore } from '../layout.store'
import { SidebarComponent } from '../sidebar/sidebar.component'
import { TopbarComponent } from '../topbar/topbar.component'

/**
 * Contenedor de las páginas autenticadas: barra lateral + barra superior + contenido.
 */
@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent],
  templateUrl: './shell.component.html',
})
export class ShellComponent {
  protected readonly layout = inject(LayoutStore)
}
