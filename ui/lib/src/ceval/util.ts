/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { isMobile } from '../device';
import { memoize, escapeHtml } from '../index';
import { domDialog } from '../view/dialog';

export function isEvalBetter(a: Tree.ClientEval, b: Tree.ClientEval, desiredPvs: number): boolean {
  return (
    a.depth > b.depth ||
    (a.depth === b.depth && a.nodes > b.nodes) ||
    (a.pvs.length >= desiredPvs && b.pvs.length < desiredPvs)
  );
}

export function renderEval(e: number): string {
  e = Math.max(Math.min(Math.round(e / 10) / 10, 99), -99);
  return (e > 0 ? '+' : '') + e.toFixed(1);
}

export function sanIrreversible(variant: VariantKey, san: string): boolean {
  if (san.startsWith('O-O')) return true;
  if (variant === 'crazyhouse') return false;
  if (san.includes('x')) return true; // capture
  if (san[0].toLowerCase() === san[0]) return true; // pawn move
  return variant === 'threeCheck' && san.includes('+');
}

export const fewerCores: () => boolean = memoize<boolean>(
  () => isMobile() || navigator.userAgent.includes('CrOS'),
);

export const sharedWasmMemory = (lo: number, hi = 32767): WebAssembly.Memory => {
  let shrink = 4; // 32767 -> 24576 -> 16384 -> 12288 -> 8192 -> 6144 -> etc
  while (true) {
    try {
      return new WebAssembly.Memory({ shared: true, initial: lo, maximum: hi });
    } catch (e) {
      if (hi <= lo || !(e instanceof RangeError)) throw e;
      hi = Math.max(lo, Math.ceil(hi - hi / shrink));
      shrink = shrink === 4 ? 3 : 4;
    }
  }
};

export function showEngineError(engine: string, error: string): void {
  domDialog({
    class: 'engine-error',
    modal: true,
    htmlText:
      `<h2>${escapeHtml(engine)} <bad>error</bad></h2>` +
      (error.includes('Status 503')
        ? $html`
          <p>Your external engine does not appear to be connected.</p>
          <p>Please check the network and restart your provider if possible.</p>`
        : $html`
          <pre>${escapeHtml(error)}</pre>
          <h2>Things to try</h2>
          <ul>
            <li>Decrease memory slider in engine settings</li>
            <li>Clear site data for lichess.org</li>
            <li>Select another engine</li>
            <li>Update your browser</li>
          </ul>`),
  }).then(dlg => {
    const select = () =>
      setTimeout(() => {
        const range = document.createRange();
        range.selectNodeContents(dlg.view.querySelector('.err')!);
        window.getSelection()?.removeAllRanges();
        window.getSelection()?.addRange(range);
      }, 0);
    dlg.view.querySelector('.err')?.addEventListener('focus', select);
    dlg.show();
  });
}
