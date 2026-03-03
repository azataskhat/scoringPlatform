/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: "#3B82F6",
        surface: "#111827",
        card: "#1F2937",
        border: "#374151",
      },
    },
  },
  plugins: [],
};
