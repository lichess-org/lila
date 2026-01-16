import { parseUci, Position } from 'chessops';
import { scalachessCharPair } from 'chessops/compat';
import type { TreeNode, TreeNodeIncomplete } from 'lib/tree/types';

export const plyColor = (ply: number): Color => (ply % 2 === 0 ? 'white' : 'black');

export function readOnlyProp<A>(value: A): () => A {
  return () => value;
}

// mutates and returns the node
export const completeNode = (node: TreeNodeIncomplete): TreeNode => {
  node.children ||= [];
  node.id ||= node.uci ? scalachessCharPair(parseUci(node.uci)!) : '';
  node.children.forEach(completeNode);
  return node as TreeNode;
};

export function treeReconstruct(parts: TreeNodeIncomplete[], sidelines?: TreeNode[][]): TreeNode {
  const root = completeNode(parts[0]);
  let node = root;
  for (let i = 1; i < parts.length; i++) {
    const n = completeNode(parts[i]);
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
