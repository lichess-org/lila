import { onInsert, looseH as h, VNode, Attrs, LooseVNodes } from './snabbdom';
import { isTouchDevice } from './device';
import * as xhr from './xhr';
import * as licon from './licon';

let dialogPolyfill: { registerDialog: (dialog: HTMLDialogElement) => void };

export interface Dialog {
  readonly open: boolean; // is visible?
  readonly view: HTMLElement; // your content div
  readonly returnValue?: 'ok' | 'cancel' | string; // how did we close?

  showModal(): Promise<Dialog>; // resolves on close
  show(): Promise<Dialog>; // resolves on close
  actions(actions?: Action | Action[]): void; // set new or reattach existing actions
  close(): void;
}

export interface DialogOpts {
  class?: string; // zero or more classes for your view div
  css?: ({ url: string } | { hashed: string })[]; // fetches hashed or full url css
  htmlText?: string; // content, text will be used as-is
  cash?: Cash; // content, overrides htmlText, will be cloned and any 'none' class removed
  htmlUrl?: string; // content, overrides htmlText and cash, url will be xhr'd
  append?: { node: HTMLElement; where?: string }[]; // if no where selector, appends to view
  attrs?: { dialog?: Attrs; view?: Attrs }; // optional attrs for dialog and view div
  actions?: Action | Action[]; // if present, add listeners to action buttons
  onClose?: (dialog: Dialog) => void; // called when dialog closes
  noCloseButton?: boolean; // if true, no upper right corner close button
  noClickAway?: boolean; // if true, no click-away-to-close
  noScrollable?: boolean; // if true, no scrollable div container. Fixes dialogs containing an auto-completer
}

export interface DomDialogOpts extends DialogOpts {
  parent?: Element; // for centering and dom placement, otherwise fixed on document.body
  show?: 'modal' | boolean; // if not falsy, auto-show, and if 'modal' remove from dom on close
}

//snabDialog automatically shows as 'modal' on redraw unless onInsert callback is supplied
export interface SnabDialogOpts extends DialogOpts {
  vnodes?: LooseVNodes; // content, overrides other content properties
  onInsert?: (dialog: Dialog) => void; // if supplied, call show() or showModal() manually
}

export type ActionListener = (dialog: Dialog, action: Action, e: Event) => void;

// Actions are managed listeners / results that are easily refreshed on DOM changes
// if no event is specified, then 'click' is assumed
export type Action =
  | { selector: string; event?: string | string[]; listener: ActionListener }
  | { selector: string; event?: string | string[]; result: string };

export const ready = site.load.then(async () => {
  window.addEventListener('resize', onResize);
  if (window.HTMLDialogElement) return true;
  dialogPolyfill = (await import(site.asset.url('npm/dialog-polyfill.esm.js')).catch(() => undefined))
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
  if (o.class) view.classList.add(...o.class.split(/[. ]/).filter(x => x));
  for (const [k, v] of Object.entries(o.attrs?.view ?? {})) view.setAttribute(k, String(v));
  if (html) view.innerHTML = html;

  const scrollable = $as<Element>(`<div class="${o.noScrollable ? 'not-' : ''}scrollable">`);
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
        `div.${o.noScrollable ? 'not-' : ''}scrollable`,
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
  private restore?: { focus?: HTMLElement; overflow: string };
  private resolve?: (dialog: Dialog) => void;
  private eventCleanup: { el: Element; type: string; listener: EventListener }[] = [];
  private observer: MutationObserver = new MutationObserver(list => {
    for (const m of list)
      if (m.type === 'childList')
        for (const n of m.removedNodes) {
          if (n === this.dialog) {
            this.onRemove();
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

    this.observer.observe(document.body, { childList: true, subtree: true });
    view.parentElement?.style.setProperty('---viewport-height', `${window.innerHeight}px`);
    view.addEventListener('click', e => e.stopPropagation());

    dialog.addEventListener('cancel', () => !this.returnValue && (this.returnValue = 'cancel'));
    dialog.addEventListener('close', this.onRemove);
    dialog.querySelector('.close-button-anchor > .close-button')?.addEventListener('click', cancelOnInterval);

    if (!o.noClickAway) setTimeout(() => dialog.addEventListener('click', cancelOnInterval));
    for (const app of o.append ?? []) {
      (app.where ? view.querySelector(app.where) : view)?.appendChild(app.node);
    }
    this.actions();
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

  // attach/reattach existing listeners or provide a set of new ones
  actions = (actions = this.o.actions) => {
    for (const { el, type, listener } of this.eventCleanup) {
      el.removeEventListener(type, listener);
    }
    this.eventCleanup = [];
    if (!actions) return;
    for (const a of Array.isArray(actions) ? actions : [actions]) {
      for (const event of Array.isArray(a.event) ? a.event : a.event ? [a.event] : ['click']) {
        for (const el of this.view.querySelectorAll(a.selector)) {
          const listener = (e: Event) => {
            if ('listener' in a) a.listener(this, a, e);
            else this.close(a.result);
          };
          this.eventCleanup.push({ el, type: event, listener });
          el.addEventListener(event, listener);
        }
      }
    }
  };

  show = (): Promise<Dialog> => {
    this.restore = {
      overflow: document.body.style.overflow,
    };
    document.body.style.overflow = 'hidden';
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

  private onRemove = () => {
    this.observer.disconnect();
    if (!this.dialog.returnValue) this.dialog.returnValue = 'cancel';
    this.restore?.focus?.focus(); // one modal at a time please
    if (this.restore?.overflow !== undefined) document.body.style.overflow = this.restore.overflow;
    this.restore = undefined;
    this.resolve?.(this);
    this.o.onClose?.(this);
    this.dialog.remove();
    for (const css of this.o.css ?? [])
      'hashed' in css && site.asset.removeCssPath(css.hashed), 'url' in css && site.asset.removeCss(css.url);
  };
}

function assets(o: DialogOpts) {
  const cssPromises = (o.css ?? []).map(css => {
    if ('hashed' in css) return site.asset.loadCssPath(css.hashed);
    else if ('url' in css) return site.asset.loadCss(css.url);
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
  $('dialog > div.scrollable').css('---viewport-height', `${window.innerHeight}px`);
}

const focusQuery = ['button', 'input', 'select', 'textarea']
  .map(sel => `${sel}:not(:disabled)`)
  .concat(['[href]', '[tabindex="0"]', '[role="tab"]'])
  .join(',');
