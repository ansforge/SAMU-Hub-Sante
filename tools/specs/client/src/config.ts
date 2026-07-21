declare global {
  interface Window {
    __ENV__?: { VITE_SPECS_API_DOMAIN?: string };
  }
}

export const apiDomain =
  window.__ENV__?.VITE_SPECS_API_DOMAIN || import.meta.env.VITE_SPECS_API_DOMAIN;
