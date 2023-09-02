import { VNode, h } from 'snabbdom';
import { onInsert, MaybeVNodes } from './snabbdom';
import { spinnerVdom } from './spinner';
import * as xhr from './xhr';
import * as licon from './licon';

// see ui/common/css/component/dialog.scss

interface DialogOpts {
  class?: string;
  htmlUrl?: string;
  cssPath?: string;
  onClose?: () => void;
  noCloseButton?: boolean;
  noClickAway?: boolean;
}

export interface SnabDialogOpts extends DialogOpts {
  vnodes?: MaybeVNodes;
  onInsert?: ($wrap: Cash) => void;
  noShow?: (dialog: HTMLDialogElement) => void; // get dialog for direct access
}

export interface DomDialogOpts extends DialogOpts {
  el?: Node;
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
    'dialog',
    {
      key: o.class ?? 'dialog',
      hook: onInsert(el => {
        dialog = el as HTMLDialogElement;
        if (!o.noClickAway) dialog.addEventListener('click', () => dialog.close());

        if (o.onClose) dialog.addEventListener('close', o.onClose);
        dialog.addEventListener('keydown', onKeydown);
      }),
    },
    [
      o.noCloseButton
        ? null
        : h('button.close-button', {
            attrs: { 'data-icon': licon.X, 'aria-label': 'Close' },
            hook: onInsert(el => el.addEventListener('click', () => dialog.close())),
          }),
      h(
        o.class ? `div.${o.class}` : 'div',
        {
          hook: onInsert(async el => {
            if (!o.noClickAway) el.addEventListener('click', e => e.stopPropagation());
            const [html] = await all;
            if (html) el.innerHTML = html;
            o.onInsert?.($(el));
            if (o.noShow) o.noShow(dialog);
            else dialog.showModal();
            focus(dialog);
          }),
        },
        o.vnodes ?? [spinnerVdom()],
      ),
    ],
  );
}

export async function domDialog(o: DomDialogOpts): Promise<HTMLDialogElement> {
  const onClose = () => {
    dialog.remove();
    o.onClose?.();
  };

  const dialog = document.createElement('dialog');
  const view = document.createElement('div');
  const [html] = await Promise.all([
    o.htmlUrl ? xhr.text(o.htmlUrl) : Promise.resolve(undefined),
    o.cssPath ? lichess.loadCssPath(o.cssPath) : Promise.resolve(),
  ]);

  view.classList.add(...(o.class ?? '').split('.'));
  view.addEventListener('click', e => e.stopPropagation());

  if (o.el || html) view.appendChild(o.el ?? $as<Node>(html!));

  if (!o.noClickAway) dialog.addEventListener('click', () => dialog.close());
  dialog.addEventListener('close', onClose);
  dialog.addEventListener('keydown', onKeydown);

  if (!o.noCloseButton) {
    const anchor = $as<Element>('<div class="close-button-anchor">');
    const btn = anchor.appendChild(
      $as<Node>(`<button class="close-button" aria-label="Close" data-icon="${licon.X}"/>`),
    );
    dialog.appendChild(anchor);
    btn.addEventListener('click', () => dialog.close());
  }

  dialog.appendChild(view);

  if (o.parent) $(o.parent).append(dialog);
  else document.body.appendChild(dialog);

  focus(dialog);
  return dialog;
}

const focusQuery = ['button', 'input', 'select', 'textarea']
  .map(sel => `${sel}:not(:disabled)`)
  .concat(['[href]', '[tabindex="0"]'])
  .join(',');

function focus(dialog: Element) {
  const $focii = $(focusQuery, dialog);
  ($as<HTMLElement>($focii.eq(1)) ?? $as<HTMLElement>($focii)).focus();
}

function onKeydown(e: KeyboardEvent) {
  const dialog = e.currentTarget as HTMLDialogElement;

  if (e.key === 'Escape' || e.key === 'Enter') dialog.close();
  else if (e.key === 'Tab') {
    const $focii = $(focusQuery, dialog),
      first = $as<HTMLElement>($focii.first()),
      last = $as<HTMLElement>($focii.last());

    if (e.shiftKey && document.activeElement === first) first?.focus();
    else if (!e.shiftKey && document.activeElement === last) last?.focus();
  } else if (dialog.contains(e.target as Node)) return;

  e.stopPropagation();
  e.preventDefault();
}
