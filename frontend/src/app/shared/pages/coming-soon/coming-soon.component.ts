import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import { ButtonModule } from 'primeng/button'

/**
 * Marcador para secciones del menú que se construyen en módulos posteriores.
 * Recibe `label` e `icon` desde `data` de la ruta (withComponentInputBinding).
 */
@Component({
  selector: 'app-coming-soon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonModule],
  templateUrl: './coming-soon.component.html',
})
export class ComingSoonComponent {
  readonly label = input<string>('Próximamente')
  readonly icon = input<string>()
}
