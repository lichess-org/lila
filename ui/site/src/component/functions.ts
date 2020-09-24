export const requestIdleCallback = (window.requestIdleCallback || window.setTimeout).bind(window);

export const escapeHtml = (str: string) =>
  /[&<>"']/.test(str) ?
    str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/'/g, '&#39;')
      .replace(/"/g, '&quot;') :
    str;
