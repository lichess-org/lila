// no side effects allowed due to re-export by index.ts

import {
  type VNode,
  type VNodeData,
  type VNodeChildElement,
  type VNodeChildren,
  type Hooks,
  type Attrs,
  type Classes,
  h as snabH,
  thunk,
} from 'snabbdom';

export type { Attrs, Hooks, Classes, VNode, VNodeData, VNodeChildElement, VNodeChildren };
export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];
export { thunk };

export function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks {
  return {
    insert: vnode => f(vnode.elm as A),
  };
}

export function bind<K extends keyof GlobalEventHandlersEventMap>(
  eventName: K,
  f: (ev: GlobalEventHandlersEventMap[K]) => any,
  redraw?: Redraw,
  passive = true,
): Hooks {
  return onInsert(el =>
    el.addEventListener(
      eventName,
      e => {
        const res = f(e);
        if (res === false && !passive) e.preventDefault();
        redraw?.();
        return res;
      },
      { passive },
    ),
  );
}

export const bindNonPassive = <K extends keyof GlobalEventHandlersEventMap>(
  eventName: K,
  f: (ev: GlobalEventHandlersEventMap[K]) => any,
  redraw?: Redraw,
): Hooks => bind(eventName, f, redraw, false);

export function bindSubmit(f: (e: SubmitEvent) => unknown, redraw?: () => void): Hooks {
  return bind('submit', e => (e.preventDefault(), f(e)), redraw, false);
}

export const dataIcon = (icon: string): Attrs => ({
  'data-icon': icon,
});

export const iconTag = (icon: string): VNode => snabH('i', { attrs: dataIcon(icon) });

export type LooseVNode = VNodeChildElement | boolean;
export type LooseVNodes = LooseVNode | LooseVNodes[];

// '' may be falsy but it's a valid VNode
// 0 may be falsy but it's a valid VNode
const kidFilter = (x: VNodeData | LooseVNodes): boolean => (x && x !== true) || x === '' || x === 0;

const filterKids = (children: LooseVNodes): VNodeChildElement[] => {
  const flatKids: LooseVNode[] = [];
  flattenKids(children, flatKids);
  return flatKids.filter(kidFilter) as VNodeChildElement[];
};

// strip boolean results and flatten arrays in renders.  Allows
//   hl('div', isDivEmpty || [ 'foo', fooHasBar && [ 'has', 'bar' ])
export function hl(sel: string, dataOrKids?: VNodeData | LooseVNodes, kids?: LooseVNodes): VNode {
  if (kids) return snabH(sel, dataOrKids as VNodeData, filterKids(kids));
  if (!kidFilter(dataOrKids)) return snabH(sel);
  if (Array.isArray(dataOrKids) || (typeof dataOrKids === 'object' && 'sel' in dataOrKids!))
    return snabH(sel, filterKids(dataOrKids as LooseVNodes));
  else return snabH(sel, dataOrKids as VNodeData);
}

// for deep trees i think it's more efficient to flatten arrays here than to spread them in renders.
// but we're mostly after cleaner syntax
const flattenKids = (maybeArray: LooseVNodes, out: LooseVNode[]) => {
  if (Array.isArray(maybeArray)) for (const el of maybeArray) flattenKids(el, out);
  else out.push(maybeArray);
};

export const noTrans: (s: string) => VNode = s => snabH('span', { attrs: { lang: 'en' } }, s);

export const requiresI18n = <Cat extends keyof I18n>(
  catalog: Cat,
  redraw: Redraw,
  render: (cat: I18n[Cat]) => VNode,
): VNode => {
  if (!window.i18n[catalog]) {
    site.asset.loadI18n(catalog).then(redraw);
    return snabH('span', '...');
  }
  return render(window.i18n[catalog]);
};
