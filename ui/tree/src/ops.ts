export function withMainlineChild<T>(node: Tree.Node, f: (node: Tree.Node) => T): T | undefined {
  const next = node.children[0];
  return next ? f(next) : undefined;
}

export function findInMainline(fromNode: Tree.Node, predicate: (node: Tree.Node) => boolean): Tree.Node | undefined {
  const findFrom = function (node: Tree.Node): Tree.Node | undefined {
    if (predicate(node)) return node;
    return withMainlineChild(node, findFrom);
  };
  return findFrom(fromNode);
}

// returns a list of nodes collected from the original one
export function collect(from: Tree.Node, pickChild: (node: Tree.Node) => Tree.Node | undefined): Tree.Node[] {
  const nodes = [from];
  let n = from,
    c;
  while ((c = pickChild(n))) {
    nodes.push(c);
    n = c;
  }
  return nodes;
}

function pickFirstChild(node: Tree.Node): Tree.Node | undefined {
  return node.children[0];
}

export function childById(node: Tree.Node, id: string): Tree.Node | undefined {
  return node.children.find(child => child.id === id);
}

export function last(nodeList: Tree.Node[]): Tree.Node | undefined {
  return nodeList[nodeList.length - 1];
}

export function nodeAtPly(nodeList: Tree.Node[], ply: number): Tree.Node | undefined {
  return nodeList.find(node => node.ply === ply);
}

export function takePathWhile(nodeList: Tree.Node[], predicate: (node: Tree.Node) => boolean): Tree.Path {
  let path = '';
  for (const i in nodeList) {
    if (predicate(nodeList[i])) path += nodeList[i].id;
    else break;
  }
  return path;
}

export function removeChild(parent: Tree.Node, id: string): void {
  parent.children = parent.children.filter(function (n) {
    return n.id !== id;
  });
}

export function countChildrenAndComments(node: Tree.Node) {
  const count = {
    nodes: 1,
    comments: (node.comments || []).length,
  };
  node.children.forEach(function (child) {
    const c = countChildrenAndComments(child);
    count.nodes += c.nodes;
    count.comments += c.comments;
  });
  return count;
}

// adds n2 into n1
export function merge(n1: Tree.Node, n2: Tree.Node): void {
  n1.eval = n2.eval;
  if (n2.glyphs) n1.glyphs = n2.glyphs;
  n2.comments &&
    n2.comments.forEach(function (c) {
      if (!n1.comments) n1.comments = [c];
      else if (
        !n1.comments.some(function (d) {
          return d.text === c.text;
        })
      )
        n1.comments.push(c);
    });
  n2.children.forEach(function (c) {
    const existing = childById(n1, c.id);
    if (existing) merge(existing, c);
    else n1.children.push(c);
  });
}

export function hasBranching(node: Tree.Node, maxDepth: number): boolean {
  return maxDepth <= 0 || !!node.children[1] || (node.children[0] && hasBranching(node.children[0], maxDepth - 1));
}

export function mainlineNodeList(from: Tree.Node): Tree.Node[] {
  return collect(from, pickFirstChild);
}

export function updateAll(root: Tree.Node, f: (node: Tree.Node) => void): void {
  // applies f recursively to all nodes
  function update(node: Tree.Node) {
    f(node);
    node.children.forEach(update);
  }
  update(root);
}
