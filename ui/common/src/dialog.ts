import { VNode, Attrs } from 'snabbdom';
import { onInsert, h, MaybeVNodes } from './snabbdom';
import { spinnerVdom } from './spinner';
import * as xhr from './xhr';
import * as licon from './licon';

export interface Dialog {
  readonly open: boolean;
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
  vnodes?: MaybeVNodes;
  onInsert?: (dialog: Dialog) => void; // prevents showModal, caller must do so manually
}

export interface DomDialogOpts extends DialogOpts {
  parent?: HTMLElement | Cash; // TODO - for positioning, need to fix css to be useful
  show?: 'modal' | boolean; // auto-show and remove from dom when closed, no reshow
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
        'div.scrollable',
        h(
          'div.dialog-content' + (o.class ? `.${o.class}` : ''),
          {
            attrs: o.attrs?.view,
            hook: onInsert(async view => {
              const [html] = await ass;
              if (html && !o.vnodes) view.innerHTML = html;

              const wrapper = new DialogWrapper(dialog, view, o);

              if (o.onInsert) o.onInsert(wrapper);
              else wrapper.showModal();
            }),
          },
          o.vnodes ?? spinnerVdom(),
        ),
      ),
    ],
  );
}

export async function domDialog(o: DomDialogOpts): Promise<Dialog> {
  const [html] = await assets(o);

  const dialog = document.createElement('dialog');
  for (const [k, v] of Object.entries(o.attrs?.dialog ?? {})) dialog.setAttribute(k, String(v));

  const scrollable = $as<Element>('<div class="scrollable"/>');
  const view = $as<HTMLElement>('<div class="dialog-content"/>');
  view.classList.add(...(o.class ?? '').split('.'));
  for (const [k, v] of Object.entries(o.attrs?.view ?? {})) view.setAttribute(k, String(v));
  if (html) view.innerHTML = html;
  scrollable.appendChild(view);

  if (!o.noCloseButton) {
    const anchor = $as<Element>(
      `<div class="close-button-anchor">` +
        `<button class="close-button" type="cancel" aria-label="Close" data-icon="${licon.X}"/></div>`,
    );
    anchor.querySelector('button')?.addEventListener('click', () => dialog.close());
    dialog.appendChild(anchor);
  }
  dialog.appendChild(scrollable);
  if (!o.parent) document.body.appendChild(dialog);
  else {
    $(o.parent).append(dialog);
    dialog.style.position = 'absolute';
  }

  const wrapper = new DialogWrapper(dialog, view, o);
  if (o.show && o.show === 'modal') wrapper.showModal();
  else if (o.show) wrapper.show();

  return wrapper;
}

class DialogWrapper implements Dialog {
  constructor(
    readonly dialog: HTMLDialogElement,
    readonly view: HTMLElement,
    readonly o: DialogOpts,
  ) {
    dialog.addEventListener('close', () => this.onClose());
    if ('show' in o && o.show) dialog.addEventListener('close', dialog.remove);
    if (!o.noClickAway) dialog.addEventListener('click', () => dialog.close());
    view.addEventListener('click', e => e.stopPropagation());
    this.onResize(); // safari vh
  }
  get open() {
    return this.dialog.open;
  }

  show = () => this.dialog.show();
  close = () => this.dialog.close();

  onResize = () => this.view.style.setProperty('--vh', `${window.innerHeight * 0.01}px`);
  onClose = () => {
    window.removeEventListener('resize', this.onResize);
    this.o.onClose?.(this);
  };

  showModal = () => {
    const focii = Array.from($(focusQuery, this.view)) as HTMLElement[];
    if (focii.length > 1) focii[1].focus(); // skip close button
    else if (focii.length) focii[0].focus();
    window.addEventListener('resize', this.onResize);

    this.addModalListeners?.();
    this.view.scrollTop = 0;
    this.dialog.showModal();
  };

  addModalListeners? = () => {
    this.dialog.addEventListener('keydown', onModalKeydown);
    this.dialog.addEventListener('touchmove', (e: TouchEvent) => e.stopPropagation());
    this.addModalListeners = undefined; // only do this once
  };
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
