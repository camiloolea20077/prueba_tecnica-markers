import { inject, Injectable, signal } from '@angular/core'
import { ConfirmationService } from 'primeng/api'
import { AuthService } from '../auth/services/auth.service'

const COLLAPSED_KEY = 'dc_sidebar_collapsed'

/**
 * Estado visual del layout: barra lateral colapsada (escritorio) y abierta (móvil).
 */
@Injectable({ providedIn: 'root' })
export class LayoutStore {
  private readonly confirmation = inject(ConfirmationService)
  private readonly auth = inject(AuthService)

  private readonly _collapsed = signal(this.readCollapsed())
  private readonly _mobileOpen = signal(false)

  readonly collapsed = this._collapsed.asReadonly()
  readonly mobileOpen = this._mobileOpen.asReadonly()

  toggleCollapsed(): void {
    const next = !this._collapsed()
    this._collapsed.set(next)
    try {
      localStorage.setItem(COLLAPSED_KEY, String(next))
    } catch {
      /* almacenamiento no disponible: solo se pierde la preferencia */
    }
  }

  toggleMobile(): void {
    this._mobileOpen.update((open) => !open)
  }

  closeMobile(): void {
    this._mobileOpen.set(false)
  }

  /** Pide confirmación antes de cerrar la sesión. */
  confirmLogout(): void {
    this.confirmation.confirm({
      header: 'Cerrar sesión',
      message: '¿Deseas salir de Data Credits?',
      icon: 'pi pi-sign-out',
      acceptLabel: 'Sí, salir',
      rejectLabel: 'Cancelar',
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () => {
        this.closeMobile()
        this.auth.logout()
      },
    })
  }

  private readCollapsed(): boolean {
    try {
      return localStorage.getItem(COLLAPSED_KEY) === 'true'
    } catch {
      return false
    }
  }
}
