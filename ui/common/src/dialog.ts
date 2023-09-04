import { VNode, Attrs } from 'snabbdom';
import { onInsert, h, MaybeVNodes } from './snabbdom';
import { spinnerVdom } from './spinner';
import { isTouchDevice } from './mobile';
import * as xhr from './xhr';
import * as licon from './licon';

export interface Dialog {
  readonly open: boolean;
  readonly returnValue: string | undefined;
  readonly view: HTMLElement;

  showModal(): void;
  show(): void;
  close(): void;
}

interface DialogOpts {
  class?: string;
  cssPath?: string;
  cash?: Cash;
  html?: { url?: string; text?: string };
  attrs?: { dialog?: Attrs; view?: Attrs };
  onClose?: (dialog: Dialog) => void;
  noCloseButton?: boolean;
  noClickAway?: boolean;
}

export interface SnabDialogOpts extends DialogOpts {
  content?: MaybeVNodes;
  onInsert?: (dialog: Dialog) => void; // prevents showModal, caller must do so manually
}

export interface DomDialogOpts extends DialogOpts {
  parent?: HTMLElement | Cash; // TODO - for positioning, need to fix css to be useful
}

export function snabDialog(o: SnabDialogOpts): VNode {
  const ass = assets(o);
  let dialog: HTMLDialogElement;

  return h(
    'dialog',
    {
      key: o.class ?? 'dialog',
      attrs: o.attrs?.dialog,
      hook: onInsert(el => (dialog = el as HTMLDialogElement)),
    },
    [
      o.noCloseButton ||
        h(
          'div.close-button-anchor',
          h('button.close-button', {
            attrs: { 'data-icon': licon.X, 'aria-label': 'Close', type: 'cancel' },
            hook: onInsert(el => el.addEventListener('click', () => dialog.close())),
          }),
        ),
      h(
        'div.modal-wrap' + (o.class ? `.${o.class}` : ''),
        {
          attrs: o.attrs?.view,
          hook: onInsert(async view => {
            const [html] = await ass;
            if (html) view.innerHTML = html;

            const dlg = makeDialog(dialog, view, o);

            if (o.onInsert) o.onInsert(dlg);
            else dlg.showModal();
          }),
        },
        o.content ?? spinnerVdom(),
      ),
    ],
  );
}

export async function domDialog(o: DomDialogOpts): Promise<Dialog> {
  const [html] = await assets(o);

  const dialog = document.createElement('dialog');
  for (const [k, v] of Object.entries(o.attrs?.dialog ?? {})) dialog.setAttribute(k, String(v));

  const view = document.createElement('div');
  view.classList.add('modal-wrap', ...(o.class ?? '').split('.'));
  for (const [k, v] of Object.entries(o.attrs?.view ?? {})) view.setAttribute(k, String(v));

  if (html) view.innerHTML = html;

  if (!o.noCloseButton) {
    const anchor = $as<Element>('<div class="close-button-anchor">');
    const btn = anchor.appendChild(
      $as<Node>(`<button class="close-button" type="cancel" aria-label="Close" data-icon="${licon.X}"/>`),
    );
    console.log(anchor);
    dialog.appendChild(anchor);
    btn.addEventListener('click', () => dialog.close());
  }

  dialog.appendChild(view);
  if (o.parent) $(o.parent).append(dialog);
  else document.body.appendChild(dialog);

  return makeDialog(dialog, view, o);
}

function makeDialog(dialog: HTMLDialogElement, view: HTMLElement, o: DialogOpts): Dialog {
  let modalListeners: { [event: string]: (e: Event) => void } = {
    keydown: onModalKeydown,
    touchmove: (e: TouchEvent) => isTouchDevice() && e.preventDefault(),
  };

  function show(modal: boolean) {
    const focii = Array.from($(focusQuery, view)) as HTMLElement[];
    if (focii.length) (focii.length > 1 ? focii[1] : focii[0]).focus();
    if (modal) {
      Object.entries(modalListeners).forEach(([e, l]) => dialog.addEventListener(e, l));
      modalListeners = {}; // only add them once, dialog may be reshown
    }
    dialog.returnValue = '';
    view.scrollTop = 0;

    if (modal) dialog.showModal();
    else dialog.show();
  }

  return new (class implements Dialog {
    constructor(readonly view: HTMLElement) {
      dialog.addEventListener('close', () => o.onClose?.(this));
      if (!o.noClickAway) dialog.addEventListener('click', () => dialog.close());
      view.addEventListener('click', e => e.stopPropagation());
    }
    show = () => show(false);
    showModal = () => show(true);
    close = () => dialog.close();
    get open() {
      return dialog.open;
    }
    get returnValue() {
      return dialog.returnValue;
    }
    get html() {
      return view.innerHTML;
    }
    set html(html: string) {
      view.innerHTML = html;
    }
  })(view);
}

function assets(o: DialogOpts) {
  return Promise.all([
    o.html?.url ? xhr.text(o.html.url) : Promise.resolve(o.cash?.html() ?? o.html?.text),
    o.cssPath ? lichess.loadCssPath(o.cssPath) : Promise.resolve(),
  ]);
}

function onModalKeydown(e: KeyboardEvent) {
  if (e.key === 'Tab') {
    const $focii = $(focusQuery, e.currentTarget as Element),
      first = $as<HTMLElement>($focii.first()),
      last = $as<HTMLElement>($focii.last()),
      focus = document.activeElement as HTMLElement;
    if (focus === last && !e.shiftKey) first.focus();
    else if (focus === first && e.shiftKey) last.focus();
    else return;
    e.preventDefault();
  }
  e.stopPropagation();
}

const focusQuery = ['button', 'input', 'select', 'textarea']
  .map(sel => `${sel}:not(:disabled)`)
  .concat(['[href]', '[tabindex="0"]'])
  .join(',');
