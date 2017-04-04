import * as treePath from './path';
import * as ops from './ops';
import { defined } from 'common';

export default function(root: Tree.Node) {

  function firstPly() {
    return root.ply;
  }

  function lastNode(): Tree.Node {
    return ops.findInMainline(root, function(node: Tree.Node) {
      return !node.children.length;
    })!;
  }

  function lastPly(): number {
    return lastNode().ply;
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

  const getCurrentNodesAfterPly = function(nodeList: Tree.Node[], mainline: Tree.Node[], ply: number): Tree.Node[] {
    var node, nodes = [];
    for (var i in nodeList) {
      node = nodeList[i];
      if (node.ply <= ply && mainline[i].id !== node.id) break;
      if (node.ply > ply) nodes.push(node);
    }
    return nodes;
  };

  function pathIsMainline(path: Tree.Path): boolean {
    return pathIsMainlineFrom(root, path);
  }

  function pathExists(path: Tree.Path): boolean {
    return !!nodeAtPath(path);  // TODO: Bug?
  }

  function pathIsMainlineFrom(node: Tree.Node, path: Tree.Path): boolean {
    if (path === '') return true;
    const pathId = treePath.head(path);
    const child = node.children[0];
    if (!child || child.id !== pathId) return false;
    return pathIsMainlineFrom(child, treePath.tail(path));
  }

  function lastMainlineNode(path: Tree.Path): Tree.Node {
    return lastMainlineNodeFrom(root, path);
  }

  function lastMainlineNodeFrom(node: Tree.Node, path: Tree.Path): Tree.Node {
    if (path === '') return node;
    const pathId = treePath.head(path);
    const child = node.children[0];
    if (!child || child.id !== pathId) return node;
    return lastMainlineNodeFrom(child, treePath.tail(path));
  }

  function getNodeList(path: Tree.Path): Tree.Node[] {
    return ops.collect(root, function(node: Tree.Node) {
      const id = treePath.head(path);
      if (id === '') return undefined;
      path = treePath.tail(path);
      return ops.childById(node, id);
    });
  }

  function getOpening(nodeList: Tree.Node[]): Tree.Opening | undefined {
    var opening: Tree.Opening | undefined;
    nodeList.forEach(function(node: Tree.Node) {
      opening = node.opening || opening;
    });
    return opening;
  }

  function updateAt(path: Tree.Path, update: (node: Tree.Node) => void): Tree.Node | undefined {
    const node = nodeAtPathOrNull(path);
    if (node) {
      update(node);
      return node;
    }
    return undefined;
  }

  // returns new path
  function addNode(node: Tree.Node, path: Tree.Path): Tree.Path | undefined {
    const newPath = path + node.id;
    var existing = nodeAtPathOrNull(newPath);
    if (existing) {
      if (defined(node.dests) && !defined(existing.dests)) existing.dests = node.dests;
      if (defined(node.drops) && !defined(existing.drops)) existing.drops = node.drops;
      return newPath;
    }
    return updateAt(path, function(parent: Tree.Node) {
      parent.children.push(node);
    }) ? newPath : undefined;
  }

  function addNodes(nodes: Tree.Node[], path: Tree.Path): Tree.Path | undefined {
    var node = nodes[0];
    if (!node) return path;
    const newPath = addNode(node, path);
    return newPath ? addNodes(nodes.slice(1), newPath) : undefined;
  }

  function deleteNodeAt(path: Tree.Path): void {
    var parent = nodeAtPath(treePath.init(path));
    var id = treePath.last(path);
    ops.removeChild(parent, id);
  }

  function promoteAt(path: Tree.Path, toMainline: boolean): void {
    var nodes = getNodeList(path);
    for (var i = nodes.length - 2; i >= 0; i--) {
      var node = nodes[i + 1];
      var parent = nodes[i];
      if (parent.children[0].id !== node.id) {
        ops.removeChild(parent, node.id);
        parent.children.unshift(node);
        if (!toMainline) break;
      }
    }
  }

  function setCommentAt(comment: Tree.Comment, path: Tree.Path) {
    if (!comment.text) deleteCommentAt(comment.id, path);
    else updateAt(path, function(node) {
      node.comments = node.comments || [];
      var existing = node.comments.find(function(c) {
        return c.id === comment.id;
      });
      if (existing) existing.text = comment.text;
      else node.comments.push(comment);
    });
  }

  function deleteCommentAt(id: string, path: Tree.Path) {
    updateAt(path, function(node) {
      var comments = (node.comments || []).filter(function(c) {
        return c.id !== id
      });
      node.comments = comments.length ? comments : undefined;
    });
  }

  function setGlyphsAt(glyphs: Tree.Glyph[], path: Tree.Path) {
    updateAt(path, function(node) {
      node.glyphs = glyphs;
    });
  }

  function getParentClock(node: Tree.Node, path: Tree.Path) {
    if (!('parentClock' in node)) {
      var parent = path && nodeAtPath(treePath.init(path));
      if (!parent) node.parentClock = node.clock;
      else if (!('clock' in parent)) node.parentClock = undefined;
      else node.parentClock = parent.clock;
    }
    return node.parentClock;
  }

  return {
    root: root,
    ops: ops,
    firstPly: firstPly,
    lastPly: lastPly,
    nodeAtPath: nodeAtPath,
    getNodeList: getNodeList,
    getOpening: getOpening,
    updateAt: updateAt,
    addNode: addNode,
    addNodes: addNodes,
    addDests: function(dests: string, path: Tree.Path, opening?: Tree.Opening) {
      return updateAt(path, function(node: Tree.Node) {
        node.dests = dests;
        if (opening) node.opening = opening;
      });
    },
    setShapes: function(shapes: Tree.Shape[], path: Tree.Path) {
      return updateAt(path, function(node: Tree.Node) {
        node.shapes = shapes;
      });
    },
    setCommentAt: setCommentAt,
    deleteCommentAt: deleteCommentAt,
    setGlyphsAt: setGlyphsAt,
    pathIsMainline: pathIsMainline,
    lastMainlineNode: lastMainlineNode,
    pathExists: pathExists,
    deleteNodeAt: deleteNodeAt,
    promoteAt: promoteAt,
    getCurrentNodesAfterPly: getCurrentNodesAfterPly,
    merge: function(tree: Tree.Node) {
      ops.merge(root, tree);
    },
    removeCeval: function() {
      ops.updateAll(root, function(n) {
        delete n.ceval;
        delete n.threat;
      });
    },
    removeComputerVariations: function() {
      ops.mainlineNodeList(root).forEach(function(n) {
        n.children = n.children.filter(function(c) {
          return !c.comp;
        });
      });
    },
    getParentClock: getParentClock
  };
}
