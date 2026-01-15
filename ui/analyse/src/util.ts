import { parseUci, Position } from 'chessops';
import { scalachessCharPair } from 'chessops/compat';

export const plyColor = (ply: number): Color => (ply % 2 === 0 ? 'white' : 'black');

export function readOnlyProp<A>(value: A): () => A {
  return function (): A {
    return value;
  };
}

export const completeNode = (node: Tree.NodeIncomplete): Tree.Node => {
  node.children ||= [];
  node.id ||= node.uci ? scalachessCharPair(parseUci(node.uci)!) : '';
  node.children.forEach(completeNode);
  return node as Tree.Node;
};

export function treeReconstruct(parts: Tree.NodeIncomplete[], sidelines?: Tree.Node[][]): Tree.Node {
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

export function addCrazyData(node: Tree.Node, pos: Position): void {
  if (pos.pockets)
    node.crazy = {
      pockets: [pos.pockets.white, pos.pockets.black],
    };
}
