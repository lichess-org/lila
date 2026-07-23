import { isTouchDevice } from '@/device';
import { Janitor } from '@/event';
import { frag } from '@/index';
import { licon } from '@/licon';
import { pubsub } from '@/pubsub';
import * as xhr from '@/xhr';

import { onInsert, hl, type VNode, type Attrs, type LooseVNodes } from './snabbdom';

export interface Dialog<Ctx = undefined> {
  ctx: Ctx;
  readonly view: HTMLElement; // your content div
  readonly dialog: HTMLDialogElement; // the dialog element
  readonly returnValue?: 'ok' | 'cancel' | string; // how did we close?

  show(): Promise<Dialog<Ctx>>; // promise resolves on close
  updateActions(actions?: Action<Ctx> | Action<Ctx>[]): void; // set new actions or reattach existing if no args
  close(returnValue?: string): void;
}

export interface DialogOpts<Ctx = undefined> {
  ctx?: Ctx;
  class?: string; // classes for your view div
  css?: ({ url: string } | { hashed: string })[]; // hashed or full url css
  htmlText?: string; // content, htmlText is inserted as fragment into DOM
  cash?: Cash; // content, precedence over htmlText, cash will be cloned and any 'none' class removed
  htmlUrl?: string; // content, precedence over htmlText and cash, url will be xhr'd
  insert?: { node: Node; selector?: string; position?: 'child' | 'before' | 'after' }[]; // default is 'child'
  attrs?: { dialog?: Attrs; view?: Attrs }; // optional attrs for dialog and view div
  focus?: string; // query selector for focus on show
  actions?: Action<Ctx> | Action<Ctx>[]; // add listeners to controls, call updateActions() to reattach
  onShow?: (dialog: Dialog<Ctx>) => void; // called after dialog is shown
  onClose?: (dialog: Dialog<Ctx>) => void; // always called when dialog closes
  noCloseButton?: boolean; // if true, no upper right corner close button
  noScrollable?: boolean; // if true, no scrollable div container. Fixes dialogs containing an auto-completer
  modal?: boolean; // if true, show as modal (darken everything else)
  easyClose?: 'anyClick' | 'clickOutside';
}

// show is an explicit property for domDialog.
export interface DomDialogOpts<Ctx = undefined> extends DialogOpts<Ctx> {
  parent?: Element; // for centering and dom placement, otherwise fixed on document.body
  show?: boolean; // show dialog immediately after construction
}

export interface SnabDialogOpts<Ctx = undefined> extends DialogOpts<Ctx> {
  vnodes?: LooseVNodes; // content, overrides all other content properties
  onInsert?: (dialog: Dialog<Ctx>) => void; // if provided you must call show
}

export type ActionListener<T extends Event = Event, Ctx = undefined> = (
  e: T,
  dialog: Dialog<Ctx>,
  action: Action<Ctx>,
) => void;

// Actions are listeners / results for controls
// if no event is specified, then 'click' is assumed
// if no selector is given, the handler is attached to the dialog-content view div
export type Action<Ctx = undefined> =
  | { selector?: string; event?: string | string[]; listener: ActionListener<any, Ctx> }
  | { selector?: string; event?: string | string[]; result: string };

// when opts contains 'show', domDialog function's result promise resolves on dialog closure.
// otherwise, the promise resolves once assets are loaded and it is safe to call show
export async function domDialog<Ctx = undefined>(o: DomDialogOpts<Ctx>): Promise<Dialog<Ctx>> {
  const html = await loadAssets(o);

  const dialog = document.createElement('dialog');
  for (const [k, v] of Object.entries(o.attrs?.dialog ?? {})) dialog.setAttribute(k, String(v));
  if (isTouchDevice()) dialog.classList.add('touch-scroll');
  if (o.parent) dialog.style.position = 'absolute';

  if (!o.noCloseButton) {
    const anchor = frag<Element>('<div class="close-button-anchor">');
    anchor.innerHTML = `<button class="close-button" aria-label="Close" data-icon="${licon.X}">`;
    dialog.appendChild(anchor);
  }

  const view = document.createElement('div');
  view.classList.add('dialog-content');
  if (o.class) view.classList.add(...o.class.split(/[. ]/).filter(Boolean));
  for (const [k, v] of Object.entries(o.attrs?.view ?? {})) view.setAttribute(k, String(v));
  if (html) view.innerHTML = html;

  const scrollable = frag<Element>(`<div class="${o.noScrollable ? 'not-' : ''}scrollable">`);
  scrollable.appendChild(view);
  dialog.appendChild(scrollable);

  (o.parent ?? document.body).appendChild(dialog);

  const wrapper = new DialogWrapper<Ctx>(dialog, view, o);
  return o.show ? wrapper.show() : wrapper;
}

export function snabDialog<Ctx = undefined>(o: SnabDialogOpts<Ctx>): VNode {
  let dialog: HTMLDialogElement;
  const classes = o.class?.split(/[. ]/).filter(Boolean) ?? [];
  const dialogVNode = hl(
    'dialog',
    {
      class: { 'touch-scroll': isTouchDevice() },
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
        'div',
        { class: { scrollable: !o.noScrollable } },
        hl(
          'div.dialog-content',
          {
            class: Object.fromEntries(classes.map(c => [c, true])),
            attrs: o.attrs?.view,
            hook: onInsert(async view => {
              const html = await loadAssets(o);
              if (!o.vnodes && html) view.innerHTML = html;
              const dlg = new DialogWrapper<Ctx>(dialog, view, o);
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
  return hl('div.snab-modal-mask', { class: { none: Boolean(o.onInsert) } }, dialogVNode);
}

const easyCloseHandler = new (class {
  private stack: DialogWrapper[] = [];

  push(dlg: DialogWrapper<any>) {
    if (!dlg.o.easyClose) return;
    if (this.stack.length === 0)
      document.addEventListener('pointerdown', this.pointerdown, { capture: true });
    this.stack.push(dlg);
  }

  remove(dlg: DialogWrapper<any>): void {
    this.stack = this.stack.filter(d => d !== dlg);
    if (this.stack.length === 0)
      document.removeEventListener('pointerdown', this.pointerdown, { capture: true });
  }

  private readonly pointerdown = (e: PointerEvent) => {
    if (!this.top?.o.easyClose) return;

    if (this.top.o.easyClose === 'clickOutside') {
      const { clientX: x, clientY: y } = e;
      const bounds = this.top.dialog.getBoundingClientRect();
      if (x >= bounds.left && x <= bounds.right && y >= bounds.top && y <= bounds.bottom) return;
    }
    if (e.pointerType === 'touch' || !this.top.o.modal)
      window.addEventListener(
        'click',
        e => {
          e.preventDefault();
          e.stopImmediatePropagation();
        },
        { once: true, capture: true },
      );
    this.top.close('cancel');
    e.stopPropagation();
  };

  private get top(): DialogWrapper | undefined {
    return this.stack[this.stack.length - 1];
  }
})();

class DialogWrapper<Ctx = undefined> implements Dialog<Ctx> {
  private readonly dialogEvents = new Janitor();
  private readonly actionEvents = new Janitor();
  private resolve?: (dialog: Dialog<Ctx>) => void;
  private readonly observer: MutationObserver = new MutationObserver(list => {
    for (const m of list)
      if (m.type === 'childList')
        for (const n of m.removedNodes) {
          if (n === this.dialog) {
            this.onRemove();
            return;
          }
        }
  });
  private readonly focusQuery =
    'button, input, select, textarea, [href], [tabindex], [role="tab"], [role="button"], [role="link"]';

  constructor(
    readonly dialog: HTMLDialogElement,
    readonly view: HTMLElement,
    readonly o: DialogOpts<Ctx>,
    public ctx: Ctx = o.ctx as Ctx,
  ) {
    this.observer.observe(document.body, { childList: true, subtree: true });
    document.body.style.setProperty('---viewport-height', `${window.innerHeight}px`);
    this.dialogEvents.addListener(view, 'click', e => e.stopPropagation());
    this.dialogEvents.addListener(dialog, 'cancel', e => {
      if (!o.easyClose && o.noCloseButton && o.class !== 'alert') return e.preventDefault();
      if (!this.dialog.returnValue) this.dialog.returnValue = 'cancel';
    });
    this.dialogEvents.addListener(dialog, 'close', this.onRemove);
    if (!o.noCloseButton)
      this.dialogEvents.addListener(
        dialog.querySelector<HTMLButtonElement>('.close-button-anchor > .close-button')!,
        'click',
        () => this.close('cancel'),
      );
    for (const app of o.insert ?? []) {
      if (app.node === view) break;
      const target = (app.selector ? view.querySelector(app.selector) : view)!;
      if (app.position === 'before') target.before(app.node);
      else if (app.position === 'after') target.after(app.node);
      else target.appendChild(app.node);
    }
    this.updateActions();
    this.dialogEvents.addListener(this.dialog, 'keydown', this.onKeydown);
  }

  get returnValue(): string {
    return this.dialog.returnValue;
  }

  show = async (): Promise<Dialog<Ctx>> => {
    (await pubsub.after('polyfill.dialog'))?.(this.dialog);
    const snabModal = this.dialog.parentElement === this.dialog.closest('.snab-modal-mask');
    if (this.o.modal) this.view.scrollTop = 0;
    if (snabModal) this.dialog.parentElement?.classList.remove('none');

    if (this.o.modal && !snabModal) this.dialog.showModal();
    else this.dialog.show();

    easyCloseHandler.push(this);
    this.dialogEvents.addCleanupTask(() => easyCloseHandler.remove(this));

    this.autoFocus();
    this.o.onShow?.(this);
    return new Promise(resolve => (this.resolve = resolve));
  };

  close = (v?: string) => {
    this.dialog.close(v || this.returnValue || 'ok');
  };

  updateActions = (actions = this.o.actions) => {
    this.actionEvents.cleanup();
    this.o.actions = actions;
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

  private readonly onKeydown = (e: KeyboardEvent) => {
    if (e.key === 'Escape' && (this.o.easyClose || !this.o.noCloseButton)) {
      this.close('cancel');
      e.preventDefault();
    } else if (e.key === 'Tab') {
      const focii = [...this.dialog.querySelectorAll<HTMLElement>(this.focusQuery)].filter(
        el =>
          el.tabIndex !== -1 &&
          el.checkVisibility({ visibilityProperty: true }) &&
          !el.matches(':disabled') &&
          !el.closest('[inert]'),
      );
      focii.sort((a, b) => {
        const ati = Number(a.getAttribute('tabindex') ?? '0');
        const bti = Number(b.getAttribute('tabindex') ?? '0');
        if (ati > 0 && (bti === 0 || ati < bti)) return -1;
        else if (bti > 0 && ati !== bti) return 1;
        else return a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_PRECEDING ? 1 : -1;
      });
      const first = focii[0],
        last = focii[focii.length - 1],
        focus = document.activeElement as HTMLElement;
      console.log(focii.map(el => el.classList.toString()));
      if (focus === last && !e.shiftKey) first?.focus();
      else if (focus === first && e.shiftKey) last?.focus();
      else return;
      e.preventDefault();
    }

    if (['Escape', 'Tab'].includes(e.key)) e.stopPropagation(); // trap 'Enter' for modals here or?
  };

  private autoFocus() {
    const focus =
      (this.o.focus ? this.view.querySelector(this.o.focus) : this.view.querySelector('input[autofocus]')) ??
      this.view.querySelector(this.focusQuery);

    if (!(focus instanceof HTMLElement)) return;
    focus.focus();
    if (focus instanceof HTMLInputElement) focus.select();
  }

  private readonly onRemove = () => {
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

async function loadAssets<Ctx>(o: DialogOpts<Ctx>): Promise<string> {
  const results = await Promise.allSettled([
    o.htmlUrl
      ? xhr.text(o.htmlUrl)
      : Promise.resolve(o.cash?.clone().removeClass('none')[0]?.outerHTML ?? o.htmlText),
    site.asset.loadCssPath('bits.dialog'),
    ...(o.css ?? []).map(css =>
      'hashed' in css ? site.asset.loadCssPath(css.hashed) : site.asset.loadCss(css.url),
    ),
  ]);
  return (results[0]?.status === 'fulfilled' && results[0].value) || '';
}
