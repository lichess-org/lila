// TODO: Remove this when TypeScript 4.4.3 gets released
//       TS 4.4 introduced support for requestIdleCallback but 4.4.2 hangs rollup
declare global {
  interface Window {
    requestIdleCallback(callback: () => void, options?: { timeout: number }): void;
  }
}

export const requestIdleCallback = (f: () => void, timeout?: number) => {
  if (window.requestIdleCallback) window.requestIdleCallback(f, timeout ? { timeout } : undefined);
  else requestAnimationFrame(f);
};

export const escapeHtml = (str: string) =>
  /[&<>"']/.test(str)
    ? str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/'/g, '&#39;')
        .replace(/"/g, '&quot;')
    : str;
