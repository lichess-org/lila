import { VNode, h } from 'snabbdom';
import { onInsert, MaybeVNodes } from './snabbdom';
import { spinnerVdom } from './spinner';
import * as xhr from './xhr';
import * as licon from './licon';

interface DialogOpts {
  // ui/common/css/component/dialog.scss
  class?: string; // 'period.separated.class.list' for content div
  cssPath?: string; // themed css base
  closeButton?: boolean; // provides an X close button
  onClose?: () => void;
  clickAway?: boolean; // click outside to close
}

export interface SnabDialogOpts extends DialogOpts {
  htmlUrl?: string;
  content?: MaybeVNodes;
  modal?: boolean; // default = false
  onInsert?: ($wrap: Cash) => void;
  onShow?: () => void;
}

export interface DomDialogOpts extends DialogOpts {
  inner: { url?: string; text?: string; el?: HTMLElement };
  parent?: HTMLElement | Cash; // default = document.body
}

export function snabDialog(o: SnabDialogOpts): VNode {
  let dialog: HTMLDialogElement;
  //const close = () => dialog.close();

  const all = Promise.all([
    o.htmlUrl ? xhr.text(o.htmlUrl) : Promise.resolve(undefined),
    o.cssPath ? lichess.loadCssPath(o.cssPath) : Promise.resolve(),
  ]);

  return h(
    'dialog.base-dialog',
    {
      hook: onInsert(async el => {
        dialog = el as HTMLDialogElement;
        if (o.clickAway)
          dialog.addEventListener('click', () => {
            console.log('hiya');
            dialog.close();
          });
        if (o.onClose) dialog.addEventListener('close', o.onClose);
        await all;
        setTimeout(() => {
          if (o.modal) dialog.showModal();
          else dialog.show();
          o.onShow?.();
        });
      }),
    },
    [
      !o.closeButton
        ? null
        : h('button.close-button', {
            attrs: { 'data-icon': licon.X, 'aria-label': 'Close' },
            hook: onInsert(el => el.addEventListener('click', () => dialog.close())),
          }),
      h(
        `div.${o.class ?? 'base-view'}`,
        {
          hook: onInsert(async el => {
            el.addEventListener('click', e => e.stopPropagation());
            o.onInsert?.($(el));
            const [html] = await all;
            if (html) el.innerHTML = html;
            if (o.modal) dialog.showModal();
            else dialog.show();
            o.onShow?.();
          }),
        },
        o.content ?? [spinnerVdom()],
      ),
    ],
  );
}

export async function domDialog(o: DomDialogOpts): Promise<HTMLDialogElement> {
  const close = () => {
    dialog.remove();
    o.onClose?.();
  };

  const dialog = $as<HTMLDialogElement>('<dialog class="base-dialog>');
  const view = document.createElement('div');

  const [html] = await Promise.all([
    o.inner.url ? xhr.text(o.inner.url) : Promise.resolve(o.inner.text),
    o.cssPath ? lichess.loadCssPath(o.cssPath) : Promise.resolve(),
  ]);

  view.classList.add(...(o.class ?? 'base-view').split('.'));
  view.addEventListener('click', e => e.stopPropagation());
  if (o.inner.el || html) view.appendChild(o.inner.el ?? $as<Node>(html!));

  if (o.clickAway) dialog.addEventListener('click', close);

  if (o.closeButton) {
    const anchor = $as<Element>('<div class="close-button-anchor">');
    const btn = anchor.appendChild(
      $as<Node>(`<button class="close-button" aria-label="Close" data-icon="${licon.X}"/>`),
    );
    dialog.appendChild(anchor);
    btn.addEventListener('click', close);
  }

  dialog.appendChild(view);

  if (o.parent) $(o.parent).append(dialog);
  else document.body.appendChild(dialog);

  return dialog;
}
