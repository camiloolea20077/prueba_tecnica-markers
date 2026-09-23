import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

/**
 * Logo de Data Credits: isotipo + nombre.
 * `tone="light"` para fondos oscuros, `tone="dark"` para fondos claros.
 */
@Component({
  selector: 'app-brand-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './brand-logo.component.html',
})
export class BrandLogoComponent {
  readonly tone = input<'light' | 'dark'>('dark')
  readonly size = input(44)
  readonly stacked = input(false)
  readonly showTagline = input(true)
  /** false = solo el isotipo (barra lateral colapsada). */
  readonly showName = input(true)

  // Blanco y negro: sobre fondo oscuro isotipo blanco con trazo negro, y al revés.
  protected readonly badgeFill = computed(() => (this.tone() === 'light' ? '#ffffff' : '#09090b'))
  protected readonly strokeColor = computed(() => (this.tone() === 'light' ? '#09090b' : '#ffffff'))
  protected readonly nameClass = computed(() =>
    this.tone() === 'light' ? 'text-white' : 'text-surface-950',
  )
  protected readonly taglineClass = computed(() =>
    this.tone() === 'light' ? 'text-surface-400' : 'text-surface-500',
  )
}
