/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        ink: "#1a1a1a",
        muted: "#8a8580",
        bg: "#faf8f5",
        border: "#e8e3dc",
        brand: {
          DEFAULT: "#1a1a1a",
          dark: "#000000",
        },
        band: {
          green: "#2e7d5b",
          amber: "#c98a2b",
          red: "#c0503f",
        },
      },
      fontFamily: {
        serif: ['Georgia', 'Times New Roman', 'serif'],
        sans: ['Inter', 'Helvetica Neue', 'Arial', 'sans-serif'],
        mono: ['Courier New', 'monospace'],
      },
    },
  },
  plugins: [],
};
