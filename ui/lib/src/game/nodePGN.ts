import type { TreeNode } from '@/tree/types';

import { fixCrazySan } from './chess';

export const plyPrefix = (node: TreeNode): string =>
  `${Math.floor((node.ply + 1) / 2)}${node.ply % 2 === 1 ? '. ' : '... '}`;

export function renderNodesTxt(node: TreeNode, forcePly: boolean): string {
  if (node.children.length === 0) return '';

  let s = '';
  const first = node.children[0];
  if (forcePly || first.ply % 2 === 1) s += plyPrefix(first);
  s += fixCrazySan(first.san!);

  for (let i = 1; i < node.children.length; i++) {
    const child = node.children[i];
    s += ` (${plyPrefix(child)}${fixCrazySan(child.san!)}`;
    const variation = renderNodesTxt(child, false);
    if (variation) s += ' ' + variation;
    s += ')';
  }

  const mainline = renderNodesTxt(first, node.children.length > 1);
  if (mainline) s += ' ' + mainline;

  return s;
}
