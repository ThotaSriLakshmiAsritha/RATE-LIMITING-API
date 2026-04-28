/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#1D55F4',
          foreground: '#FFFFFF',
        },
        secondary: {
          DEFAULT: '#668EFE',
          foreground: '#FFFFFF',
        },
        background: '#F5F7FA',
        card: {
          DEFAULT: '#FFFFFF',
          foreground: '#0C0D0D',
        },
        muted: {
          DEFAULT: '#F1F5F9',
          foreground: '#A2A7B2',
        },
        accent: {
          DEFAULT: '#F5F7FA',
          foreground: '#1D55F4',
        },
        destructive: {
          DEFAULT: '#EF4444',
          foreground: '#FFFFFF',
        },
        success: {
          DEFAULT: '#22C55E',
          foreground: '#FFFFFF',
        },
        warning: {
          DEFAULT: '#F59E0B',
          foreground: '#FFFFFF',
        },
        border: '#E2E8F0',
        ink: {
          100: '#F3F4F6',
          200: '#E5E7EB',
          300: '#D1D5DB',
          600: '#4B5563',
          700: '#374151',
          800: '#1F2937',
          900: '#111827',
        },
        moss: {
          500: '#10B981',
          700: '#047857',
        },
      },
      borderRadius: {
        lg: '16px',
        xl: '20px',
        '2xl': '24px',
      },
      fontFamily: {
        sans: ['Inter', '"SF Pro Display"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      boxShadow: {
        soft: '0 4px 20px -4px rgba(0, 0, 0, 0.05)',
        'card-hover': '0 10px 40px -10px rgba(29, 85, 244, 0.1)',
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.4s ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
      },
    },
  },
  plugins: [],
}

