export const root: Tree.Path = '';

export const size = (path: Tree.Path): number => path.length / 2;

export const head = (path: Tree.Path): Tree.Path => path.slice(0, 2);

export const tail = (path: Tree.Path): string => path.slice(2);

export const init = (path: Tree.Path): Tree.Path => path.slice(0, -2);

export const last = (path: Tree.Path): string => path.slice(-2);

export const contains = (p1: Tree.Path, p2: Tree.Path): boolean => p1.startsWith(p2);

export function fromNodeList(nodes: Tree.Node[]): Tree.Path {
  let path = '';
  for (const i in nodes) path += nodes[i].id;
  return path;
}

export const isChildOf = (child: Tree.Path, parent: Tree.Path): boolean =>
  !!child && child.slice(0, -2) === parent;
