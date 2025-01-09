export const root: Tree.Path = '';

export function size(path: Tree.Path): number {
  return path.length / 2;
}

export function head(path: Tree.Path): Tree.Path {
  return path.slice(0, 2);
}

export function tail(path: Tree.Path): string {
  return path.slice(2);
}

export function init(path: Tree.Path): Tree.Path {
  return path.slice(0, -2);
}

export function last(path: Tree.Path): string {
  return path.slice(-2);
}

export function contains(p1: Tree.Path, p2: Tree.Path): boolean {
  return p1.startsWith(p2);
}

export function fromNodeList(nodes: Tree.Node[]): Tree.Path {
  let path = '';
  for (const i in nodes) path += nodes[i].id;
  return path;
}

export function isChildOf(child: Tree.Path, parent: Tree.Path): boolean {
  return !!child && child.slice(0, -2) === parent;
}
