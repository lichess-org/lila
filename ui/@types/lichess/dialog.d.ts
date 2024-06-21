// implementation: file://./../../site/src/dialog.ts

interface Dialog {
  readonly open: boolean; // is visible?
  readonly view: HTMLElement; // your content div
  readonly returnValue?: 'ok' | 'cancel' | string; // how did we close?

  showModal(): Promise<Dialog>; // resolves on close
  show(): Promise<Dialog>; // resolves on close
  actions(actions?: Action | Action[]): void; // set new or reattach existing actions
  close(): void;
}

interface DialogOpts {
  class?: string; // zero or more classes for your view div
  css?: ({ url: string } | { hashed: string })[]; // fetches hashed or full url css
  htmlText?: string; // content, text will be used as-is
  cash?: Cash; // content, overrides htmlText, will be cloned and any 'none' class removed
  htmlUrl?: string; // content, overrides htmlText and cash, url will be xhr'd
  append?: { node: HTMLElement; where?: string; how?: 'after' | 'before' | 'child' }[]; // default 'child'
  attrs?: { dialog?: _Snabbdom.Attrs; view?: _Snabbdom.Attrs }; // optional attrs for dialog and view div
  actions?: Action | Action[]; // if present, add listeners to action buttons
  onClose?: (dialog: Dialog) => void; // called when dialog closes
  noCloseButton?: boolean; // if true, no upper right corner close button
  noClickAway?: boolean; // if true, no click-away-to-close
  noScrollable?: boolean; // if true, no scrollable div container. Fixes dialogs containing an auto-completer
}

interface DomDialogOpts extends DialogOpts {
  parent?: Element; // for centering and dom placement, otherwise fixed on document.body
  show?: 'modal' | boolean; // if not falsy, auto-show, and if 'modal' remove from dom on close
}

//snabDialog automatically shows as 'modal' on redraw unless onInsert callback is supplied
interface SnabDialogOpts extends DialogOpts {
  vnodes?: _Snabbdom.LooseVNodes; // content, overrides other content properties
  onInsert?: (dialog: Dialog) => void; // if supplied, call show() or showModal() manually
}

type ActionListener = (dialog: Dialog, action: Action, e: Event) => void;

// Actions are managed listeners / results that are easily refreshed on DOM changes
// if no event is specified, then 'click' is assumed
type Action =
  | { selector: string; event?: string | string[]; listener: ActionListener }
  | { selector: string; event?: string | string[]; result: string };

declare namespace _Snabbdom {
  type Attrs = Record<string, string | number | boolean>;
  type Key = string | number | symbol;
  type VNode = {
    sel: string | undefined;
    data: { [key: string]: any } | undefined;
    children: Array<VNode | string> | undefined;
    elm: Node | undefined;
    text: string | undefined;
    key: Key | undefined;
  };
  type LooseVNodes = (VNode | string | undefined | null | boolean)[];
}
