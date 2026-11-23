import { attributesModule, classModule, init, type VNode } from 'snabbdom';

export interface MountApp<C> {
  el: Element;
  makeCtrl: (redraw: Redraw) => C;
  view: (ctrl: C) => VNode;
  patch?: (oldVnode: VNode | Element | DocumentFragment, vnode: VNode) => VNode;
}

let defaultPatch: ReturnType<typeof init> | undefined;

export function mountApp<C>({ el, makeCtrl, view, patch }: MountApp<C>): C {
  const applyPatch = patch ?? (defaultPatch ??= init([classModule, attributesModule]));
  let vnode: VNode | Element = el;
  const ctrl = makeCtrl(redraw);
  const blueprint = view(ctrl);
  el.innerHTML = '';
  vnode = applyPatch(vnode, blueprint);
  return ctrl;

  function redraw(): void {
    vnode = applyPatch(vnode, view(ctrl));
  }
}
