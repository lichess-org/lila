import * as treePath from './path';
import * as ops from './ops';
import { defined } from '../index';
import type { Clock, Glyph, Shape, TreeComment, TreeNode, TreePath } from './types';

export { treePath as path, ops };

export type MaybeNode = TreeNode | undefined;

export interface TreeWrapper {
  root: TreeNode;
  lastPly(): number;
  nodeAtPath(path: TreePath): TreeNode;
  getNodeList(path: TreePath): TreeNode[];
  longestValidPath(path: string): TreePath;
  updateAt(path: TreePath, update: (node: TreeNode) => void): MaybeNode;
  addNode(node: TreeNode, path: TreePath): TreePath | undefined;
  addNodes(nodes: TreeNode[], path: TreePath): TreePath | undefined;
  setDests(dests: Dests, path: TreePath): MaybeNode;
  setShapes(shapes: Shape[], path: TreePath): MaybeNode;
  setCommentAt(comment: TreeComment, path: TreePath): MaybeNode;
  deleteCommentAt(id: string, path: TreePath): MaybeNode;
  setGlyphsAt(glyphs: Glyph[], path: TreePath): MaybeNode;
  setClockAt(clock: Clock | undefined, path: TreePath): MaybeNode;
  pathIsMainline(path: TreePath): boolean;
  pathIsForcedVariation(path: TreePath): boolean;
  lastMainlineNode(path: TreePath): TreeNode;
  extendPath(path: TreePath, isMainline: boolean): TreePath;
  pathExists(path: TreePath): boolean;
  deleteNodeAt(path: TreePath): void;
  promoteAt(path: TreePath, toMainline: boolean): void;
  forceVariationAt(path: TreePath, force: boolean): MaybeNode;
  getCurrentNodesAfterPly(nodeList: TreeNode[], mainline: TreeNode[], ply: number): TreeNode[];
  merge(tree: TreeNode): void;
  removeCeval(): void;
  parentNode(path: TreePath): TreeNode;
  getParentClock(node: TreeNode, path: TreePath): Clock | undefined;
  walkUntilTrue(
    fn: (node: TreeNode, isMainline: boolean) => boolean,
    path?: TreePath,
    branchOnly?: boolean,
  ): boolean;
}

export function makeTree(root: TreeNode): TreeWrapper {
  const lastNode = (): MaybeNode => ops.findInMainline(root, (node: TreeNode) => !node.children.length);

  const nodeAtPath = (path: TreePath): TreeNode => nodeAtPathFrom(root, path);

  function nodeAtPathFrom(node: TreeNode, path: TreePath): TreeNode {
    if (path === '') return node;
    const child = ops.childById(node, treePath.head(path));
    return child ? nodeAtPathFrom(child, treePath.tail(path)) : node;
  }

  const nodeAtPathOrNull = (path: TreePath): TreeNode | undefined => nodeAtPathOrNullFrom(root, path);

  function nodeAtPathOrNullFrom(node: TreeNode, path: TreePath): TreeNode | undefined {
    if (path === '') return node;
    const child = ops.childById(node, treePath.head(path));
    return child ? nodeAtPathOrNullFrom(child, treePath.tail(path)) : undefined;
  }

  function longestValidPathFrom(node: TreeNode, path: TreePath): TreePath {
    const id = treePath.head(path);
    const child = ops.childById(node, id);
    return child ? id + longestValidPathFrom(child, treePath.tail(path)) : '';
  }

  function getCurrentNodesAfterPly(nodeList: TreeNode[], mainline: TreeNode[], ply: number): TreeNode[] {
    const nodes = [];
    for (let i = 0; i < nodeList.length; i++) {
      const node = nodeList[i];
      if (node.ply <= ply && mainline[i].id !== node.id) break;
      if (node.ply > ply) nodes.push(node);
    }
    return nodes;
  }

  const pathIsMainline = (path: TreePath): boolean => pathIsMainlineFrom(root, path);

  function pathIsMainlineFrom(node: TreeNode, path: TreePath): boolean {
    if (path === '') return true;
    const child = node.children[0];
    return child?.id === treePath.head(path) && pathIsMainlineFrom(child, treePath.tail(path));
  }

  const pathExists = (path: TreePath): boolean => !!nodeAtPathOrNull(path);

  const pathIsForcedVariation = (path: TreePath): boolean => !!getNodeList(path).find(n => n.forceVariation);

  function lastMainlineNodeFrom(node: TreeNode, path: TreePath): TreeNode {
    if (path === '') return node;
    const pathId = treePath.head(path);
    const child = node.children[0];
    if (!child || child.id !== pathId) return node;
    return lastMainlineNodeFrom(child, treePath.tail(path));
  }

  const getNodeList = (path: TreePath): TreeNode[] =>
    ops.collect(root, function (node: TreeNode) {
      const id = treePath.head(path);
      if (id === '') return;
      path = treePath.tail(path);
      return ops.childById(node, id);
    });

  const extendPath = (path: TreePath, isMainline: boolean): TreePath => {
    let currNode = nodeAtPath(path);
    while ((currNode = currNode?.children[0]) && !(isMainline && currNode.forceVariation))
      path += currNode.id;
    return path;
  };

  function updateAt(path: TreePath, update: (node: TreeNode) => void): TreeNode | undefined {
    const node = nodeAtPathOrNull(path);
    if (node) update(node);
    return node;
  }

  // returns new path
  function addNode(node: TreeNode, path: TreePath): TreePath | undefined {
    const newPath = path + node.id,
      existing = nodeAtPathOrNull(newPath);
    if (existing) {
      (['dests', 'drops', 'clock'] as Array<keyof TreeNode>).forEach(key => {
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

  function addNodes(nodes: TreeNode[], path: TreePath): TreePath | undefined {
    const node = nodes[0];
    if (!node) return path;
    const newPath = addNode(node, path);
    return newPath ? addNodes(nodes.slice(1), newPath) : undefined;
  }

  const deleteNodeAt = (path: TreePath): void => ops.removeChild(parentNode(path), treePath.last(path));

  function promoteAt(path: TreePath, toMainline: boolean): void {
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

  const setCommentAt = (comment: TreeComment, path: TreePath) =>
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

  const deleteCommentAt = (id: string, path: TreePath) =>
    updateAt(path, node => {
      const comments = (node.comments || []).filter(c => c.id !== id);
      node.comments = comments.length ? comments : undefined;
    });

  const setGlyphsAt = (glyphs: Glyph[], path: TreePath) =>
    updateAt(path, node => {
      node.glyphs = glyphs;
    });

  const parentNode = (path: TreePath): TreeNode => nodeAtPath(treePath.init(path));

  const getParentClock = (node: TreeNode, path: TreePath): Clock | undefined =>
    path ? parentNode(path).clock : node.clock;

  function walkUntilTrue(
    fn: (node: TreeNode, isMainline: boolean) => boolean,
    from: TreePath = '',
    branchOnly: boolean = false,
  ) {
    function traverse(node: TreeNode, isMainline: boolean): boolean {
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
    setDests: (dests: Dests, path: TreePath) =>
      updateAt(path, node => {
        node.dests = dests;
      }),
    setShapes: (shapes: Shape[], path: TreePath) =>
      updateAt(path, (node: TreeNode) => {
        node.shapes = shapes.slice();
      }),
    setCommentAt,
    deleteCommentAt,
    setGlyphsAt,
    setClockAt: (clock: Clock | undefined, path: TreePath) =>
      updateAt(path, node => {
        node.clock = clock;
      }),
    pathIsMainline,
    pathIsForcedVariation,
    lastMainlineNode: (path: TreePath): TreeNode => lastMainlineNodeFrom(root, path),
    extendPath,
    pathExists,
    deleteNodeAt,
    promoteAt,
    forceVariationAt: (path: TreePath, force: boolean) => {
      ops.updateAll(root, n => (n.forceVariation = false));
      return updateAt(path, node => (node.forceVariation = force));
    },
    getCurrentNodesAfterPly,
    merge: (tree: TreeNode) => ops.merge(root, tree),
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
