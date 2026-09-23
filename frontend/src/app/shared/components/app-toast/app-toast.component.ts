import { ChangeDetectionStrategy, Component } from '@angular/core'
import { ToastMessageOptions } from 'primeng/api'
import { ToastModule } from 'primeng/toast'

interface SeverityStyle {
  icon: string
  iconClass: string
  label: string
}

const DEFAULT_LIFE = 4500

const SEVERITY: Record<string, SeverityStyle> = {
  success: {
    icon: 'pi pi-check',
    iconClass: 'bg-emerald-400/15 text-emerald-300 ring-emerald-400/30',
    label: 'Éxito',
  },
  error: {
    icon: 'pi pi-times',
    iconClass: 'bg-red-400/15 text-red-300 ring-red-400/30',
    label: 'Error',
  },
  warn: {
    icon: 'pi pi-exclamation-triangle',
    iconClass: 'bg-amber-400/15 text-amber-300 ring-amber-400/30',
    label: 'Aviso',
  },
  info: {
    icon: 'pi pi-info',
    iconClass: 'bg-white/10 text-white ring-white/20',
    label: 'Información',
  },
  secondary: {
    icon: 'pi pi-bell',
    iconClass: 'bg-white/10 text-white ring-white/20',
    label: 'Aviso',
  },
  contrast: {
    icon: 'pi pi-bell',
    iconClass: 'bg-white/10 text-white ring-white/20',
    label: 'Aviso',
  },
}

/**
 * Toast global con diseño propio (blanco y negro).
 * Se usa igual que siempre: `MessageService.add({ severity, summary, detail, life? })`.
 */
@Component({
  selector: 'app-toast',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ToastModule],
  templateUrl: './app-toast.component.html',
})
export class AppToastComponent {
  protected readonly defaultLife = DEFAULT_LIFE

  protected style(message: ToastMessageOptions): SeverityStyle {
    return SEVERITY[message.severity ?? 'info'] ?? SEVERITY['info']
  }
}
