import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The backend has no CORS configuration: in development the browser talks only to Vite,
// which forwards /api and /actuator to the Spring Boot app.
const backend = process.env.BACKEND_URL ?? "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": backend,
      "/actuator": backend,
    },
  },
});
