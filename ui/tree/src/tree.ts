import * as treePath from './path';
import * as ops from './ops';
import { defined } from 'common';

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
  pathExists(path: Tree.Path): boolean;
  deleteNodeAt(path: Tree.Path): void;
  promoteAt(path: Tree.Path, toMainline: boolean): void;
  forceVariationAt(path: Tree.Path, force: boolean): MaybeNode;
  getCurrentNodesAfterPly(nodeList: Tree.Node[], mainline: Tree.Node[], ply: number): Tree.Node[];
  merge(tree: Tree.Node): void;
  removeCeval(): void;
  removeComputerVariations(): void;
  parentNode(path: Tree.Path): Tree.Node;
  getParentClock(node: Tree.Node, path: Tree.Path): Tree.Clock | undefined;
}

export function build(root: Tree.Node): TreeWrapper {
  function lastNode(): MaybeNode {
    return ops.findInMainline(root, (node: Tree.Node) => !node.children.length);
  }

  function nodeAtPath(path: Tree.Path): Tree.Node {
    return nodeAtPathFrom(root, path);
  }

  function nodeAtPathFrom(node: Tree.Node, path: Tree.Path): Tree.Node {
    if (path === '') return node;
    const child = ops.childById(node, treePath.head(path));
    return child ? nodeAtPathFrom(child, treePath.tail(path)) : node;
  }

  function nodeAtPathOrNull(path: Tree.Path): Tree.Node | undefined {
    return nodeAtPathOrNullFrom(root, path);
  }

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
    for (const i in nodeList) {
      const node = nodeList[i];
      if (node.ply <= ply && mainline[i].id !== node.id) break;
      if (node.ply > ply) nodes.push(node);
    }
    return nodes;
  }

  function pathIsMainline(path: Tree.Path): boolean {
    return pathIsMainlineFrom(root, path);
  }

  function pathExists(path: Tree.Path): boolean {
    return !!nodeAtPathOrNull(path);
  }

  function pathIsMainlineFrom(node: Tree.Node, path: Tree.Path): boolean {
    if (path === '') return true;
    const pathId = treePath.head(path),
      child = node.children[0];
    if (!child || child.id !== pathId) return false;
    return pathIsMainlineFrom(child, treePath.tail(path));
  }

  function pathIsForcedVariation(path: Tree.Path): boolean {
    return !!getNodeList(path).find(n => n.forceVariation);
  }

  function lastMainlineNodeFrom(node: Tree.Node, path: Tree.Path): Tree.Node {
    if (path === '') return node;
    const pathId = treePath.head(path);
    const child = node.children[0];
    if (!child || child.id !== pathId) return node;
    return lastMainlineNodeFrom(child, treePath.tail(path));
  }

  function getNodeList(path: Tree.Path): Tree.Node[] {
    return ops.collect(root, function (node: Tree.Node) {
      const id = treePath.head(path);
      if (id === '') return;
      path = treePath.tail(path);
      return ops.childById(node, id);
    });
  }

  function updateAt(path: Tree.Path, update: (node: Tree.Node) => void): Tree.Node | undefined {
    const node = nodeAtPathOrNull(path);
    if (node) {
      update(node);
      return node;
    }
    return;
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
    return updateAt(path, function (parent: Tree.Node) {
      parent.children.push(node);
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

  function deleteNodeAt(path: Tree.Path): void {
    ops.removeChild(parentNode(path), treePath.last(path));
  }

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

  function setCommentAt(comment: Tree.Comment, path: Tree.Path) {
    return !comment.text
      ? deleteCommentAt(comment.id, path)
      : updateAt(path, function (node) {
          node.comments = node.comments || [];
          const existing = node.comments.find(function (c) {
            return c.id === comment.id;
          });
          if (existing) existing.text = comment.text;
          else node.comments.push(comment);
        });
  }

  function deleteCommentAt(id: string, path: Tree.Path) {
    return updateAt(path, function (node) {
      const comments = (node.comments || []).filter(function (c) {
        return c.id !== id;
      });
      node.comments = comments.length ? comments : undefined;
    });
  }

  function setGlyphsAt(glyphs: Tree.Glyph[], path: Tree.Path) {
    return updateAt(path, function (node) {
      node.glyphs = glyphs;
    });
  }

  function parentNode(path: Tree.Path): Tree.Node {
    return nodeAtPath(treePath.init(path));
  }

  function getParentClock(node: Tree.Node, path: Tree.Path): Tree.Clock | undefined {
    if (!('parentClock' in node)) {
      const par = path && parentNode(path);
      if (!par) node.parentClock = node.clock;
      else if (!('clock' in par)) node.parentClock = undefined;
      else node.parentClock = par.clock;
    }
    return node.parentClock;
  }

  return {
    root,
    lastPly(): number {
      return lastNode()?.ply || root.ply;
    },
    nodeAtPath,
    getNodeList,
    longestValidPath: (path: string) => longestValidPathFrom(root, path),
    updateAt,
    addNode,
    addNodes,
    addDests(dests: string, path: Tree.Path) {
      return updateAt(path, function (node: Tree.Node) {
        node.dests = dests;
      });
    },
    setShapes(shapes: Tree.Shape[], path: Tree.Path) {
      return updateAt(path, function (node: Tree.Node) {
        node.shapes = shapes;
      });
    },
    setCommentAt,
    deleteCommentAt,
    setGlyphsAt,
    setClockAt(clock: Tree.Clock | undefined, path: Tree.Path) {
      return updateAt(path, function (node) {
        node.clock = clock;
      });
    },
    pathIsMainline,
    pathIsForcedVariation,
    lastMainlineNode(path: Tree.Path): Tree.Node {
      return lastMainlineNodeFrom(root, path);
    },
    pathExists,
    deleteNodeAt,
    promoteAt,
    forceVariationAt(path: Tree.Path, force: boolean) {
      return updateAt(path, function (node) {
        node.forceVariation = force;
      });
    },
    getCurrentNodesAfterPly,
    merge(tree: Tree.Node) {
      ops.merge(root, tree);
    },
    removeCeval() {
      ops.updateAll(root, function (n) {
        delete n.ceval;
        delete n.threat;
      });
    },
    removeComputerVariations() {
      ops.mainlineNodeList(root).forEach(function (n) {
        n.children = n.children.filter(function (c) {
          return !c.comp;
        });
      });
    },
    parentNode,
    getParentClock,
  };
}
