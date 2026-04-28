/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: {
          50: '#f7faf8',
          100: '#edf3ef',
          200: '#d5e3da',
          300: '#bfd1c5',
          400: '#89a896',
          500: '#4a6957',
          600: '#385646',
          700: '#274236',
          800: '#1a3025',
          900: '#101f18',
        },
        moss: {
          300: '#78d4a1',
          500: '#16a34a',
          700: '#0f766e',
        },
        fog: {
          50: '#f8fcfa',
          100: '#eef6f2',
        },
      },
      fontFamily: {
        sans: ['"Space Grotesk"', '"Segoe UI"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', '"Consolas"', 'monospace'],
      },
    },
  },
  plugins: [],
}

