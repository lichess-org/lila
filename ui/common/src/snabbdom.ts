import { h as snabH, VNode, VNodeData, VNodeChildElement, Hooks, Attrs } from 'snabbdom';

export type Redraw = () => void;
export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks {
  return {
    insert: vnode => f(vnode.elm as A),
  };
}

export function bind(eventName: string, f: (e: Event) => any, redraw?: Redraw, passive = true): Hooks {
  return onInsert(el =>
    el.addEventListener(
      eventName,
      e => {
        const res = f(e);
        if (res === false) e.preventDefault();
        redraw?.();
        return res;
      },
      { passive },
    ),
  );
}

export const bindNonPassive = (eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks =>
  bind(eventName, f, redraw, false);

export function bindSubmit(f: (e: Event) => unknown, redraw?: () => void): Hooks {
  return bind('submit', e => (e.preventDefault(), f(e)), redraw, false);
}

export const dataIcon = (icon: string): Attrs => ({
  'data-icon': icon,
});

export const iconTag = (icon: string) => snabH('i', { attrs: dataIcon(icon) });

export type LooseVNodes = (MaybeVNode | boolean)[];
type LooseVNode = VNodeChildElement | boolean;
type VNodeKids = LooseVNode | LooseVNode[];

// '' may be falsy but it's a valid VNode
// 0 may be falsy but it's a valid VNode
const kidFilter = (x: any): boolean => (x && x !== true) || x === '' || x === 0;

const filterKids = (children: VNodeKids): VNodeChildElement[] =>
  (Array.isArray(children) ? children : [children]).filter(kidFilter) as VNodeChildElement[];

/* obviate need for some ternary expressions in renders.  Allows
     looseH('div', [ kids && h('div', 'kid') ])
   instead of
     h('div', [ isKid ? h('div', 'kid') : null ])
   'true' values are filtered out of children array same as 'false' (for || case)
*/
export function looseH(sel: string, dataOrKids?: VNodeData | null | VNodeKids, kids?: VNodeKids): VNode {
  if (kids) return snabH(sel, dataOrKids as VNodeData, filterKids(kids));
  if (!dataOrKids) return snabH(sel);
  if (Array.isArray(dataOrKids) || (typeof dataOrKids === 'object' && 'sel' in dataOrKids))
    return snabH(sel, filterKids(dataOrKids as VNodeKids));
  else return snabH(sel, dataOrKids as VNodeData);
}
