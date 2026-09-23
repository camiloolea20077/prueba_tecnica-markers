/** @type {import('tailwindcss').Config} */
const PrimeUI = require('tailwindcss-primeui')

module.exports = {
  content: ['./src/**/*.{html,ts}'],
  darkMode: ['selector', '.app-dark'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [PrimeUI],
}
