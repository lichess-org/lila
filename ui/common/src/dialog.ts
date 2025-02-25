import { onInsert, looseH as h, type VNode, type Attrs, type LooseVNodes } from './snabbdom';
import { isTouchDevice } from './device';
import { escapeHtml, frag, $as } from './common';
import { Janitor } from './event';
import * as xhr from './xhr';
import * as licon from './licon';
import { pubsub } from './pubsub';

let dialogPolyfill: { registerDialog: (dialog: HTMLDialogElement) => void };

export interface Dialog {
  readonly open: boolean; // is visible?
  readonly view: HTMLElement; // your content div
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
  onInsert?: (dialog: Dialog) => void; // if provided you must also call show
}

export type ActionListener = (e: Event, dialog: Dialog, action: Action) => void;

// Actions are listeners / results for controls
// if no event is specified, then 'click' is assumed
// if no selector is given, the handler is attached to the dialog-content view div
export type Action =
  | { selector?: string; event?: string | string[]; listener: ActionListener }
  | { selector?: string; event?: string | string[]; result: string };

// Safari versions before 15.4 need a polyfill for dialog
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
    modal: true,
    show: true,
  });
}

export async function alerts(msgs: string[]): Promise<void> {
  for (const msg of msgs) await alert(msg);
}

// non-blocking window.confirm-alike
export async function confirm(
  msg: string,
  yes: string = i18n.site.yes,
  no: string = i18n.site.no,
): Promise<boolean> {
  return (
    (
      await domDialog({
        htmlText:
          `<div>${escapeHtmlAddBreaks(msg)}</div>` +
          `<span><button class="button button-empty no">${no}</button>` +
          `<button class="button yes">${yes}</button></span>`,
        class: 'alert',
        noCloseButton: true,
        noClickAway: true,
        modal: true,
        show: true,
        focus: '.yes',
        actions: [
          { selector: '.yes', result: 'yes' },
          { selector: '.no', result: 'no' },
        ],
      })
    ).returnValue === 'yes'
  );
}

// non-blocking window.prompt-alike
export async function prompt(msg: string, def: string = ''): Promise<string | null> {
  const res = await domDialog({
    htmlText:
      `<div>${escapeHtmlAddBreaks(msg)}</div>` +
      `<input type="text" value="${escapeHtml(def)}" />` +
      `<span><button class="button button-empty cancel">${i18n.site.cancel}</button>` +
      `<button class="button ok">${i18n.site.ok}</button></span>`,
    class: 'alert',
    noCloseButton: true,
    noClickAway: true,
    modal: true,
    show: true,
    focus: 'input',
    actions: [
      { selector: '.ok', result: 'ok' },
      { selector: '.cancel', result: 'cancel' },
      {
        selector: 'input',
        event: 'keydown',
        listener: (e: KeyboardEvent, dlg) => {
          if (e.key !== 'Enter' && e.key !== 'Escape') return;
          e.preventDefault();
          if (e.key === 'Enter') dlg.close('ok');
          else if (e.key === 'Escape') dlg.close('cancel');
        },
      },
    ],
  });
  return res.returnValue === 'ok' ? res.view.querySelector('input')!.value : null;
}

// when opts contains 'show', domDialog function's result promise resolves on dialog closure.
// otherwise, the promise resolves once assets are loaded and it is safe to call show
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

  const wrapper = new DialogWrapper(dialog, view, o, false);
  if (o.show) return wrapper.show();

  return wrapper;
}

export function snabDialog(o: SnabDialogOpts): VNode {
  const ass = loadAssets(o);
  let dialog: HTMLDialogElement;

  const dialogVNode = h(
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
  return h('div.snab-modal-mask' + (o.onInsert ? '.none' : ''), dialogVNode);
}

class DialogWrapper implements Dialog {
  private resolve?: (dialog: Dialog) => void;
  private actionEvents = new Janitor();
  private dialogEvents = new Janitor();
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
    readonly isSnab: boolean,
  ) {
    if (dialogPolyfill) dialogPolyfill.registerDialog(dialog); // ios < 15.4

    const justThen = Date.now();
    const cancelOnInterval = (e: PointerEvent) => {
      if (Date.now() - justThen < 200) return; // removed isConnected() check. we catch leaks this way
      const r = dialog.getBoundingClientRect();
      if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom)
        this.close('cancel');
    };
    this.observer.observe(document.body, { childList: true, subtree: true });
    view.parentElement?.style.setProperty('---viewport-height', `${window.innerHeight}px`);
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

  show = (): Promise<Dialog> => {
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
  };

  private autoFocus() {
    const focus =
      (this.o.focus ? this.view.querySelector(this.o.focus) : this.view.querySelector('input[autofocus]')) ??
      this.view.querySelector(focusQuery);

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
      : Promise.resolve(
          o.cash ? $as<HTMLElement>($(o.cash).clone().removeClass('none')).outerHTML : o.htmlText,
        ),
    ...(o.css ?? []).map(css =>
      'hashed' in css ? site.asset.loadCssPath(css.hashed) : site.asset.loadCss(css.url),
    ),
  ]);
}

function escapeHtmlAddBreaks(s: string) {
  return escapeHtml(s).replace(/\n/g, '<br>');
}

function onResize() {
  // ios safari vh behavior workaround
  $('dialog > div.scrollable').css('---viewport-height', `${window.innerHeight}px`);
}

const focusQuery = ['button', 'input', 'select', 'textarea']
  .map(sel => `${sel}:not(:disabled)`)
  .concat(['[href]', '[tabindex="0"]', '[role="tab"]'])
  .join(',');
