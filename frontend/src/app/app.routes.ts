import { Routes } from '@angular/router'
import { authGuard } from './core/auth/guards/auth.guard'
import { guestGuard } from './core/auth/guards/guest.guard'
import { roleGuard } from './core/auth/guards/role.guard'

const comingSoon = () =>
  import('./shared/pages/coming-soon/coming-soon.component').then((m) => m.ComingSoonComponent)

/**
 * Las restricciones de rol/permisos deben coincidir con `core/layout/menu/menu.config.ts`.
 */
export const routes: Routes = [
  {
    path: 'login',
    title: 'Iniciar sesión · Data Credits',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./modules/auth/UI/page/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    loadComponent: () =>
      import('./core/layout/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        title: 'Inicio · Data Credits',
        data: { label: 'Inicio', section: 'General' },
        loadComponent: () =>
          import('./modules/home/UI/page/home/home.component').then((m) => m.HomeComponent),
      },
      {
        path: 'creditos',
        title: 'Mis créditos · Data Credits',
        canActivate: [roleGuard],
        data: {
          label: 'Mis créditos',
          section: 'Mis créditos',
          icon: 'pi pi-wallet',
          permissions: ['CREDIT_VIEW_OWN'],
        },
        loadComponent: () =>
          import('./modules/credits/UI/page/my-credits/my-credits.component').then(
            (m) => m.MyCreditsComponent,
          ),
      },
      {
        path: 'creditos/solicitar',
        title: 'Solicitar crédito · Data Credits',
        canActivate: [roleGuard],
        data: {
          label: 'Solicitar crédito',
          section: 'Mis créditos',
          icon: 'pi pi-plus-circle',
          permissions: ['CREDIT_REQUEST'],
        },
        loadComponent: () =>
          import('./modules/credits/UI/page/request-credit/request-credit.component').then(
            (m) => m.RequestCreditComponent,
          ),
      },
      {
        path: 'simulador',
        title: 'Simulador · Data Credits',
        canActivate: [roleGuard],
        data: {
          label: 'Simulador de crédito',
          section: 'Herramientas',
          icon: 'pi pi-calculator',
          permissions: ['CREDIT_SIMULATE'],
        },
        loadComponent: () =>
          import('./modules/credits/UI/page/simulator/simulator.component').then(
            (m) => m.SimulatorComponent,
          ),
      },
      {
        path: 'admin',
        canActivateChild: [roleGuard],
        data: { roles: ['ADMIN'] },
        children: [
          {
            path: 'creditos',
            title: 'Solicitudes · Data Credits',
            data: {
              label: 'Solicitudes de crédito',
              section: 'Administración',
              icon: 'pi pi-inbox',
              roles: ['ADMIN'],
              permissions: ['CREDIT_VIEW_ALL'],
            },
            loadComponent: comingSoon,
          },
          {
            path: 'tasas',
            title: 'Tasas de interés · Data Credits',
            data: {
              label: 'Tasas de interés',
              section: 'Administración',
              icon: 'pi pi-percentage',
              roles: ['ADMIN'],
              permissions: ['RATE_MANAGE'],
            },
            loadComponent: comingSoon,
          },
          {
            path: 'usuarios',
            title: 'Usuarios · Data Credits',
            data: {
              label: 'Usuarios',
              section: 'Administración',
              icon: 'pi pi-users',
              roles: ['ADMIN'],
              permissions: ['USER_MANAGE'],
            },
            loadComponent: comingSoon,
          },
        ],
      },
    ],
  },
  { path: '**', redirectTo: 'inicio' },
]
