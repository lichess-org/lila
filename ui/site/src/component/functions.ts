export const requestIdleCallback = (window.requestIdleCallback || window.setTimeout).bind(window);

type Debounced = (...args) => any;

export const debounce = (func: (...args) => any, wait: number, immediate = false): Debounced => {
  let timeout, lastBounce = 0;
  return function(this: any) {
    let context = this,
      args = arguments,
      elapsed = performance.now() - lastBounce;
    lastBounce = performance.now();
    let later = () => {
      timeout = null;
      func.apply(context, args);
    };
    clearTimeout(timeout);
    if (immediate && elapsed > wait) func.apply(context, args);
    else timeout = setTimeout(later, wait);
  };
};

let numberFormatter: false | Intl.NumberFormat | null = false;
export const numberFormat = n => {
  if (numberFormatter === false) numberFormatter = (window.Intl && Intl.NumberFormat) ? new Intl.NumberFormat() : null;
  if (numberFormatter === null) return '' + n;
  return numberFormatter.format(n);
};

export const escapeHtml = str =>
  /[&<>"']/.test(str) ?
    str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/'/g, '&#39;')
      .replace(/"/g, '&quot;') :
    str;

export const formAjax = $form => ({
  url: $form.attr('action'),
  method: $form.attr('method') || 'post',
  data: $form.serialize()
});
