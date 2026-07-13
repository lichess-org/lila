// no side effects allowed due to re-export by index.ts

import hyperx, { type HtmlTemplate } from 'hyperx';
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

import type { LiconValue } from '@/licon';
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

export const iconTag = (icon: LiconValue, attrs?: Attrs & { cls?: string }): VNode => {
  let sel = 'icon';
  if (attrs?.cls) {
    sel += '.' + attrs.cls;
    delete attrs.cls;
  }
  return snabH(sel, { attrs: { ...attrs, ...dataIcon(icon) } });
};

export const iconCls = (icon: LiconValue, cls: string): VNode =>
  snabH('icon.' + cls, { attrs: dataIcon(icon) });

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

/**
 * HTML template support based on Snabby: https://github.com/mreinstein/snabby
 */

const BOOLEAN_ATTRIBUTES = new Set([
  'allowfullscreen',
  'async',
  'autofocus',
  'checked',
  'compact',
  'declare',
  'default',
  'defer',
  'disabled',
  'formnovalidate',
  'hidden',
  'inert',
  'ismap',
  'itemscope',
  'multiple',
  'multiple',
  'muted',
  'nohref',
  'noresize',
  'noshade',
  'novalidate',
  'nowrap',
  'open',
  'readonly',
  'required',
  'reversed',
  'seamless',
  'selected',
  'sortable',
  'truespeed',
  'typemustmatch',
  'contenteditable',
  'spellcheck',
]);

function createVNode(tag: string, properties: Record<string, any>, content: any) {
  if (tag === '!--') return snabH('!', properties.comment);

  if (Array.isArray(content) && content.length) {
    content = content.length === 1 ? content[0] : content.flat();
  }

  const property = Object.keys(properties);
  if (!property?.length) return snabH(tag, content);

  const data = {} as Record<string, any>;
  let attrs;

  for (let i = 0; i < property.length; i++) {
    const name = property[i];
    let value = properties[name];

    if (name.startsWith('@')) {
      const parts = name.slice(1).split(':');

      if ((parts[0] !== 'attrs' || BOOLEAN_ATTRIBUTES.has(parts[1])) && value === 'false') {
        value = false;
      }

      let obj = data;
      for (let p = 0; p < parts.length - 1; p++) {
        const part = parts[p];
        obj = obj[part] || (obj[part] = {});
      }
      obj[parts[parts.length - 1]] = value;
      continue;
    }

    if (!attrs) attrs ||= data.attrs ||= {};

    if (name === 'class') {
      // TODO: support mixed arrays i.e. ['cls', { 'active': true }]
      if (value && typeof value === 'object' && !Array.isArray(value)) {
        data.class = value;
      } else {
        attrs.class = Array.isArray(value) ? value.join(' ') : value;
      }
      continue;
    }

    if (BOOLEAN_ATTRIBUTES.has(name) && value === 'false') value = false;

    attrs[name] = value;
  }

  return snabH(tag, data, content);
}

export const html: HtmlTemplate = hyperx(createVNode, {
  comments: true,
  attrToProp: false,
});
