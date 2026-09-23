import { definePreset } from '@primeng/themes'
import Aura from '@primeng/themes/aura'

const ZINC = {
  50: '{zinc.50}',
  100: '{zinc.100}',
  200: '{zinc.200}',
  300: '{zinc.300}',
  400: '{zinc.400}',
  500: '{zinc.500}',
  600: '{zinc.600}',
  700: '{zinc.700}',
  800: '{zinc.800}',
  900: '{zinc.900}',
  950: '{zinc.950}',
}

/**
 * Tema corporativo en blanco y negro (sin degradados).
 * Primario = negro (zinc 950) con texto blanco; superficies en grises neutros.
 * Las clases `primary-*` y `surface-*` de Tailwind (tailwindcss-primeui) usan esta paleta.
 */
export const AppPreset = definePreset(Aura, {
  semantic: {
    primary: ZINC,
    colorScheme: {
      light: {
        surface: { 0: '#ffffff', ...ZINC },
        primary: {
          color: '{zinc.950}',
          contrastColor: '#ffffff',
          hoverColor: '{zinc.800}',
          activeColor: '{zinc.700}',
        },
        highlight: {
          background: '{zinc.950}',
          focusBackground: '{zinc.800}',
          color: '#ffffff',
          focusColor: '#ffffff',
        },
      },
    },
  },
})
