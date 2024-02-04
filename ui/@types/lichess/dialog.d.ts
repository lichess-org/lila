// implementation: file://./../../site/src/component/dialog.ts

interface Dialog {
  readonly open: boolean; // is visible?
  readonly view: HTMLElement; // your content div
  readonly returnValue?: 'ok' | 'cancel' | string; // how did we close?

  showModal(): Promise<Dialog>; // resolves on close
  show(): Promise<Dialog>; // resolves on close
  close(): void;
}

interface DialogOpts {
  class?: string; // zero or more classes for your view div
  css?: ({ url: string } | { themed: string })[]; // fetches themed or full url css
  htmlText?: string; // content, text will be used as-is
  cash?: Cash; // content, overrides htmlText, will be cloned and any 'none' class removed
  htmlUrl?: string; // content, overrides htmlText and cash, url will be xhr'd
  append?: { node: HTMLElement; selector?: string }[]; // appended to view or selected parents
  attrs?: { dialog?: _Snabbdom.Attrs; view?: _Snabbdom.Attrs }; // optional attrs for dialog and view div
  action?: Action | Action[]; // if present, add handlers to action buttons
  onClose?: (dialog: Dialog) => void; // called when dialog closes
  noCloseButton?: boolean; // if true, no upper right corner close button
  noClickAway?: boolean; // if true, no click-away-to-close
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

// Action can be any "clickable" client button, usually to dismiss the dialog
interface Action {
  selector: string; // selector, click handler will be installed
  action?: string | ((dialog: Dialog, action: Action) => void);
  // if action not provided, just close
  // if string, given value will set dialog.returnValue and dialog is closed on click
  // if function, it will be called on click and YOU must close the dialog
}

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
