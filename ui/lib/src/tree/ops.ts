/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import type { TreeNode, TreePath } from './types';

export function withMainlineChild<T>(node: TreeNode, f: (node: TreeNode) => T): T | undefined {
  const next = node.children[0];
  return next ? f(next) : undefined;
}

export function findInMainline(
  fromNode: TreeNode,
  predicate: (node: TreeNode) => boolean,
): TreeNode | undefined {
  const findFrom = (node: TreeNode): TreeNode | undefined =>
    predicate(node) ? node : withMainlineChild(node, findFrom);
  return findFrom(fromNode);
}

// returns a list of nodes collected from the original one
export function collect(from: TreeNode, pickChild: (node: TreeNode) => TreeNode | undefined): TreeNode[] {
  const nodes = [from];
  let n = from,
    c;
  while ((c = pickChild(n))) {
    nodes.push(c);
    n = c;
  }
  return nodes;
}

export const childById = (node: TreeNode, id: string): TreeNode | undefined =>
  node.children.find(child => child.id === id);

export const last = (nodeList: TreeNode[]): TreeNode | undefined => nodeList[nodeList.length - 1];

export const nodeAtPly = (nodeList: TreeNode[], ply: number): TreeNode | undefined =>
  nodeList.find(node => node.ply === ply);

export function takePathWhile(nodeList: TreeNode[], predicate: (node: TreeNode) => boolean): TreePath {
  let path = '';
  for (const n of nodeList) {
    if (predicate(n)) path += n.id;
    else break;
  }
  return path;
}

export function removeChild(parent: TreeNode, id: string): void {
  parent.children = parent.children.filter(n => n.id !== id);
}

export function countChildrenAndComments(node: TreeNode): {
  nodes: number;
  comments: number;
} {
  const count = {
    nodes: 1,
    comments: (node.comments || []).length,
  };
  node.children.forEach(function (child) {
    const c = countChildrenAndComments(child);
    count.nodes += c.nodes;
    count.comments += c.comments;
  });
  return count;
}

// adds n2 into n1
export function merge(n1: TreeNode, n2: TreeNode): void {
  if (n2.eval) n1.eval = n2.eval;
  if (n2.glyphs) n1.glyphs = n2.glyphs;
  n2.comments &&
    n2.comments.forEach(function (c) {
      if (!n1.comments) n1.comments = [c];
      else if (
        !n1.comments.some(function (d) {
          return d.text === c.text;
        })
      )
        n1.comments.push(c);
    });
  n2.children.forEach(function (c) {
    const existing = childById(n1, c.id);
    if (existing) merge(existing, c);
    else n1.children.push(c);
  });
}

export const hasBranching = (node: TreeNode, maxDepth: number): boolean =>
  maxDepth <= 0 || !!node.children[1] || (!!node.children[0] && hasBranching(node.children[0], maxDepth - 1));

export const mainlineNodeList = (from: TreeNode): TreeNode[] => collect(from, node => node.children[0]);

export function updateAll(root: TreeNode, f: (node: TreeNode) => void): void {
  // applies f recursively to all nodes
  function update(node: TreeNode) {
    f(node);
    node.children.forEach(update);
  }
  update(root);
}

export function distance(a: TreePath, b: TreePath): number {
  let i = 0;
  while (i < a.length && i < b.length && a[i] === b[i] && a[i + 1] === b[i + 1]) i += 2;
  return (a.length + b.length) / 2 - i;
}

export function contains(container: TreeNode, descendant: TreeNode): boolean {
  return container === descendant || container.children.some(child => contains(child, descendant));
}
