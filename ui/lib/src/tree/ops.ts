// no side effects allowed due to re-export by index.ts

import type { TreeNodeBase, TreeNodeLite, TreePath } from './types';

export function withMainlineChild<U, T extends TreeNodeBase>(
  node: T,
  f: (node: TreeNodeBase) => U,
): U | undefined {
  const next = node.children?.[0];
  return next ? f(next) : undefined;
}

export function findInMainline<T extends TreeNodeBase>(
  fromNode: T,
  predicate: (node: T) => boolean,
): T | undefined {
  const findFrom = (node: T) => (predicate(node) ? node : withMainlineChild<T, T>(node, findFrom));
  return findFrom(fromNode);
}

// returns a list of nodes collected from the original one
export function collect<T extends TreeNodeBase>(from: T, pickChild: (node: T) => T | undefined): T[] {
  const nodes = [from];
  let n = from,
    c;
  while ((c = pickChild(n))) {
    nodes.push(c);
    n = c;
  }
  return nodes;
}

export const childById = <T extends TreeNodeBase>(node: T, id: string): T | undefined =>
  node.children?.find(child => child.id === id) as T | undefined;

export const last = <T extends TreeNodeBase>(nodeList: T[]): T | undefined => nodeList[nodeList.length - 1];

export const nodeAtPly = <T extends TreeNodeBase>(nodeList: T[], ply: number): T | undefined =>
  nodeList.find(node => node.ply === ply);

export function takePathWhile<T extends TreeNodeBase>(
  nodeList: T[],
  predicate: (node: T) => boolean,
): TreePath {
  let path = '';
  for (const n of nodeList) {
    if (predicate(n)) path += n.id;
    else break;
  }
  return path;
}

export function removeChild<T extends TreeNodeBase>(parent: T, id: string): void {
  parent.children = parent.children?.filter(n => n.id !== id);
}

export function countChildrenAndComments<T extends TreeNodeBase>(
  node: T,
): {
  nodes: number;
  comments: number;
} {
  const count = {
    nodes: 1,
    comments: (node.comments || []).length,
  };
  node.children?.forEach(function (child) {
    const c = countChildrenAndComments(child);
    count.nodes += c.nodes;
    count.comments += c.comments;
  });
  return count;
}

// adds n2 into n1
export function merge(n1: TreeNodeLite, n2: TreeNodeLite): void {
  if (n2.eval) n1.eval = n2.eval;
  if (n2.glyphs) n1.glyphs = n2.glyphs;
  n2.comments?.forEach(c => {
    if (!n1.comments) n1.comments = [c];
    else if (!n1.comments.some(d => d.text === c.text)) n1.comments.push(c);
  });
  n2.children?.forEach(c => {
    const existing = childById(n1, c.id);
    if (existing) merge(existing, c);
    else n1.children.push(c);
  });
}

export const hasBranching = (node: TreeNodeLite, maxDepth: number): boolean =>
  maxDepth <= 0 || !!node.children[1] || (!!node.children[0] && hasBranching(node.children[0], maxDepth - 1));

export const mainlineNodeList = <T extends TreeNodeBase>(from: T): T[] =>
  collect<T>(from, node => node.children?.[0] as T);

export function updateAll<T extends TreeNodeBase>(root: T, f: (node: T) => void): void {
  // applies f recursively to all nodes
  function update(node: T) {
    f(node);
    node.children?.forEach(update);
  }
  update(root);
}

export function distance(a: TreePath, b: TreePath): number {
  let i = 0;
  while (i < a.length && i < b.length && a[i] === b[i] && a[i + 1] === b[i + 1]) i += 2;
  return (a.length + b.length) / 2 - i;
}

export function contains<T extends TreeNodeBase>(container: T, descendant: T): boolean {
  return container === descendant || !!container.children?.some(child => contains(child, descendant));
}

// for serialization
export function structuredCloneLite(node: TreeNodeBase): TreeNodeLite {
  return Object.fromEntries(
    Object.entries(node)
      .filter(([_, v]) => typeof v !== 'function')
      .map(([k, v]) => {
        if (k === 'children') return [k, v.map(structuredCloneLite)];
        return [k, structuredClone(v)];
      }),
  ) as TreeNodeLite;
}

export function getNodeList<T extends TreeNodeBase>(root: T, path: TreePath): T[] {
  const nodes: T[] = [root];
  for (let i = 0, node = root; i < path.length; i += 2) {
    const child = childById(node, path.slice(i, i + 2));
    if (!child) break;
    nodes.push(child);
    node = child;
  }
  return nodes;
}
