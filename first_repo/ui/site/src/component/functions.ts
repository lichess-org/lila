export const requestIdleCallback = (f: () => void, timeout?: number) => {
  if (window.requestIdleCallback) window.requestIdleCallback(f, timeout && { timeout });
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
