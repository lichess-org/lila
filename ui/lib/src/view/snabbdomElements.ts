import { type Attrs, h, type VNode, type VNodeChildren, type VNodeData } from 'snabbdom';

const MOVABLE_ATTRS = ['alt', 'href', 'src'] as const;

type MovableAttrs = (typeof MOVABLE_ATTRS)[number];
type VNodeDataExtended = VNodeData & {
  [K in MovableAttrs]?: Attrs[K];
};

type Selector = `.${string}` | `#${string}` | `[${string}`;
type TagData = VNodeDataExtended | null;
type TagFunction = {
  (selector: Selector): VNode;
  (data: TagData): VNode;
  (children: VNodeChildren): VNode;
  (selector: Selector, data: TagData): VNode;
  (selector: Selector, children: VNodeChildren): VNode;
  (selector: Selector, data: TagData, children: VNodeChildren): VNode;
  (data: TagData, children: VNodeChildren): VNode;
};

function movePropsToAttrs(data: TagData): VNodeData | null {
  if (data == null) return null;

  let next: TagData = null;
  let attrs: Attrs | null = null;

  for (const key of MOVABLE_ATTRS) {
    const value = data[key];
    if (value == null) continue;

    if (next == null) {
      attrs = { ...data.attrs };
      next = { ...data, attrs };
    }

    attrs![key] = value;
    delete next[key];
  }

  return next ?? data;
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

function normalizeArgs(a?: TagData | VNodeChildren, b?: VNodeChildren): [TagData, VNodeChildren] {
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

function makeTag(tag: keyof HTMLElementTagNameMap, defaultData?: VNodeDataExtended): TagFunction {
  function tagFn(selector: Selector): VNode;
  function tagFn(data: TagData): VNode;
  function tagFn(children: VNodeChildren): VNode;
  function tagFn(selector: Selector, data: TagData): VNode;
  function tagFn(selector: Selector, children: VNodeChildren): VNode;
  function tagFn(selector: Selector, data: TagData, children: VNodeChildren): VNode;
  function tagFn(data: TagData, children: VNodeChildren): VNode;
  function tagFn(
    a?: Selector | TagData | VNodeChildren,
    b?: TagData | VNodeChildren,
    c?: VNodeChildren,
  ): VNode {
    const [sel, data, children] = isSelector(a)
      ? [`${tag}${a}`, ...normalizeArgs(b, c)]
      : [tag, ...normalizeArgs(a, b as VNodeChildren)];

    return h(sel, movePropsToAttrs({ ...defaultData, ...data }), children);
  }

  return tagFn;
}

export const div: TagFunction = makeTag('div');
export const p: TagFunction = makeTag('p');
export const a: TagFunction = makeTag('a');
export const span: TagFunction = makeTag('span');
export const strong: TagFunction = makeTag('strong');
export const img: TagFunction = makeTag('img', { alt: '' });
export const h1: TagFunction = makeTag('h1');
export const h2: TagFunction = makeTag('h2');
