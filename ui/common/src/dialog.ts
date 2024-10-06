import { onInsert, looseH as h, VNode, Attrs, LooseVNodes } from './snabbdom';
import { isTouchDevice } from './device';
import { escapeHtml, frag, $as } from './common';
import { eventJanitor } from './event';
import * as xhr from './xhr';
import * as licon from './licon';
import { pubsub } from './pubsub';

let dialogPolyfill: { registerDialog: (dialog: HTMLDialogElement) => void };

export interface Dialog {
  readonly open: boolean; // is visible?
  readonly view: HTMLElement; // your content div
  readonly returnValue?: 'ok' | 'cancel' | string; // how did we close?

  showModal(): Promise<Dialog>; // resolves on close
  show(): Promise<Dialog>; // resolves on close
  updateActions(actions?: Action | Action[]): void; // set new actions, reattach existing if no arg provided
  close(returnValue?: string): void;
}

export interface DialogOpts {
  class?: string; // zero or more classes for your view div
  css?: ({ url: string } | { hashed: string })[]; // fetches hashed or full url css
  htmlText?: string; // content, text will be used as-is
  cash?: Cash; // content, overrides htmlText, will be cloned and any 'none' class removed
  htmlUrl?: string; // content, overrides htmlText and cash, url will be xhr'd
  append?: { node: HTMLElement; where?: string; how?: 'after' | 'before' | 'child' }[]; // default 'child'
  attrs?: { dialog?: Attrs; view?: Attrs }; // optional attrs for dialog and view div
  focus?: string; // query selector for element to focus on show
  actions?: Action | Action[]; // add listeners to controls
  onClose?: (dialog: Dialog) => void; // called when dialog closes
  noCloseButton?: boolean; // if true, no upper right corner close button
  noClickAway?: boolean; // if true, no click-away-to-close
  noScrollable?: boolean; // if true, no scrollable div container. Fixes dialogs containing an auto-completer
}

export interface DomDialogOpts extends DialogOpts {
  parent?: Element; // for centering and dom placement, otherwise fixed on document.body
  show?: 'modal' | boolean; // if not falsy, auto-show, and if 'modal' remove from dom on close
}

//snabDialog automatically shows as 'modal' unless onInsert callback is supplied
export interface SnabDialogOpts extends DialogOpts {
  vnodes?: LooseVNodes; // content, overrides other content properties
  onInsert?: (dialog: Dialog) => void; // if supplied, you must call show() or showModal() manually
}

export type ActionListener = (e: Event, dialog: Dialog, action: Action) => void;

// Actions are managed listeners / results that are easily refreshed on DOM changes
// if no event is specified, then 'click' is assumed
// if no selector is given, then the handler is attached to the view div
export type Action =
  | { selector?: string; event?: string | string[]; listener: ActionListener }
  | { selector?: string; event?: string | string[]; result: string };

// Safari versions before 15.4 need a polyfill for dialog. this "ready" promise resolves when that's loaded
site.load.then(async () => {
  window.addEventListener('resize', onResize);
  if (!window.HTMLDialogElement)
    dialogPolyfill = (await import(site.asset.url('npm/dialog-polyfill.esm.js')).catch(() => undefined))
      ?.default;
  pubsub.complete('dialog.polyfill');
});

// non-blocking window.alert-alike
export async function alert(msg: string): Promise<void> {
  await domDialog({
    htmlText: escapeHtml(msg),
    class: 'alert',
    show: 'modal',
  });
}

// non-blocking window.confirm-alike
export async function confirm(msg: string): Promise<boolean> {
  return (
    (
      await domDialog({
        htmlText: `<div>${escapeHtml(msg)}</div>
          <span><button class="button no">no</button><button class="button yes">yes</button></span>`,
        class: 'alert',
        noCloseButton: true,
        noClickAway: true,
        show: 'modal',
        actions: [
          { selector: '.yes', result: 'yes' },
          { selector: '.no', result: 'no' },
        ],
      })
    ).returnValue === 'yes'
  );
}

// when opts contains 'show', this promise resolves as show/showModal (on dialog close) so check returnValue
// otherwise, this promise resolves once assets are loaded and things are fully constructed but not shown
export async function domDialog(o: DomDialogOpts): Promise<Dialog> {
  const [html] = await loadAssets(o);

  const dialog = document.createElement('dialog');
  for (const [k, v] of Object.entries(o.attrs?.dialog ?? {})) dialog.setAttribute(k, String(v));
  if (isTouchDevice()) dialog.classList.add('touch-scroll');
  if (o.parent) dialog.style.position = 'absolute';

  if (!o.noCloseButton) {
    const anchor = frag<Element>('<div class="close-button-anchor">');
    anchor.innerHTML = `<button class="close-button" aria-label="Close" data-icon="${licon.X}">`;
    dialog.appendChild(anchor);
  }

  const view = !html && o.append?.length === 1 ? o.append[0].node : document.createElement('div');
  view.classList.add('dialog-content');
  if (o.class) view.classList.add(...o.class.split(/[. ]/).filter(x => x));
  for (const [k, v] of Object.entries(o.attrs?.view ?? {})) view.setAttribute(k, String(v));
  if (html) view.innerHTML = html;

  const scrollable = frag<Element>(`<div class="${o.noScrollable ? 'not-' : ''}scrollable">`);
  scrollable.appendChild(view);
  dialog.appendChild(scrollable);

  (o.parent ?? document.body).appendChild(dialog);

  const wrapper = new DialogWrapper(dialog, view, o);
  if (o.show && o.show === 'modal') return wrapper.showModal();
  else if (o.show) return wrapper.show();

  return wrapper;
}

// snab dialogs without an onInsert callback are shown as modal by default. use onInsert callback to handle
// this yourself
export function snabDialog(o: SnabDialogOpts): VNode {
  const ass = loadAssets(o);
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
              else {
                wrapper.showModal();
                const inputEl = view.querySelector('input');
                if (inputEl && inputEl.autofocus) inputEl.focus();
              }
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
  private actionEvents = eventJanitor();
  private dialogEvents = eventJanitor();
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
    const cancelOnInterval = (e: PointerEvent) => {
      if (Date.now() - justThen < 200) return;
      const r = dialog.getBoundingClientRect();
      if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom)
        this.close('cancel');
    };
    this.observer.observe(document.body, { childList: true, subtree: true });
    view.parentElement?.style.setProperty('---viewport-height', `${window.innerHeight}px`);
    this.dialogEvents.addListener(view, 'click', e => e.stopPropagation());

    this.dialogEvents.addListener(dialog, 'cancel', () => !this.returnValue && (this.returnValue = 'cancel'));
    this.dialogEvents.addListener(dialog, 'close', this.onRemove);
    if (!o.noCloseButton)
      this.dialogEvents.addListener(
        dialog.querySelector('.close-button-anchor > .close-button')!,
        'click',
        () => this.close('cancel'),
      );

    if (!o.noClickAway)
      setTimeout(() => {
        this.dialogEvents.addListener(document.body, 'click', cancelOnInterval);
        this.dialogEvents.addListener(dialog, 'click', cancelOnInterval);
      });
    for (const app of o.append ?? []) {
      if (app.node === view) break;
      const where = (app.where ? view.querySelector(app.where) : view)!;
      if (app.how === 'before') where.before(app.node);
      else if (app.how === 'after') where.after(app.node);
      else where.appendChild(app.node);
    }
    this.updateActions();
    this.dialogEvents.addListener(this.dialog, 'keydown', onKeydown);
  }

  get open(): boolean {
    return this.dialog.open;
  }

  get returnValue(): string {
    return this.dialog.returnValue;
  }

  set returnValue(v: string) {
    this.dialog.returnValue = v;
  }

  show = (): Promise<Dialog> => {
    this.dialog.show();
    this.autoFocus();
    return new Promise(resolve => (this.resolve = resolve));
  };

  showModal = (): Promise<Dialog> => {
    this.view.scrollTop = 0;
    this.dialog.showModal();
    this.autoFocus();
    return new Promise(resolve => (this.resolve = resolve));
  };

  close = (v?: string) => {
    this.dialog.close(v || this.returnValue || 'ok');
  };

  // attach/reattach existing listeners or provide a set of new ones
  updateActions = (actions = this.o.actions) => {
    this.actionEvents.removeAll();
    if (!actions) return;
    for (const a of Array.isArray(actions) ? actions : [actions]) {
      for (const event of Array.isArray(a.event) ? a.event : a.event ? [a.event] : ['click']) {
        for (const el of a.selector ? this.view.querySelectorAll(a.selector) : [this.view]) {
          const listener =
            'listener' in a ? (e: Event) => a.listener(e, this, a) : () => this.close(a.result);
          this.actionEvents.addListener(el, event, listener);
        }
      }
    }
  };

  private autoFocus() {
    const focus = (
      this.o.focus ? this.view.querySelector(this.o.focus) : this.view.querySelectorAll(focusQuery)[1]
    ) as HTMLElement;
    if (!focus) return;
    focus.focus();
    if (focus instanceof HTMLInputElement) focus.select();
  }
  private onRemove = () => {
    this.observer.disconnect();
    if (!this.dialog.returnValue) this.dialog.returnValue = 'cancel';
    this.restore?.focus?.focus(); // one modal at a time please
    if (this.restore?.overflow !== undefined) document.body.style.overflow = this.restore.overflow;
    this.restore = undefined;
    this.resolve?.(this);
    this.o.onClose?.(this);
    this.dialog.remove();
    for (const css of this.o.css ?? []) {
      if ('hashed' in css) site.asset.removeCssPath(css.hashed);
      else if ('url' in css) site.asset.removeCss(css.url);
    }
    this.actionEvents.removeAll();
    this.dialogEvents.removeAll();
  };
}

function loadAssets(o: DialogOpts) {
  return Promise.all([
    o.htmlUrl
      ? xhr.text(o.htmlUrl)
      : Promise.resolve(
          o.cash ? $as<HTMLElement>($(o.cash).clone().removeClass('none')).outerHTML : o.htmlText,
        ),
    ...(o.css ?? []).map(css =>
      'hashed' in css ? site.asset.loadCssPath(css.hashed) : site.asset.loadCss(css.url),
    ),
  ]);
}

function onKeydown(e: KeyboardEvent) {
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
