import { onInsert, looseH as h, VNode } from 'common/snabbdom';
import { isTouchDevice } from 'common/device';
import * as xhr from 'common/xhr';
import * as licon from 'common/licon';

let dialogPolyfill: { registerDialog: (dialog: HTMLDialogElement) => void };

// for usage see file://./../../../@types/lichess/dialog.d.ts

export const ready = lichess.load.then(async () => {
  window.addEventListener('resize', onResize);
  if (window.HTMLDialogElement) return true;
  dialogPolyfill = (await import(lichess.asset.url('npm/dialog-polyfill.esm.js')).catch(() => undefined))
    ?.default;
  return dialogPolyfill !== undefined;
});

export async function domDialog(o: DomDialogOpts): Promise<Dialog> {
  const [html] = await assets(o);

  const dialog = document.createElement('dialog');
  for (const [k, v] of Object.entries(o.attrs?.dialog ?? {})) dialog.setAttribute(k, String(v));
  if (isTouchDevice()) dialog.classList.add('touch-scroll');
  if (o.parent) dialog.style.position = 'absolute';

  if (!o.noCloseButton) {
    const anchor = $as<Element>('<div class="close-button-anchor">');
    anchor.innerHTML = `<button class="close-button" aria-label="Close" data-icon="${licon.X}">`;
    dialog.appendChild(anchor);
  }

  const view = $as<HTMLElement>('<div class="dialog-content">');
  if (o.class) view.classList.add(...o.class.split('/[. ]/').filter(x => x));
  for (const [k, v] of Object.entries(o.attrs?.view ?? {})) view.setAttribute(k, String(v));
  if (html) view.innerHTML = html;

  const scrollable = $as<Element>('<div class="scrollable">');
  scrollable.appendChild(view);
  dialog.appendChild(scrollable);

  (o.parent ?? document.body).appendChild(dialog);

  const wrapper = new DialogWrapper(dialog, view, o);
  if (o.show && o.show === 'modal') return wrapper.showModal();
  else if (o.show) return wrapper.show();

  return wrapper;
}

// snab dialogs are shown by default, to suppress this pass onInsert callback
export function snabDialog(o: SnabDialogOpts): VNode {
  const ass = assets(o);
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
          'div.dialog-content' +
            (o.class
              ? '.' +
                o.class
                  .split(/[. ]/)
                  .filter(x => x)
                  .join('.')
              : ''),
          {
            attrs: o.attrs?.view,
            hook: onInsert(async view => {
              const [html] = await ass;
              if (!o.vnodes && html) view.innerHTML = html;
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
  private restore?: { focus: HTMLElement; overflow: string };
  private resolve?: (dialog: Dialog) => void;
  private observer: MutationObserver = new MutationObserver(list => {
    for (const m of list)
      if (m.type === 'childList')
        for (const n of m.removedNodes) {
          if (n === this.dialog) {
            this.onClose();
            return;
          }
        }
  });

  constructor(
    readonly dialog: HTMLDialogElement,
    readonly view: HTMLElement,
    readonly o: DialogOpts,
  ) {
    if (dialogPolyfill) dialogPolyfill.registerDialog(dialog); // ios < 15.4

    const justThen = Date.now();
    const cancelOnInterval = () => Date.now() - justThen > 200 && this.close('cancel');

    this.observer?.observe(document.body, { childList: true, subtree: true });
    view.parentElement?.style.setProperty('--vh', `${window.innerHeight}px`);
    view.addEventListener('click', e => e.stopPropagation());

    dialog.addEventListener('cancel', () => !this.returnValue && (this.returnValue = 'cancel'));
    dialog.addEventListener('close', this.onClose);
    dialog.querySelector('.close-button-anchor > .close-button')?.addEventListener('click', cancelOnInterval);

    if (!o.noClickAway) setTimeout(() => dialog.addEventListener('click', cancelOnInterval));
    for (const node of o.append ?? []) {
      (node.selector ? view.querySelector(node.selector) : view)!.appendChild(node.node);
    }
    if (o.action)
      for (const a of Array.isArray(o.action) ? o.action : [o.action]) {
        view.querySelector(a.selector)?.addEventListener('click', () => {
          if (!a.action || typeof a.action === 'string') this.close(a.action);
          else a.action(this, a);
        });
      }
  }

  get open() {
    return this.dialog.open;
  }

  get returnValue() {
    return this.dialog.returnValue;
  }

  set returnValue(v: string) {
    this.dialog.returnValue = v;
  }

  show = (): Promise<Dialog> => {
    this.returnValue = '';
    this.dialog.show();
    return new Promise(resolve => (this.resolve = resolve));
  };

  showModal = (): Promise<Dialog> => {
    this.restore = {
      focus: document.activeElement as HTMLElement,
      overflow: document.body.style.overflow,
    };

    $(focusQuery, this.view)[1]?.focus();
    document.body.style.overflow = 'hidden';

    this.view.scrollTop = 0;
    this.dialog.addEventListener('keydown', onModalKeydown);
    this.returnValue = '';
    this.dialog.showModal();
    return new Promise(resolve => (this.resolve = resolve));
  };

  close = (v?: string) => {
    this.dialog.close(this.returnValue || v || 'ok');
  };

  private onClose = () => {
    this.observer.disconnect();
    if (!this.dialog.returnValue) this.dialog.returnValue = 'cancel';
    this.restore?.focus.focus(); // one modal at a time please
    if (this.restore?.overflow !== undefined) document.body.style.overflow = this.restore.overflow;
    this.restore = undefined;
    this.resolve?.(this);
    this.o.onClose?.(this);
    this.dialog.remove();
  };
}

function assets(o: DialogOpts) {
  const cssPromises = (o.css ?? []).map(css => {
    if ('themed' in css) return lichess.asset.loadCssPath(css.themed);
    else if ('url' in css) return lichess.asset.loadCss(css.url);
    else return Promise.resolve();
  });
  return Promise.all([
    o.htmlUrl
      ? xhr.text(o.htmlUrl)
      : Promise.resolve(
          o.cash ? $as<HTMLElement>($(o.cash).clone().removeClass('none')).outerHTML : o.htmlText,
        ),
    ...cssPromises,
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
  // ios safari vh behavior workaround
  $('dialog > div.scrollable').css('--vh', `${window.innerHeight}px`);
}

const focusQuery = ['button', 'input', 'select', 'textarea']
  .map(sel => `${sel}:not(:disabled)`)
  .concat(['[href]', '[tabindex="0"]', '[role="tab"]'])
  .join(',');
