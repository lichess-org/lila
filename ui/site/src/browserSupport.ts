import { frag, once } from 'lib';
import { isIos, isSafari, isWebkit } from 'lib/device';
import { pubsub } from 'lib/pubsub';
import { alert } from 'lib/view';

export async function loadPolyfills(): Promise<void> {
  await Promise.all([dialogPolyfill(), resizePolyfill()]);
}

async function dialogPolyfill() {
  let registerDialog = undefined;
  try {
    if (typeof window.HTMLDialogElement === 'undefined') {
      registerDialog = (await import(site.asset.url('npm/dialog-polyfill.esm.js'))).default.registerDialog;
    }
  } finally {
    pubsub.complete('polyfill.dialog', registerDialog);
  }
}

async function resizePolyfill() {
  if (typeof window.ResizeObserver === 'undefined') {
    window.ResizeObserver = (await import('@juggle/resize-observer')).ResizeObserver;
  }
}

export function fixBrowserStyle() {
  if (isSafari()) {
    // https://bugs.webkit.org/show_bug.cgi?id=245402
    document.head.append(frag<HTMLStyleElement>('<style>legend { display: contents; }</style>'));
  }

  // prevent zoom when keyboard shows on iOS
  if (isIos() && !('MSStream' in window)) {
    const el = document.querySelector<HTMLMetaElement>('meta[name=viewport]');
    el?.setAttribute('content', el.getAttribute('content') + ',maximum-scale=1.0');
  }
}

export function upgradeNag() {
  if (isWebkit({ below: '15.4' }) && once('upgrade.nag', { days: 14 })) {
    pubsub
      .after('polyfill.dialog')
      .then(() => alert('Your browser is out of date.\nLichess may not work properly.'));
  }
}
