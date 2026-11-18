import { attributesModule, classModule, init, type VNode } from 'snabbdom';

const defaultPatch = init([classModule, attributesModule]);

export interface MountApp<C> {
  el: Element;
  makeCtrl: (redraw: Redraw) => C;
  view: (ctrl: C) => VNode;
  patch?: (oldVnode: VNode | Element | DocumentFragment, vnode: VNode) => VNode;
}

export function mountApp<C>({ el, makeCtrl, view, patch = defaultPatch }: MountApp<C>): C {
  let vnode: VNode | Element = el;
  const ctrl = makeCtrl(redraw);
  vnode = patch(vnode, view(ctrl));
  return ctrl;

  function redraw(): void {
    vnode = patch(vnode, view(ctrl));
  }
}
