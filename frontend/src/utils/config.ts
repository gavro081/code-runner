// base URL for backend API calls.
// empty by default -> relative "/api/..." which is proxied to the gateway by
// the Vite dev server proxy (npm run dev), and nginx in the production container.
export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? "";
