export interface RuntimeEnv {
  API_BASE_URL?: string;
}

declare global {
  interface Window {
    __env?: RuntimeEnv;
  }
}

export function getApiBaseUrl(): string {
  const configured = window.__env?.API_BASE_URL || 'http://localhost:8080/api';
  return configured.replace(/\/+$/, '');
}
