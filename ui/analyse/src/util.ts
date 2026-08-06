import { Position } from 'chessops';

import { completeNode } from 'lib/tree/node';
import type { TreeNode, TreeNodeBase, TreeNodeLite } from 'lib/tree/types';

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

export function removeComputerAnnotations(node: TreeNodeLite): void {
  node.comments = node.comments?.filter(comment => !comment.comp);
  if (!node.comments?.length) delete node.comments;
  node.glyphs = node.glyphs?.filter(glyph => !glyph.comp);
  if (!node.glyphs?.length) delete node.glyphs;
  node.children?.forEach(removeComputerAnnotations);
}

export function mergeMainlineAnalysis(mergeTo: TreeNodeLite, fromAnalysis: TreeNodeLite): void {
  removeComputerAnnotations(mergeTo);
  const mergeNodes = (to: TreeNodeLite, from: TreeNodeLite, isParentComp: boolean) => {
    if (from.eval) to.eval = from.eval;
    for (const glyph of from.glyphs?.filter(glyph => glyph.comp) ?? []) {
      to.glyphs ??= [];
      if (!to.glyphs.some(existing => existing.id === glyph.id && !existing.comp === !glyph.comp))
        to.glyphs.push(glyph);
    }
    for (const comment of from.comments?.filter(comment => comment.comp) ?? []) {
      to.comments ??= [];
      if (!to.comments.some(existing => existing.id === comment.id)) to.comments.push(comment);
    }
    for (const child of from.children ?? []) {
      const existing = to.children.find(node => node.id === child.id);
      if (existing) mergeNodes(existing, child, isParentComp || Boolean(child.comp));
      else {
        // ensure engine lines display as consecutive first children, uninterrupted
        const insertAt = isParentComp ? to.children.findIndex(c => !c.comp) : to.children.length;
        to.children.splice(insertAt < 0 ? to.children.length : insertAt, 0, child);
      }
    }
  };
  mergeNodes(mergeTo, fromAnalysis, false);
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
