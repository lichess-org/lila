import { Position } from 'chessops';

import { completeNode } from 'lib/tree/node';
import type { TreeNode, TreeNodeLite, TreeNodeBase } from 'lib/tree/types';

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

export function mergeLocalEval(into: TreeNodeLite, from: TreeNodeLite, isStudy: boolean): void {
  into.children = into.children.filter(hasUserData);
  into.eval = from.eval;

  const filteredComments = into.comments?.filter(c => String(c.by).toLowerCase() !== 'lichess');
  const filteredGlyphs = into.glyphs?.filter(g => ![2, 4, 6].includes(g.id));

  if (into.comments?.length !== filteredComments?.length) into.glyphs = filteredGlyphs; // cross fingers
  into.comments = filteredComments;

  const lastEngineComment = from.comments
    ?.slice()
    .reverse()
    .find(c => String(c.by).toLowerCase() === 'lichess');
  if (lastEngineComment) {
    into.comments ??= [];
    into.comments.push(lastEngineComment);
  }
  const lastEngineGlyph = from.glyphs
    ?.slice()
    .reverse()
    .find(g => [2, 4, 6].includes(g.id));
  if (lastEngineGlyph) {
    into.glyphs ??= [];
    into.glyphs.push(lastEngineGlyph);
  }

  for (const fromChild of from.children) {
    const intoChild = into.children.find(n => fromChild.id === n.id);
    if (intoChild) mergeLocalEval(intoChild, fromChild, isStudy);
    else into.children.push(fromChild);
  }

  function hasUserData(node: TreeNode): boolean {
    return (
      !node.comp ||
      node.comments?.some(c => String(c.by).toLowerCase() !== 'lichess') ||
      // no need to recurse ordinary game replays. only the first engine node is marked anyways
      (isStudy && node.children.some(hasUserData))
    );
  }
}
