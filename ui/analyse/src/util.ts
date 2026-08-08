import { Position } from 'chessops';

import { completeNode } from 'lib/tree/node';
import type { TreeNode, TreeNodeBase, TreeNodeLite } from 'lib/tree/types';

export function readOnlyProp<A>(value: A): () => A {
  return () => value;
}

export function treeReconstruct(
  parts: TreeNodeBase[],
  variant: VariantKey,
  sidelines?: TreeNode[][],
): TreeNode {
  const completer = completeNode(variant);
  const root = completer(parts[0]);
  let node = root;
  for (let i = 1; i < parts.length; i++) {
    const n = completer(parts[i]);
    const variations = sidelines ? sidelines[i] : [];
    node.children.unshift(n, ...variations);
    node = n;
  }
  return root;
}

export function addCrazyData(node: TreeNode, pos: Position): void {
  if (pos.pockets)
    node.crazy = {
      pockets: [pos.pockets.white, pos.pockets.black],
    };
}

export function hasUserContent(node: TreeNodeLite): boolean {
  return (
    !node.comp ||
    Boolean(node.comments?.some(comment => !comment.comp)) ||
    Boolean(node.glyphs?.some(glyph => !glyph.comp)) ||
    node.children.some(hasUserContent)
  );
}

// keep computer nodes only when they are needed as ancestors of a user node
export function pruneStaticAnalysis(node: TreeNodeLite): boolean {
  node.children = node.children.filter(pruneStaticAnalysis);
  return hasUserContent(node);
}
