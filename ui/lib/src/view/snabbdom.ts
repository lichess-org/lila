// no side effects allowed due to re-export by index.ts

import {
  type VNode,
  type VNodeData,
  type VNodeChildElement,
  type VNodeChildren,
  type Hooks,
  type Attrs,
  type Classes,
  type JsxVNodeChildren,
  h as snabH,
  jsx as snabbdomJsx,
  thunk,
} from 'snabbdom';

import type { LiconValue } from '@/licon';
export type { Attrs, Hooks, Classes, VNode, VNodeData, VNodeChildElement, VNodeChildren };
export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];
export { thunk, snabH };

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

export function bindSubmit(f: (e: SubmitEvent) => void, redraw?: () => void): Hooks {
  return bind(
    'submit',
    e => {
      e.preventDefault();
      f(e);
    },
    redraw,
    false,
  );
}

export const dataIcon = (icon: LiconValue): Attrs => ({
  'data-icon': icon,
});

export const testId = (id: string): Attrs => (site.debug ? { 'data-testid': id } : {});

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

export function jsx(tag: string, data: VNodeData | null, ...children: JsxVNodeChildren[]): VNode {
  return snabbdomJsx(
    tag,
    data &&
      Object.entries(data).reduce<VNodeData>((normalized, [name, value]) => {
        if (name === 'attrs') {
          normalized.attrs = { ...normalized.attrs, ...value };
        } else if (['hook', 'key', 'on', 'props', 'style'].includes(name)) {
          normalized[name] = value;
        } else if (name === 'class') {
          normalized.attrs = {
            ...normalized.attrs,
            class: Array.isArray(value) ? value.filter(Boolean).join(' ') : value,
          };
        } else {
          normalized.attrs = { ...normalized.attrs, [name]: value };
        }
        return normalized;
      }, {}),
    ...children,
  );
}

export namespace jsx {
  export namespace JSX {
    export type Element = VNode;
    export type IntrinsicElements = Record<
      string,
      Omit<VNodeData, 'class'> & {
        class?: Classes | string | (string | false | null | undefined)[];
        [name: string]: any;
      }
    >;
  }
}
