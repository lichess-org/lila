import { log } from 'common/permalog';
import { domDialog } from 'common/dialog';
import { escapeHtml } from 'common';

function terseHref(): string {
  return window.location.href.replace(/^(https:\/\/)?(?:lichess|lichess1)\.org\//, '/');
}

export function addExceptionListeners() {
  window.addEventListener('error', async e => {
    const loc = e.filename ? ` - (${e.filename}:${e.lineno}:${e.colno})` : '';
    log(`${terseHref()} - ${e.message}${loc}\n${e.error?.stack ?? ''}`.trim());
    if (site.debug)
      domDialog({
        htmlText: escapeHtml(`${e.message}${loc}\n${e.error?.stack ?? ''}`),
        class: 'debug',
        show: true,
      });
  });

  window.addEventListener('unhandledrejection', async e => {
    let reason = e.reason;
    if (typeof reason !== 'string')
      try {
        reason = JSON.stringify(e.reason);
      } catch (_) {
        reason = 'unhandled rejection, reason not a string';
      }
    log(`${terseHref()} - ${reason}`);
    if (site.debug)
      domDialog({
        htmlText: escapeHtml(reason),
        class: 'debug',
        show: true,
      });
  });
}
