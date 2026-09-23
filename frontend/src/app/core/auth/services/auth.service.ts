import { inject, Injectable } from '@angular/core'
import { Router } from '@angular/router'
import { map, Observable, tap } from 'rxjs'
import { AuthUser, LoginRequest } from '../interfaces/auth.model'
import { AuthStore } from '../store/auth.store'
import { AuthRepository } from './auth.repository'

/**
 * Casos de uso de autenticación del front: login, refresco del usuario y logout.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly repository = inject(AuthRepository)
  private readonly store = inject(AuthStore)
  private readonly router = inject(Router)

  login(credentials: LoginRequest, remember: boolean): Observable<AuthUser> {
    const payload: LoginRequest = {
      email: credentials.email.trim(),
      password: credentials.password,
    }
    return this.repository.login(payload).pipe(
      tap((res) => this.store.setSession(res.data, remember)),
      map((res) => res.data.user),
    )
  }

  refreshUser(): Observable<AuthUser> {
    return this.repository.me().pipe(
      map((res) => res.data),
      tap((user) => this.store.updateUser(user)),
    )
  }

  logout(returnUrl?: string): void {
    this.store.clear()
    void this.router.navigate(['/login'], {
      queryParams: returnUrl ? { returnUrl } : undefined,
    })
  }

  /** Ruta de inicio según el rol del usuario. */
  homeUrl(): string {
    return '/inicio'
  }
}
