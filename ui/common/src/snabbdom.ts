import { h as snabH, VNode, VNodeData, VNodeChildElement, Hooks, Attrs } from 'snabbdom';

export type Redraw = () => void;
export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];

export function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks {
  return {
    insert: vnode => f(vnode.elm as A),
  };
}

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void, passive = true): Hooks {
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

type LooseVNode = VNodeChildElement | boolean;
type VNodeKids = LooseVNode | LooseVNode[];

function filterKids(children: VNodeKids): VNodeChildElement[] {
  return (
    typeof children === 'boolean'
      ? []
      : Array.isArray(children)
      ? children.filter(x => typeof x !== 'boolean')
      : [children]
  ) as VNodeChildElement[];
}

/* obviate need for some ternary expressions in renders.  Allows
     lh('div', [ kids && h('div', 'kid') ])
     lh('div', [ noKids || h('div', 'kid') ])
   instead of
     h('div', [ isKid ? h('div', 'kid') : null ])
   'true' values are filtered out of children array same as 'false' (for || case)
*/
export function lh(sel: string, dataOrKids?: VNodeData | null | VNodeKids, kids?: VNodeKids): VNode {
  if (kids) return snabH(sel, dataOrKids as VNodeData, filterKids(kids));
  if (!dataOrKids) return snabH(sel);
  if (Array.isArray(dataOrKids) || (typeof dataOrKids === 'object' && 'sel' in dataOrKids))
    return snabH(sel, filterKids(dataOrKids as VNodeKids));
  else return snabH(sel, dataOrKids as VNodeData);
}
