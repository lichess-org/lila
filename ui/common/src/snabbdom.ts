import {
  type VNode,
  type VNodeData,
  type VNodeChildElement,
  type Hooks,
  type Attrs,
  h as snabH,
} from 'snabbdom';

export type { Attrs, VNode };
export type Redraw = () => void;
export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];

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

export type LooseVNode = VNode | string | undefined | null | boolean;
export type LooseVNodes = LooseVNode[];
export type VNodeKids = LooseVNode | LooseVNodes;

// '' may be falsy but it's a valid VNode
// 0 may be falsy but it's a valid VNode
const kidFilter = (x: VNodeData | VNodeKids): boolean => (x && x !== true) || x === '';

const filterKids = (children: VNodeKids): VNodeChildElement[] =>
  (Array.isArray(children) ? children : [children]).filter(kidFilter) as VNodeChildElement[];

/* obviate need for some ternary expressions in renders.  Allows
     looseH('div', [ kids && h('div', 'kid') ])
   instead of
     h('div', [ isKid ? h('div', 'kid') : null ])
   'true' values are filtered out of children array same as 'false' (for || case)
*/
export function looseH(sel: string, dataOrKids?: VNodeData | VNodeKids, kids?: VNodeKids): VNode {
  if (kids) return snabH(sel, dataOrKids as VNodeData, filterKids(kids));
  if (!kidFilter(dataOrKids)) return snabH(sel);
  if (Array.isArray(dataOrKids) || (typeof dataOrKids === 'object' && 'sel' in dataOrKids!))
    return snabH(sel, filterKids(dataOrKids as VNodeKids));
  else return snabH(sel, dataOrKids as VNodeData);
}
