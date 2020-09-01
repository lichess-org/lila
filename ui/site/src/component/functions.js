lichess.requestIdleCallback = (window.requestIdleCallback || window.setTimeout).bind(window);
lichess.dispatchEvent = (el, eventName) => el.dispatchEvent(new Event(eventName));

lichess.once = (key, mod) => {
  if (mod === 'always') return true;
  if (!lichess.storage.get(key)) {
    lichess.storage.set(key, 1);
    return true;
  }
  return false;
};

lichess.debounce = (func, wait, immediate) => {
  let timeout, lastBounce = 0;
  return function() {
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

{
  let formatter = false;
  lichess.numberFormat = n => {
    if (formatter === false) formatter = (window.Intl && Intl.NumberFormat) ? new Intl.NumberFormat() : null;
    if (formatter === null) return n;
    return formatter.format(n);
  };
}

lichess.escapeHtml = str =>
  /[&<>"']/.test(str) ?
  str
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/'/g, '&#39;')
  .replace(/"/g, '&quot;') :
  str;

lichess.makeChat = data =>
  new Promise(resolve =>
    requestAnimationFrame(() => {
      data.loadCss = lichess.loadCssPath;
      resolve(LichessChat(document.querySelector('.mchat'), data));
    })
  );

lichess.formAjax = $form => ({
  url: $form.attr('action'),
  method: $form.attr('method') || 'post',
  data: $form.serialize()
});
