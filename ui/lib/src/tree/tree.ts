import * as treePath from './path';
import * as ops from './ops';
import { defined } from '../common';

export { treePath as path, ops };

export type MaybeNode = Tree.Node | undefined;

export interface TreeWrapper {
  root: Tree.Node;
  lastPly(): number;
  nodeAtPath(path: Tree.Path): Tree.Node;
  getNodeList(path: Tree.Path): Tree.Node[];
  longestValidPath(path: string): Tree.Path;
  updateAt(path: Tree.Path, update: (node: Tree.Node) => void): MaybeNode;
  addNode(node: Tree.Node, path: Tree.Path): Tree.Path | undefined;
  addNodes(nodes: Tree.Node[], path: Tree.Path): Tree.Path | undefined;
  addDests(dests: string, path: Tree.Path): MaybeNode;
  setShapes(shapes: Tree.Shape[], path: Tree.Path): MaybeNode;
  setCommentAt(comment: Tree.Comment, path: Tree.Path): MaybeNode;
  deleteCommentAt(id: string, path: Tree.Path): MaybeNode;
  setGlyphsAt(glyphs: Tree.Glyph[], path: Tree.Path): MaybeNode;
  setClockAt(clock: Tree.Clock | undefined, path: Tree.Path): MaybeNode;
  pathIsMainline(path: Tree.Path): boolean;
  pathIsForcedVariation(path: Tree.Path): boolean;
  lastMainlineNode(path: Tree.Path): Tree.Node;
  extendPath(path: Tree.Path, isMainline: boolean): Tree.Path;
  pathExists(path: Tree.Path): boolean;
  deleteNodeAt(path: Tree.Path): void;
  promoteAt(path: Tree.Path, toMainline: boolean): void;
  forceVariationAt(path: Tree.Path, force: boolean): MaybeNode;
  getCurrentNodesAfterPly(nodeList: Tree.Node[], mainline: Tree.Node[], ply: number): Tree.Node[];
  merge(tree: Tree.Node): void;
  removeCeval(): void;
  parentNode(path: Tree.Path): Tree.Node;
  getParentClock(node: Tree.Node, path: Tree.Path): Tree.Clock | undefined;
  walkUntilTrue(
    fn: (node: Tree.Node, isMainline: boolean) => boolean,
    path?: Tree.Path,
    branchOnly?: boolean,
  ): boolean;
}

export function build(root: Tree.Node): TreeWrapper {
  const lastNode = (): MaybeNode => ops.findInMainline(root, (node: Tree.Node) => !node.children.length);

  const nodeAtPath = (path: Tree.Path): Tree.Node => nodeAtPathFrom(root, path);

  function nodeAtPathFrom(node: Tree.Node, path: Tree.Path): Tree.Node {
    if (path === '') return node;
    const child = ops.childById(node, treePath.head(path));
    return child ? nodeAtPathFrom(child, treePath.tail(path)) : node;
  }

  const nodeAtPathOrNull = (path: Tree.Path): Tree.Node | undefined => nodeAtPathOrNullFrom(root, path);

  function nodeAtPathOrNullFrom(node: Tree.Node, path: Tree.Path): Tree.Node | undefined {
    if (path === '') return node;
    const child = ops.childById(node, treePath.head(path));
    return child ? nodeAtPathOrNullFrom(child, treePath.tail(path)) : undefined;
  }

  function longestValidPathFrom(node: Tree.Node, path: Tree.Path): Tree.Path {
    const id = treePath.head(path);
    const child = ops.childById(node, id);
    return child ? id + longestValidPathFrom(child, treePath.tail(path)) : '';
  }

  function getCurrentNodesAfterPly(nodeList: Tree.Node[], mainline: Tree.Node[], ply: number): Tree.Node[] {
    const nodes = [];
    for (let i = 0; i < nodeList.length; i++) {
      const node = nodeList[i];
      if (node.ply <= ply && mainline[i].id !== node.id) break;
      if (node.ply > ply) nodes.push(node);
    }
    return nodes;
  }

  const pathIsMainline = (path: Tree.Path): boolean => pathIsMainlineFrom(root, path);

  function pathIsMainlineFrom(node: Tree.Node, path: Tree.Path): boolean {
    if (path === '') return true;
    const child = node.children[0];
    return child?.id === treePath.head(path) && pathIsMainlineFrom(child, treePath.tail(path));
  }

  const pathExists = (path: Tree.Path): boolean => !!nodeAtPathOrNull(path);

  const pathIsForcedVariation = (path: Tree.Path): boolean => !!getNodeList(path).find(n => n.forceVariation);

  function lastMainlineNodeFrom(node: Tree.Node, path: Tree.Path): Tree.Node {
    if (path === '') return node;
    const pathId = treePath.head(path);
    const child = node.children[0];
    if (!child || child.id !== pathId) return node;
    return lastMainlineNodeFrom(child, treePath.tail(path));
  }

  const getNodeList = (path: Tree.Path): Tree.Node[] =>
    ops.collect(root, function (node: Tree.Node) {
      const id = treePath.head(path);
      if (id === '') return;
      path = treePath.tail(path);
      return ops.childById(node, id);
    });

  const extendPath = (path: Tree.Path, isMainline: boolean): Tree.Path => {
    let currNode = nodeAtPath(path);
    while ((currNode = currNode?.children[0]) && !(isMainline && currNode.forceVariation))
      path += currNode.id;
    return path;
  };

  function updateAt(path: Tree.Path, update: (node: Tree.Node) => void): Tree.Node | undefined {
    const node = nodeAtPathOrNull(path);
    if (node) update(node);
    return node;
  }

  // returns new path
  function addNode(node: Tree.Node, path: Tree.Path): Tree.Path | undefined {
    const newPath = path + node.id,
      existing = nodeAtPathOrNull(newPath);
    if (existing) {
      (['dests', 'drops', 'clock'] as Array<keyof Tree.Node>).forEach(key => {
        if (defined(node[key]) && !defined(existing[key])) existing[key] = node[key] as never;
      });
      return newPath;
    }
    return updateAt(path, n => {
      n.children.push(node);
    })
      ? newPath
      : undefined;
  }

  function addNodes(nodes: Tree.Node[], path: Tree.Path): Tree.Path | undefined {
    const node = nodes[0];
    if (!node) return path;
    const newPath = addNode(node, path);
    return newPath ? addNodes(nodes.slice(1), newPath) : undefined;
  }

  const deleteNodeAt = (path: Tree.Path): void => ops.removeChild(parentNode(path), treePath.last(path));

  function promoteAt(path: Tree.Path, toMainline: boolean): void {
    const nodes = getNodeList(path);
    for (let i = nodes.length - 2; i >= 0; i--) {
      const node = nodes[i + 1];
      const parent = nodes[i];
      if (parent.children[0].id !== node.id) {
        ops.removeChild(parent, node.id);
        parent.children.unshift(node);
        if (!toMainline) break;
      } else if (node.forceVariation) {
        node.forceVariation = false;
        if (!toMainline) break;
      }
    }
  }

  const setCommentAt = (comment: Tree.Comment, path: Tree.Path) =>
    !comment.text
      ? deleteCommentAt(comment.id, path)
      : updateAt(path, node => {
          node.comments = node.comments || [];
          const existing = node.comments.find(function (c) {
            return c.id === comment.id;
          });
          if (existing) existing.text = comment.text;
          else node.comments.push(comment);
        });

  const deleteCommentAt = (id: string, path: Tree.Path) =>
    updateAt(path, node => {
      const comments = (node.comments || []).filter(c => c.id !== id);
      node.comments = comments.length ? comments : undefined;
    });

  const setGlyphsAt = (glyphs: Tree.Glyph[], path: Tree.Path) =>
    updateAt(path, node => {
      node.glyphs = glyphs;
    });

  const parentNode = (path: Tree.Path): Tree.Node => nodeAtPath(treePath.init(path));

  const getParentClock = (node: Tree.Node, path: Tree.Path): Tree.Clock | undefined =>
    path ? parentNode(path).clock : node.clock;

  function walkUntilTrue(
    fn: (node: Tree.Node, isMainline: boolean) => boolean,
    from: Tree.Path = '',
    branchOnly: boolean = false,
  ) {
    function traverse(node: Tree.Node, isMainline: boolean): boolean {
      if (fn(node, isMainline)) return true;
      let i = branchOnly ? 1 : 0;
      branchOnly = false;
      while (i < node.children.length) {
        const c = node.children[i];
        if (traverse(c, isMainline && i === 0 && !c.forceVariation)) return true;
        i++;
      }
      return false;
    }
    const n = nodeAtPathOrNull(from);
    return n ? traverse(n, pathIsMainline(from)) : false;
  }

  return {
    root,
    lastPly: (): number => lastNode()?.ply || root.ply,
    nodeAtPath,
    getNodeList,
    longestValidPath: (path: string) => longestValidPathFrom(root, path),
    updateAt,
    addNode,
    addNodes,
    addDests: (dests: string, path: Tree.Path) =>
      updateAt(path, (node: Tree.Node) => {
        node.dests = dests;
      }),
    setShapes: (shapes: Tree.Shape[], path: Tree.Path) =>
      updateAt(path, (node: Tree.Node) => {
        node.shapes = shapes.slice();
      }),
    setCommentAt,
    deleteCommentAt,
    setGlyphsAt,
    setClockAt: (clock: Tree.Clock | undefined, path: Tree.Path) =>
      updateAt(path, node => {
        node.clock = clock;
      }),
    pathIsMainline,
    pathIsForcedVariation,
    lastMainlineNode: (path: Tree.Path): Tree.Node => lastMainlineNodeFrom(root, path),
    extendPath,
    pathExists,
    deleteNodeAt,
    promoteAt,
    forceVariationAt: (path: Tree.Path, force: boolean) => {
      ops.updateAll(root, n => (n.forceVariation = false));
      return updateAt(path, node => (node.forceVariation = force));
    },
    getCurrentNodesAfterPly,
    merge: (tree: Tree.Node) => ops.merge(root, tree),
    removeCeval: () =>
      ops.updateAll(root, function (n) {
        delete n.ceval;
        delete n.threat;
      }),
    parentNode,
    getParentClock,
    walkUntilTrue,
  };
}
