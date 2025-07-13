export const root: Tree.Path = '';

export const size = (path: Tree.Path): number => path.length / 2;

export const head = (path: Tree.Path): Tree.Path => path.slice(0, 2);

export const tail = (path: Tree.Path): string => path.slice(2);

export const init = (path: Tree.Path): Tree.Path => path.slice(0, -2);

export const last = (path: Tree.Path): string => path.slice(-2);

export const contains = (p1: Tree.Path, p2: Tree.Path): boolean => p1.startsWith(p2);

export const fromNodeList = (nodes: Tree.Node[]): Tree.Path => nodes.map(n => n.id).join('');

export const isChildOf = (child: Tree.Path, parent: Tree.Path): boolean =>
  !!child && child.slice(0, -2) === parent;

export const intersection = (p1: Tree.Path, p2: Tree.Path): Tree.Path => {
  const head1 = head(p1),
    head2 = head(p2);
  return head1 !== '' && head1 === head2 ? head1 + intersection(tail(p1), tail(p2)) : '';
};
