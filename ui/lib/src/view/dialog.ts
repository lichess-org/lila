/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { onInsert, hl, type VNode, type Attrs, type LooseVNodes } from './snabbdom';
import { isTouchDevice } from '@/device';
import { frag } from '@/index';
import { Janitor } from '@/event';
import * as xhr from '@/xhr';
import * as licon from '@/licon';
import { pubsub } from '@/pubsub';

export interface Dialog {
  readonly view: HTMLElement; // your content div
  readonly dialog: HTMLDialogElement; // the dialog element
  readonly returnValue?: 'ok' | 'cancel' | string; // how did we close?

  show(): Promise<Dialog>; // promise resolves on close
  updateActions(actions?: Action | Action[]): void; // set new actions or reattach existing if no args
  close(returnValue?: string): void;
}

export interface DialogOpts {
  class?: string; // classes for your view div
  css?: ({ url: string } | { hashed: string })[]; // hashed or full url css
  htmlText?: string; // content, htmlText is inserted as fragment into DOM
  cash?: Cash; // content, precedence over htmlText, cash will be cloned and any 'none' class removed
  htmlUrl?: string; // content, precedence over htmlText and cash, url will be xhr'd
  append?: { node: HTMLElement; where?: string; how?: 'after' | 'before' | 'child' }[]; // default is 'child'
  attrs?: { dialog?: Attrs; view?: Attrs }; // optional attrs for dialog and view div
  focus?: string; // query selector for focus on show
  actions?: Action | Action[]; // add listeners to controls, call updateActions() to reattach
  onClose?: (dialog: Dialog) => void; // always called when dialog closes
  noCloseButton?: boolean; // if true, no upper right corner close button
  noClickAway?: boolean; // if true, no click-away-to-close
  noScrollable?: boolean; // if true, no scrollable div container. Fixes dialogs containing an auto-completer
  modal?: boolean; // if true, show as modal (darken everything else)
}

// show is an explicit property for domDialog.
export interface DomDialogOpts extends DialogOpts {
  parent?: Element; // for centering and dom placement, otherwise fixed on document.body
  show?: boolean; // show dialog immediately after construction
}

// for snabDialog, show is inferred from !onInsert
export interface SnabDialogOpts extends DialogOpts {
  vnodes?: LooseVNodes; // content, overrides all other content properties
  onInsert?: (dialog: Dialog) => void; // if provided you must call show
}

export type ActionListener = (e: Event, dialog: Dialog, action: Action) => void;

// Actions are listeners / results for controls
// if no event is specified, then 'click' is assumed
// if no selector is given, the handler is attached to the dialog-content view div
export type Action =
  | { selector?: string; event?: string | string[]; listener: ActionListener }
  | { selector?: string; event?: string | string[]; result: string };

// when opts contains 'show', domDialog function's result promise resolves on dialog closure.
// otherwise, the promise resolves once assets are loaded and it is safe to call show
export async function domDialog(o: DomDialogOpts): Promise<Dialog> {
  const [html] = await loadAssets(o);

  const dialog = document.createElement('dialog');
  for (const [k, v] of Object.entries(o.attrs?.dialog ?? {})) dialog.setAttribute(k, String(v));
  if (isTouchDevice() && o.actions) dialog.classList.add('touch-scroll');
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

  const wrapper = new DialogWrapper(dialog, view, o, false);
  if (o.show) return wrapper.show();

  return wrapper;
}

export function snabDialog(o: SnabDialogOpts): VNode {
  const ass = loadAssets(o);
  let dialog: HTMLDialogElement;

  const dialogVNode = hl(
    `dialog${isTouchDevice() ? '.touch-scroll' : ''}`,
    {
      key: o.class ?? 'dialog',
      attrs: o.attrs?.dialog,
      hook: onInsert(el => (dialog = el as HTMLDialogElement)),
    },
    [
      o.noCloseButton ||
        hl(
          'div.close-button-anchor',
          hl('button.close-button', { attrs: { 'data-icon': licon.X, 'aria-label': i18n.site.close } }),
        ),
      hl(
        `div.${o.noScrollable ? 'not-' : ''}scrollable`,
        hl(
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
              const dlg = new DialogWrapper(dialog, view, o, true);
              if (o.onInsert) o.onInsert(dlg);
              else dlg.show();
            }),
          },
          o.vnodes,
        ),
      ),
    ],
  );
  if (!o.modal) return dialogVNode;
  return hl('div.snab-modal-mask' + (o.onInsert ? '.none' : ''), dialogVNode);
}

class DialogWrapper implements Dialog {
  private dialogEvents = new Janitor();
  private actionEvents = new Janitor();
  private resolve?: (dialog: Dialog) => void;
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
  private focusQuery = ['button', 'input', 'select', 'textarea']
    .map(sel => `${sel}:not(:disabled)`)
    .concat(['[href]', '[tabindex]', '[role="tab"]'])
    .join(',');

  constructor(
    readonly dialog: HTMLDialogElement,
    readonly view: HTMLElement,
    readonly o: DialogOpts,
    readonly isSnab: boolean,
  ) {
    const justThen = Date.now();
    const cancelOnInterval = (e: PointerEvent) => {
      if (!this.dialog.isConnected) console.trace('likely zombie dialog. Always Be Close()ing');
      if (Date.now() - justThen < 200) return;
      const r = dialog.getBoundingClientRect();
      if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom)
        this.close('cancel');
    };
    this.observer.observe(document.body, { childList: true, subtree: true });
    document.body.style.setProperty('---viewport-height', `${window.innerHeight}px`);
    this.dialogEvents.addListener(view, 'click', e => e.stopPropagation());

    this.dialogEvents.addListener(dialog, 'cancel', e => {
      if (o.noClickAway && o.noCloseButton && o.class !== 'alert') return e.preventDefault();
      if (!this.returnValue) this.returnValue = 'cancel';
    });
    this.dialogEvents.addListener(dialog, 'close', this.onRemove);
    if (!o.noCloseButton)
      this.dialogEvents.addListener(
        dialog.querySelector('.close-button-anchor > .close-button')!,
        'click',
        () => this.close('cancel'),
      );

    if (!o.noClickAway)
      setTimeout(() => {
        this.dialogEvents.addListener(document.body, 'pointerdown', cancelOnInterval);
        this.dialogEvents.addListener(dialog, 'pointerdown', cancelOnInterval);
      });
    for (const app of o.append ?? []) {
      if (app.node === view) break;
      const where = (app.where ? view.querySelector(app.where) : view)!;
      if (app.how === 'before') where.before(app.node);
      else if (app.how === 'after') where.after(app.node);
      else where.appendChild(app.node);
    }
    this.updateActions();
    this.dialogEvents.addListener(this.dialog, 'keydown', this.onKeydown);
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

  show = async (): Promise<Dialog> => {
    (await pubsub.after('polyfill.dialog'))?.(this.dialog);

    if (this.o.modal) this.view.scrollTop = 0;
    if (this.isSnab) {
      if (this.dialog.parentElement === this.dialog.closest('.snab-modal-mask'))
        this.dialog.parentElement?.classList.remove('none');
      this.dialog.show();
    } else if (this.o.modal) this.dialog.showModal();
    else this.dialog.show();
    this.autoFocus();
    return new Promise(resolve => (this.resolve = resolve));
  };

  close = (v?: string) => {
    this.dialog.close(v || this.returnValue || 'ok');
  };

  // attach/reattach existing listeners or provide a set of new ones
  updateActions = (actions = this.o.actions) => {
    this.actionEvents.cleanup();
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

  private onKeydown = (e: KeyboardEvent) => {
    if (e.key === 'Escape' && !(this.o.noCloseButton && this.o.noClickAway)) {
      this.close('cancel');
      e.preventDefault();
    } else if (e.key === 'Tab') {
      const focii = [...this.dialog.querySelectorAll<HTMLElement>(this.focusQuery)].filter(
        el => el.getAttribute('tabindex') !== '-1',
      );
      focii.sort((a, b) => {
        const ati = Number(a.getAttribute('tabindex') ?? '0');
        const bti = Number(b.getAttribute('tabindex') ?? '0');
        if (ati > 0 && (bti === 0 || ati < bti)) return -1;
        else if (bti > 0 && ati !== bti) return 1;
        else return a.compareDocumentPosition(b) & 2 ? 1 : -1;
      });
      const first = focii[0],
        last = focii[focii.length - 1],
        focus = document.activeElement as HTMLElement;

      if (focus === last && !e.shiftKey) first?.focus();
      else if (focus === first && e.shiftKey) last?.focus();
      else return;
      e.preventDefault();
    }
    e.stopPropagation();
  };

  private autoFocus() {
    const focus =
      (this.o.focus ? this.view.querySelector(this.o.focus) : this.view.querySelector('input[autofocus]')) ??
      this.view.querySelector(this.focusQuery);

    if (!(focus instanceof HTMLElement)) return;
    focus.focus();
    if (focus instanceof HTMLInputElement) focus.select();
  }

  private onRemove = () => {
    this.observer.disconnect();
    if (!this.dialog.returnValue) this.dialog.returnValue = 'cancel';
    this.resolve?.(this);
    this.o.onClose?.(this);
    if (this.dialog.parentElement?.classList.contains('snab-modal-mask')) this.dialog.parentElement.remove();
    else this.dialog.remove();
    for (const css of this.o.css ?? []) {
      if ('hashed' in css) site.asset.removeCssPath(css.hashed);
      else if ('url' in css) site.asset.removeCss(css.url);
    }
    this.actionEvents.cleanup();
    this.dialogEvents.cleanup();
  };
}

function loadAssets(o: DialogOpts) {
  return Promise.all([
    o.htmlUrl
      ? xhr.text(o.htmlUrl)
      : Promise.resolve(o.cash?.clone().removeClass('none')[0]?.outerHTML ?? o.htmlText),
    site.asset.loadCssPath('bits.dialog'),
    ...(o.css ?? []).map(css =>
      'hashed' in css ? site.asset.loadCssPath(css.hashed) : site.asset.loadCss(css.url),
    ),
  ]);
}
