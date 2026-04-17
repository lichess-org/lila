// no side effects allowed due to re-export by index.ts

import type { TreeNode, TreePath } from './types';

export const root: TreePath = '';

export const size = (path: TreePath): number => path.length / 2;

export const head = (path: TreePath): TreePath => path.slice(0, 2);

export const tail = (path: TreePath): string => path.slice(2);

export const init = (path: TreePath): TreePath => path.slice(0, -2);

export const last = (path: TreePath): string => path.slice(-2);

export const contains = (p1: TreePath, p2: TreePath): boolean => p1.startsWith(p2);

export const fromNodeList = (nodes: TreeNode[]): TreePath => nodes.map(n => n.id).join('');

export const isChildOf = (child: TreePath, parent: TreePath): boolean =>
  !!child && child.slice(0, -2) === parent;

export const intersection = (p1: TreePath, p2: TreePath): TreePath => {
  const head1 = head(p1),
    head2 = head(p2);
  return head1 !== '' && head1 === head2 ? head1 + intersection(tail(p1), tail(p2)) : '';
};
