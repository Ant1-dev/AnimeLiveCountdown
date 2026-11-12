import { inject, RESPONSE_INIT } from '@angular/core';

/**
 * Sets the HTTP status of the server-rendered response. No-op in the browser.
 * Call in an injection context and keep the returned function for later use.
 */
export function injectResponseStatus(): (status: number, headers?: Record<string, string>) => void {
  const init = inject(RESPONSE_INIT, { optional: true });
  return (status, headers) => {
    if (!init) return;
    init.status = status;
    if (headers) {
      const merged = new Headers(init.headers);
      for (const [key, value] of Object.entries(headers)) merged.set(key, value);
      init.headers = merged;
    }
  };
}
