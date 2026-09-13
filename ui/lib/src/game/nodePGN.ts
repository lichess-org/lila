import type { TreeNodeLite } from '@/tree/types';

import { fixCrazySan } from './chess';

export const plyPrefix = (node: TreeNodeLite): string =>
  `${Math.floor((node.ply + 1) / 2)}${node.ply % 2 === 1 ? '. ' : '... '}`;

export function renderNodesTxt(node: TreeNodeLite, forcePly: boolean, includeVariations = true): string {
  const first = node.children[0];
  if (!first || (!includeVariations && first.forceVariation)) return '';

  let s = '';
  if (forcePly || first.ply % 2 === 1) s += plyPrefix(first);
  s += fixCrazySan(first.san!);

  if (includeVariations) {
    for (let i = 1; i < node.children.length; i++) {
      const child = node.children[i];
      s += ` (${plyPrefix(child)}${fixCrazySan(child.san!)}`;
      const variation = renderNodesTxt(child, false, includeVariations);
      if (variation) s += ' ' + variation;
      s += ')';
    }
  }

  const mainline = renderNodesTxt(first, s.endsWith(')'), includeVariations);
  if (mainline) s += ' ' + mainline;

  return s;
}
