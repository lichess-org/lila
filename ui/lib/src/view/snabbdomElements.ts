import {
  type Attrs,
  h,
  type VNode,
  type VNodeChildElement,
  type VNodeChildren,
  type VNodeData,
} from 'snabbdom';

import type { LiconValue } from '@/licon';

type RemoveIndexSignature<T> = {
  [K in keyof T as string extends K ? never : number extends K ? never : symbol extends K ? never : K]: T[K];
};
type StrictVNodeData = RemoveIndexSignature<VNodeData>;
type VNodeDataExtended = StrictVNodeData & Record<string, unknown>;
type NestedVNodeChildren = VNodeChildElement | NestedVNodeChildren[];

type Selector = `.${string}` | `#${string}` | `[${string}`;
type TagData = VNodeDataExtended | null;
type TagFunction = {
  (): VNode;
  (selector: Selector): VNode;
  (data: TagData): VNode;
  (children: NestedVNodeChildren): VNode;
  (selector: Selector, data: TagData): VNode;
  (selector: Selector, children: NestedVNodeChildren): VNode;
  (selector: Selector, data: TagData, children: NestedVNodeChildren): VNode;
  (data: TagData, children: NestedVNodeChildren): VNode;
};
type TagFactory<Args extends unknown[]> = (...args: Args) => TagFunction;

const VNODE_DATA_KEYS = new Set<keyof StrictVNodeData>([
  'props',
  'attrs',
  'class',
  'style',
  'dataset',
  'on',
  'attachData',
  'hook',
  'key',
  'ns',
  'fn',
  'args',
]);

const MAX_CHILDREN_NEST_LEVEL = 5;

function movePropsToAttrs(data: TagData): VNodeData | null {
  if (data == null) return null;

  let next: TagData = null;
  let attrs: Attrs | null = null;

  for (const key of Object.keys(data)) {
    if (VNODE_DATA_KEYS.has(key as keyof StrictVNodeData)) continue;

    const value = data[key];
    if (!isAttrValue(value)) continue;

    if (next === null || attrs === null) {
      attrs = { ...data.attrs };
      next = { ...data, attrs };
    }

    attrs[key] = value;
    delete next[key];
  }

  return next ?? data;
}

function isAttrValue(value: unknown): value is Attrs[keyof Attrs] {
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean';
}

function isVNode(value: unknown): value is VNode {
  return (
    value !== null && typeof value === 'object' && ('sel' in value || 'text' in value || 'children' in value)
  );
}

function isVNodeData(value: unknown): value is VNodeDataExtended {
  return value !== null && typeof value === 'object' && !Array.isArray(value) && !isVNode(value);
}

function isSelector(value: unknown): value is Selector {
  return (
    typeof value === 'string' && (value.startsWith('.') || value.startsWith('#') || value.startsWith('['))
  );
}

function normalizeArgs(
  a?: TagData | NestedVNodeChildren,
  b?: NestedVNodeChildren,
): [TagData, NestedVNodeChildren] {
  if (b !== undefined) {
    if (!isVNodeData(a) && a !== null) {
      throw new TypeError(
        'Invalid arguments: when passing 2 arguments, the first must be VNodeDataExtended or null',
      );
    }
    return [a, b];
  }

  if (isVNodeData(a) || a === null) {
    return [a, []];
  }

  return [{}, a ?? []];
}

export function makeTag(tag: keyof HTMLElementTagNameMap, defaultData?: VNodeDataExtended): TagFunction {
  return makeExoticTag(tag, defaultData);
}

export function makeExoticTag(tag: string, defaultData?: VNodeDataExtended): TagFunction {
  function tagFn(): VNode;
  function tagFn(selector: Selector): VNode;
  function tagFn(data: TagData): VNode;
  function tagFn(children: NestedVNodeChildren): VNode;
  function tagFn(selector: Selector, data: TagData): VNode;
  function tagFn(selector: Selector, children: NestedVNodeChildren): VNode;
  function tagFn(selector: Selector, data: TagData, children: NestedVNodeChildren): VNode;
  function tagFn(data: TagData, children: NestedVNodeChildren): VNode;
  function tagFn(
    a?: Selector | TagData | NestedVNodeChildren,
    b?: TagData | NestedVNodeChildren,
    c?: NestedVNodeChildren,
  ): VNode {
    const [sel, data, children] = isSelector(a)
      ? [`${tag}${a}`, ...normalizeArgs(b, c)]
      : [tag, ...normalizeArgs(a, b as NestedVNodeChildren)];

    return h(
      sel,
      movePropsToAttrs({ ...defaultData, ...data }),
      Array.isArray(children) ? (children.flat(MAX_CHILDREN_NEST_LEVEL) as VNodeChildren) : children,
    );
  }

  return tagFn;
}

export const div: TagFunction = makeTag('div');
export const p: TagFunction = makeTag('p');
export const button: TagFunction = makeTag('button');
export const span: TagFunction = makeTag('span');
export const strong: TagFunction = makeTag('strong');
export const time: TagFunction = makeTag('time');
export const label: TagFunction = makeTag('label');
export const select: TagFunction = makeTag('select');
export const option: TagFunction = makeTag('option');
export const main: TagFunction = makeTag('main');
export const form: TagFunction = makeTag('form');
export const h1: TagFunction = makeTag('h1');
export const h2: TagFunction = makeTag('h2');

export const table: TagFunction = makeTag('table');
export const thead: TagFunction = makeTag('thead');
export const tbody: TagFunction = makeTag('tbody');
export const tr: TagFunction = makeTag('tr');
export const th: TagFunction = makeTag('th');
export const td: TagFunction = makeTag('td');

export const a: TagFactory<[href: string]> = href => makeTag('a', { href });
export const img: TagFactory<[src: string, alt: string]> = (src, alt) => makeTag('img', { alt, src });
export const input: TagFactory<[type: HTMLInputElement['type']]> = (type = 'text') =>
  makeTag('input', { type });
export const optgroup: TagFactory<[label: string]> = label => makeTag('optgroup', { label });

export const icon: TagFactory<[icon: LiconValue]> = icon => makeExoticTag('icon', { 'data-icon': icon });
