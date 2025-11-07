import { pubsub } from 'lib/pubsub';

// Safari versions before 15.4 need a polyfill for dialog

function onResize() {
  // ios safari vh behavior workaround
  $('dialog > div.scrollable').css('---viewport-height', `${window.innerHeight}px`);
}

export async function loadPolyfills(): Promise<void> {
  site.polyfill ??= {} as typeof site.polyfill;
  window.addEventListener('resize', onResize);
  if (!window.HTMLDialogElement)
    site.polyfill.dialog = (
      await import(site.asset.url('npm/dialog-polyfill.esm.js')).catch(() => undefined)
    )?.default;
  pubsub.complete('polyfill.dialog');
}
