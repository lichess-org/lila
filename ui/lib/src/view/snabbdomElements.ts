import { type VNode, type VNodeData, type VNodeChildren, h, type Attrs } from 'snabbdom';

type TagData = VNodeDataExtended | null;
type TagFunction = {
  (selector: string): VNode;
  (data: TagData): VNode;
  (children: VNodeChildren): VNode;
  (selector: string, data: TagData): VNode;
  (selector: string, children: VNodeChildren): VNode;
  (selector: string, data: TagData, children: VNodeChildren): VNode;
  (data: TagData, children: VNodeChildren): VNode;
};

type MovableAttrs = (typeof MOVABLE_ATTRS)[number];
type VNodeDataExtended = VNodeData & {
  [K in MovableAttrs]?: Attrs[K];
};

const MOVABLE_ATTRS = ['alt', 'href', 'src'] as const;

function movePropsToAttrs(data: TagData): VNodeData | null {
  if (!data) return data;

  let next: TagData = null;

  for (const key of MOVABLE_ATTRS) {
    const value = data[key];
    if (value == null) continue;

    if (next == null) {
      next = {
        ...data,
        attrs: { ...data.attrs },
      };
    }

    const attrs = next.attrs as Attrs;
    attrs[key] = value;
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

const SELECTOR_REGEX = /^[.#[]/;

function isSelector(value: unknown): value is string {
  return typeof value === 'string' && SELECTOR_REGEX.test(value);
}

function normalizeArgs(a?: TagData | VNodeChildren, b?: VNodeChildren): [TagData, VNodeChildren] {
  if (b !== undefined) {
    if (!isVNodeData(a) && a !== null) {
      throw new TypeError('Invalid arguments: when passing 2 arguments, the first must be VNodeData or null');
    }
    return [a, b];
  }

  if (isVNodeData(a) || a === null) {
    return [a, []];
  }

  return [{}, a ?? []];
}

function makeTag(tag: string): TagFunction {
  function tagFn(selector: string): VNode;
  function tagFn(data: TagData): VNode;
  function tagFn(children: VNodeChildren): VNode;
  function tagFn(selector: string, data: TagData): VNode;
  function tagFn(selector: string, children: VNodeChildren): VNode;
  function tagFn(selector: string, data: TagData, children: VNodeChildren): VNode;
  function tagFn(data: TagData, children: VNodeChildren): VNode;
  function tagFn(
    a?: string | TagData | VNodeChildren,
    b?: TagData | VNodeChildren,
    c?: VNodeChildren,
  ): VNode {
    if (isSelector(a)) {
      const [data, children] = normalizeArgs(b, c);
      return h(`${tag}${a}`, movePropsToAttrs(data), children);
    }

    const [data, children] = normalizeArgs(
      a as TagData | VNodeChildren | undefined,
      b as VNodeChildren | undefined,
    );

    return h(tag, movePropsToAttrs(data), children);
  }

  return tagFn;
}

export const div: TagFunction = makeTag('div');
export const p: TagFunction = makeTag('p');
export const a: TagFunction = makeTag('a');
export const span: TagFunction = makeTag('span');
export const strong: TagFunction = makeTag('strong');
export const img: TagFunction = makeTag('img');
export const h1: TagFunction = makeTag('h1');
export const h2: TagFunction = makeTag('h2');
