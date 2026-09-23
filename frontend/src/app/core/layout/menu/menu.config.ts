import { MenuSection } from './menu.model'

/**
 * Menú completo de la aplicación. `MenuService` lo filtra según rol y permisos.
 * Las restricciones deben coincidir con las de `app.routes.ts`.
 */
export const APP_MENU: MenuSection[] = [
  {
    label: 'General',
    items: [{ label: 'Inicio', icon: 'pi pi-home', route: '/inicio', exact: true }],
  },
  {
    label: 'Mis créditos',
    items: [
      {
        label: 'Mis créditos',
        icon: 'pi pi-wallet',
        route: '/creditos',
        exact: true,
        description: 'Consulta el estado de tus solicitudes.',
        permissions: ['CREDIT_VIEW_OWN'],
      },
      {
        label: 'Solicitar crédito',
        icon: 'pi pi-plus-circle',
        route: '/creditos/solicitar',
        description: 'Envía una nueva solicitud de crédito.',
        permissions: ['CREDIT_REQUEST'],
      },
    ],
  },
  {
    label: 'Herramientas',
    items: [
      {
        label: 'Simulador',
        icon: 'pi pi-calculator',
        route: '/simulador',
        description: 'Calcula la cuota con tasa efectiva anual.',
        permissions: ['CREDIT_SIMULATE'],
      },
    ],
  },
  {
    label: 'Administración',
    items: [
      {
        label: 'Solicitudes',
        icon: 'pi pi-inbox',
        route: '/admin/creditos',
        description: 'Aprueba o rechaza solicitudes de crédito.',
        roles: ['ADMIN'],
        permissions: ['CREDIT_VIEW_ALL'],
      },
      {
        label: 'Tasas de interés',
        icon: 'pi pi-percentage',
        route: '/admin/tasas',
        description: 'Administra los tramos de tasa EA.',
        roles: ['ADMIN'],
        permissions: ['RATE_MANAGE'],
      },
      {
        label: 'Usuarios',
        icon: 'pi pi-users',
        route: '/admin/usuarios',
        description: 'Gestiona usuarios y roles.',
        roles: ['ADMIN'],
        permissions: ['USER_MANAGE'],
      },
    ],
  },
]
