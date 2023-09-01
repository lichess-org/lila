import { VNode, h } from 'snabbdom';
import { onInsert, MaybeVNodes } from './snabbdom';
import * as xhr from './xhr';
import * as licon from './licon';

interface DialogOpts {
  class?: string; // = 'dialog', or 'period.separated.class.list'
  attrs?: { [key: string]: string }; // = {}
  cssPath?: string; // themed css base
  closeButton?: boolean; // provides an X close button
  clickAway?: boolean; // click outside to close
  onClose?: () => void;
}

export interface SnabDialogOpts extends DialogOpts {
  inner: MaybeVNodes;
  show?: boolean | 'modal'; // = false
}

export interface DomDialogOpts extends DialogOpts {
  inner: { url?: string; text?: string; el?: HTMLElement };
  parent?: HTMLElement | Cash; // = document.body
}

export function snabDialog(o: SnabDialogOpts): VNode {
  const clickOutside = (e: UIEvent) => dialog.contains(e.target as Node) && close();
  const close = () => {
    dialog.close();
    document.removeEventListener('click', clickOutside);
    o.onClose?.();
  };
  let dialog: HTMLDialogElement;
  const css = o.cssPath ? lichess.loadCssPath(o.cssPath) : Promise.resolve();

  return h('');
  /*return h(`dialog.${o.class ?? 'dialog'}`, { attrs: o?.attrs, hook: onInsert(async el => {
    dialog = el as HTMLDialogElement;

  }) }, [
    !o.clickAway
      ? null
      : h('button.close', {
          attrs: { 'data-icon': licon.X, 'aria-label': 'Close' },
          hook: onInsert(el => {
            const dialog = el.parentElement as HTMLDialogElement;
            if (o.clickAway) document.addEventListener('click', clickOutside);
            el.addEventListener('click', close);
            console.log('hayo', o.cssPath);
            if (o.cssPath) await lichess.loadCssPath(o.cssPath);
            console.log('now we show');
            if (o.show === 'modal') dialog.showModal();
            else if (o.show) dialog.show();
          }),
        }),
    ...o.inner,
  ]);*/
}

export async function domDialog(o: DomDialogOpts): Promise<HTMLDialogElement> {
  if (!o.inner.url && !o.inner.text && !o.inner.el) return Promise.reject();

  const [html] = await Promise.all([
    o.inner.url ? xhr.text(o.inner.url) : Promise.resolve(o.inner.text),
    o.cssPath ? lichess.loadCssPath(o.cssPath) : Promise.resolve(),
  ]);

  const dialog = document.createElement('dialog');
  dialog.classList.add(...(o.class ?? 'dialog').split('.'));
  for (const [attr, val] of Object.entries(o.attrs ?? {})) dialog.setAttribute(attr, val);

  const close = () => {
    dialog.remove();
    document.removeEventListener('click', clickOutside);
    o.onClose?.();
  };
  const clickOutside = (e: UIEvent) => dialog.contains(e.target as Node) && close();

  if (o.closeButton) {
    dialog.appendChild(
      $as<Node>(
        $(
          '<div class="close-button-anchor">' +
            `<button class="button-none close" aria-label="Close" data-icon="${licon.X}"/></div>`,
        ),
      ),
    );
    $('button.close', dialog).on('click', close);
  }
  if (o.clickAway) document.addEventListener('click', clickOutside);

  dialog.appendChild(o.inner.el ?? $as<HTMLElement>($(html)));

  if (o.parent) $(o.parent).append(dialog);
  else document.body.appendChild(dialog);

  return dialog;
}
