import { VNode, Attrs } from 'snabbdom';
import { onInsert, h, MaybeVNodes } from './snabbdom';
import { isTouchDevice, isIOS } from './mobile';
import * as xhr from './xhr';
import * as licon from './licon';

let dialogPolyfill: { registerDialog: (dialog: HTMLDialogElement) => void };

lichess.load.then(() => {
  window.addEventListener('resize', onResize);
  if (isIOS({ below: 15.4 })) {
    import(lichess.assetUrl('npm/dialog-polyfill.esm.js')).then(m => (dialogPolyfill = m.default));
  }
});

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

export interface DomDialogOpts extends DialogOpts {
  parent?: Element;
  show?: 'modal' | boolean;
}

export interface SnabDialogOpts extends DialogOpts {
  vnodes?: MaybeVNodes;
  onInsert?: (dialog: Dialog) => void;
}

export async function domDialog(o: DomDialogOpts): Promise<Dialog> {
  const [html] = await assets(o.html, o.cssPath, o.cash);

  const dialog = document.createElement('dialog');
  if (isTouchDevice()) dialog.classList.add('touch-scroll');
  if (o.parent) dialog.style.position = 'absolute';
  for (const [k, v] of Object.entries(o.attrs?.dialog ?? {})) dialog.setAttribute(k, String(v));

  if (!o.noCloseButton) {
    const anchor = $as<Element>('<div class="close-button-anchor">');
    anchor.innerHTML = `<button class="close-button" aria-label="Close" data-icon="${licon.X}">`;
    dialog.appendChild(anchor);
  }

  const view = $as<HTMLElement>('<div class="dialog-content">');
  if (o.class) view.classList.add(...o.class.split('.'));
  for (const [k, v] of Object.entries(o.attrs?.view ?? {})) view.setAttribute(k, String(v));
  if (html) view.innerHTML = html;

  const scrollable = $as<Element>('<div class="scrollable">');
  scrollable.appendChild(view);
  dialog.appendChild(scrollable);

  (o.parent ?? document.body).appendChild(dialog);

  const wrapper = new DialogWrapper(dialog, view, o);
  if (o.show && o.show === 'modal') wrapper.showModal();
  else if (o.show) wrapper.show();

  return wrapper;
}

export function snabDialog(o: SnabDialogOpts): VNode {
  const ass = assets(o.html, o.cssPath, o.cash);
  let dialog: HTMLDialogElement;

  return h(
    `dialog${isTouchDevice() ? '.touch-scroll' : ''}`,
    {
      key: o.class ?? 'dialog',
      attrs: o.attrs?.dialog,
      hook: onInsert(el => (dialog = el as HTMLDialogElement)),
    },
    [
      o.noCloseButton ||
        h(
          'div.close-button-anchor',
          h('button.close-button', { attrs: { 'data-icon': licon.X, 'aria-label': 'Close' } }),
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
          o.vnodes,
        ),
      ),
    ],
  );
}

class DialogWrapper implements Dialog {
  constructor(
    readonly dialog: HTMLDialogElement,
    readonly view: HTMLElement,
    readonly o: DialogOpts,
  ) {
    if (dialogPolyfill) {
      console.log('registered one');
      dialogPolyfill.registerDialog(dialog); // ios < 15.4
    }
    view.parentElement?.style.setProperty('--vh', `${window.innerHeight * 0.01}px`); // ios safari
    view.addEventListener('click', e => e.stopPropagation());

    dialog.addEventListener('close', this.onClose);
    dialog.querySelector('button.close-button')?.addEventListener('click', this.close);

    if (!o.noClickAway) setTimeout(() => dialog.addEventListener('click', this.close), 0);
  }

  get open() {
    return this.dialog.open;
  }

  show = () => this.dialog.show();

  restoreFocus?: HTMLElement;

  showModal = () => {
    this.restoreFocus = document.activeElement as HTMLElement;
    $(focusQuery, this.view)[1]?.focus();

    this.addModalListeners?.();
    this.view.scrollTop = 0;

    this.dialog.showModal();
  };

  close = () => this.dialog.close();

  onClose = () => {
    this.o.onClose?.(this);
    if ('show' in this.o && this.o.show === 'modal') this.dialog.remove();
    this.restoreFocus?.focus();
    this.restoreFocus = undefined;
  };

  addModalListeners? = () => {
    this.dialog.addEventListener('keydown', onModalKeydown);
    //if (isTouchDevice()) this.dialog.addEventListener('touchmove', (e: TouchEvent) => e.stopPropagation());
    this.addModalListeners = undefined; // only once per HTMLDialogElement
  };
}

function assets(html?: { url?: string; text?: string }, cssPath?: string, cash?: Cash) {
  return Promise.all([
    html?.url
      ? xhr.text(html.url)
      : Promise.resolve(cash ? $as<HTMLElement>($(cash).clone().removeClass('none')).outerHTML : html?.text),
    cssPath ? lichess.loadCssPath(cssPath) : Promise.resolve(),
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

function onResize() {
  const vh = window.innerHeight * 0.01; // ios safari
  $('dialog > div.scrollable').css('--vh', `${vh}px`);
}

const focusQuery = ['button', 'input', 'select', 'textarea']
  .map(sel => `${sel}:not(:disabled)`)
  .concat(['[href]', '[tabindex="0"]'])
  .join(',');
