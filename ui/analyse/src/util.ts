export const plyColor = (ply: number): Color => (ply % 2 === 0 ? 'white' : 'black');

export function readOnlyProp<A>(value: A): () => A {
  return function (): A {
    return value;
  };
}

export const plyToTurn = (ply: number): number => Math.floor((ply - 1) / 2) + 1;

export function treeReconstruct(parts: Tree.Node[], sidelines?: Tree.Node[][]): Tree.Node {
  const root = parts[0],
    nb = parts.length;
  let node = root;
  root.id = '';
  for (let i = 1; i < nb; i++) {
    const n = parts[i];
    const variations = sidelines ? sidelines[i] : [];
    if (node.children) node.children.unshift(n, ...variations);
    else node.children = [n, ...variations];
    node = n;
  }
  node.children = node.children || [];
  return root;
}
