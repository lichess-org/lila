export const requestIdleCallback = (window.requestIdleCallback || window.setTimeout).bind(window);

let numberFormatter: false | Intl.NumberFormat | null = false;
export const numberFormat = n => {
  if (numberFormatter === false) numberFormatter = (window.Intl && Intl.NumberFormat) ? new Intl.NumberFormat() : null;
  if (numberFormatter === null) return '' + n;
  return numberFormatter.format(n);
};

export const escapeHtml = (str: string) =>
  /[&<>"']/.test(str) ?
    str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/'/g, '&#39;')
      .replace(/"/g, '&quot;') :
    str;
