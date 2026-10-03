import type { VNode, VNodeChildren } from 'snabbdom';

/**
 * Recursively extract every text node from a snabbdom VNode tree,
 * joining them with a single space.
 */
export function extractText(node: VNodeChildren | VNodeChildren[]): string {
  if (node === null || node === undefined) return '';
  if (typeof node === 'string' || typeof node === 'number') return String(node);
  if (Array.isArray(node))
    return node
      .map(extractText)
      .join(' ')
      .replace(/\s{2,}/g, ' ')
      .trim();
  // VNode
  const vnode = node as VNode;
  if (vnode.text !== undefined) return vnode.text;
  if (vnode.children) return extractText(vnode.children);
  return '';
}
