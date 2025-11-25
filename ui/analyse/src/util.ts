export const plyColor = (ply: number): Color => (ply % 2 === 0 ? 'white' : 'black');

export function readOnlyProp<A>(value: A): () => A {
  return function (): A {
    return value;
  };
}

const ensureChildren = (node: Tree.NodeOptionalChildren): Tree.Node => {
  node.children ||= [];
  return node as Tree.Node;
};

export function treeReconstruct(parts: Tree.NodeOptionalChildren[], sidelines?: Tree.Node[][]): Tree.Node {
  const root = ensureChildren(parts[0]);
  root.id = '';
  let node = root;
  for (let i = 1; i < parts.length; i++) {
    const n = ensureChildren(parts[i]);
    const variations = sidelines ? sidelines[i] : [];
    node.children.unshift(n, ...variations);
    node = n;
  }
  return root;
}
