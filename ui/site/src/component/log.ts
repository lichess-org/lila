import { objectStorage, ObjectStorage } from 'common/objectStorage';
import { isTouchDevice } from 'common/device';
import { domDialog } from 'common/dialog';

export default function makeLog(): LichessLog {
  let store: ObjectStorage<string, number>;
  let resolveReady: () => void;
  let lastKey = 0;
  let drift = 0.001;

  const keep = 1000; // trimmed on startup
  const ready = new Promise<void>(resolve => (resolveReady = resolve));

  objectStorage<string, number>({ store: 'log' })
    .then(async s => {
      const keys = await s.list();
      if (keys.length > keep) {
        await s.remove(IDBKeyRange.upperBound(keys[keys.length - keep], true));
      }
      store = s;
      resolveReady();
    })
    .catch(() => {
      resolveReady();
      objectStorage<string, number>({ store: 'log' })
        .then(s => s.clear())
        .catch(() => {});
    });

  function stringify(val: any): string {
    return !val || typeof val === 'string' ? String(val) : JSON.stringify(val);
  }

  const log: any = async (...args: any[]) => {
    const msg = args.map(stringify).join(' ');
    let nextKey = Date.now();
    console.log(...args);
    if (nextKey === lastKey) {
      nextKey += drift;
      drift += 0.001;
    } else {
      drift = 0.001;
      lastKey = nextKey;
    }
    await ready;
    await store?.put(nextKey, msg);
  };

  log.clear = async () => {
    await ready;
    await store?.clear();
    lastKey = 0;
  };

  log.get = async (): Promise<string> => {
    await ready;
    const [keys, vals] = await Promise.all([store.list(), store.getMany()]);
    return keys.map((k, i) => `${new Date(k).toISOString()} - ${vals[i]}`).join('\n');
  };

  log.diagnostic = show;

  window.addEventListener('error', async e => {
    log(`${e.message} (${e.filename}:${e.lineno}:${e.colno})\n${e.error?.stack ?? ''}`.trim());
  });
  window.addEventListener('unhandledrejection', async e => {
    log(`${e.reason}\n${e.reason.stack ?? ''}`.trim());
  });

  return log;
}

async function show() {
  const logs = await lichess.log.get();
  const text =
    `Browser: ${navigator.userAgent}\n` +
    `Cores: ${navigator.hardwareConcurrency}\n` +
    `Touch: ${isTouchDevice()} ${navigator.maxTouchPoints}\n` +
    `Screen: ${window.screen.width}x${window.screen.height}\n` +
    `Lang: ${navigator.language}` +
    (logs ? `\n\n${logs}` : '');

  const dlg = await domDialog({
    class: 'diagnostic',
    cssPath: 'diagnostic',
    htmlText:
      `<h2>Diagnostics</h2><pre tabindex="0" class="err">${lichess.escapeHtml(text)}</pre>` +
      (logs ? `<button class="clear button">Clear Logs</button>` : ''),
  });
  const select = () =>
    setTimeout(() => {
      const range = document.createRange();
      range.selectNodeContents(dlg.view.querySelector('.err')!);
      window.getSelection()?.removeAllRanges();
      window.getSelection()?.addRange(range);
    }, 0);
  $('.err', dlg.view).on('focus', select);
  $('.clear', dlg.view).on('click', () => lichess.log.clear().then(lichess.reload));
  dlg.showModal();
}
