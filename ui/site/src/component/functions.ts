import { storage } from './storage';
import { loadCssPath } from './assets';

export const requestIdleCallback = (window.requestIdleCallback || window.setTimeout).bind(window);

export const dispatchEvent = (el: HTMLElement, eventName: string) => el.dispatchEvent(new Event(eventName));

export const once = (key: string, mod: 'always' | undefined) => {
  if (mod === 'always') return true;
  if (!storage.get(key)) {
    storage.set(key, '1');
    return true;
  }
  return false;
};

export const debounce = (func, wait, immediate) => {
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

export const makeChat = data =>
  new Promise(resolve =>
    requestAnimationFrame(() => {
      data.loadCss = loadCssPath;
      resolve(window.LichessChat(document.querySelector('.mchat'), data));
    })
  );

export const formAjax = $form => ({
  url: $form.attr('action'),
  method: $form.attr('method') || 'post',
  data: $form.serialize()
});
