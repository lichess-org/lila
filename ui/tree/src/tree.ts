import * as treePath from './path';
import * as ops from './ops';
import { defined } from 'common';
import { countGhosts } from 'draughtsground/fen'
import { readCaptureLength, san2alg } from 'draughts';

export type MaybeNode = Tree.Node | undefined;

export interface TreeWrapper {
  root: Tree.Node;
  lastPly(displayPly?: boolean): number;
  nodeAtPath(path: Tree.Path): Tree.Node;
  getNodeList(path: Tree.Path): Tree.Node[];
  longestValidPath(path: string): Tree.Path;
  getOpening(nodeList: Tree.Node[]): Tree.Opening | undefined;
  updateAt(path: Tree.Path, update: (node: Tree.Node) => void): MaybeNode;
  setAmbs(node: Tree.Node, parent: Tree.Node): void;
  addNode(node: Tree.Node, path: Tree.Path, puzzleEditor: Boolean, coordSystem?: number): Tree.Path | undefined;
  addNodes(nodes: Tree.Node[], path: Tree.Path): Tree.Path | undefined;
  addDests(dests: string, path: Tree.Path, opening?: Tree.Opening, alternatives?: Tree.Alternative[], destsUci?: Uci[]): MaybeNode;
  setShapes(shapes: Tree.Shape[], path: Tree.Path): MaybeNode;
  setCommentAt(comment: Tree.Comment, path: Tree.Path): MaybeNode;
  deleteCommentAt(id: string, path: Tree.Path): MaybeNode;
  setGlyphsAt(glyphs: Tree.Glyph[], path: Tree.Path): MaybeNode;
  setClockAt(clock: Tree.Clock | undefined, path: Tree.Path): MaybeNode;
  pathIsMainline(path: Tree.Path): boolean;
  lastMainlineNode(path: Tree.Path): Tree.Node;
  pathExists(path: Tree.Path): boolean;
  deleteNodeAt(path: Tree.Path): void;
  promoteAt(path: Tree.Path, toMainline: boolean): void;
  getCurrentNodesAfterPly(nodeList: Tree.Node[], mainline: Tree.Node[], ply: number): Tree.Node[];
  merge(tree: Tree.Node): void;
  removeCeval(): void;
  removeComputerVariations(): void;
  parentNode(path: Tree.Path): Tree.Node;
  getParentClock(node: Tree.Node, path: Tree.Path): Tree.Clock | undefined;
}

export function build(root: Tree.Node): TreeWrapper {

  function lastNode(): Tree.Node {
    return ops.findInMainline(root, function (node: Tree.Node) {
      return !node.children.length;
    })!;
  }

  function nodeAtPath(path: Tree.Path): Tree.Node {
    return nodeAtPathFrom(root, path);
  }

  function nodeAtPathFrom(node: Tree.Node, path: Tree.Path): Tree.Node {
    if (path === '') return node;
    const tail = treePath.tail(path);
    const child = ops.childById(node, treePath.head(path));
    return child ? nodeAtPathFrom(child, tail) : node;
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
    var id = treePath.head(path);
    const child = ops.childById(node, id);
    return child ? id + longestValidPathFrom(child, treePath.tail(path)) : '';
  }

  function getCurrentNodesAfterPly(nodeList: Tree.Node[], mainline: Tree.Node[], ply: number): Tree.Node[] {
    var node, nodes = [];
    for (var i in nodeList) {
      node = nodeList[i];
      const nodePly = node.displayPly ? node.displayPly : node.ply;
      if (nodePly <= ply && mainline[i].id !== node.id) break;
      if (nodePly > ply) nodes.push(node);
    }
    return nodes;
  };

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

  function getOpening(nodeList: Tree.Node[]): Tree.Opening | undefined {
    var opening: Tree.Opening | undefined;
    nodeList.forEach(function (node: Tree.Node) {
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
    return;
  }

  function getChildIndex(parent: Tree.Node, findChild: Tree.Node): number {
    for (let i = 0; i < parent.children.length; i++) {
      const child = parent.children[i];
      if (child.id === findChild.id && child.uci === findChild.uci && child.fen === findChild.fen && child.san === findChild.san)
        return i;
    }
    return -1;
  }

  function setAmbs(node: Tree.Node, parent: Tree.Node): void {
    //Hardcoded corresponding server ambiguity ids for studies, ugly solution but leads to displaying a path matching the server
    var ambs = 1
    do {
      node.id = node.id.substr(0, 1) + String.fromCharCode(35 + 50 + ambs);
      ambs++;
    } while (parent.children.some(child => child !== node && child.id === node.id));
  }

  // returns new path
  function addNode(newNode: Tree.Node, path: Tree.Path, puzzleEditor: Boolean = false, coordSystem?: number): Tree.Path | undefined {

    var newPath = path + newNode.id;
    var existing = nodeAtPathOrNull(newPath);

    const newGhosts = countGhosts(newNode.fen);
    if (existing && newGhosts > 0) {
      //new node might be an immediate ambiguity
      const parent = nodeAtPathOrNull(path);
      if (parent && parent.children.some(child => child.san === newNode.san && child.fen !== newNode.fen)) {
        setAmbs(newNode, parent);
        newPath = path + newNode.id;
        existing = nodeAtPathOrNull(newPath);
      }
    }

    if (existing) {
      if (defined(newNode.dests) && !defined(existing.dests)) existing.dests = newNode.dests;
      if (defined(newNode.destsUci) && !defined(existing.destsUci)) existing.destsUci = newNode.destsUci;
      if (defined(newNode.drops) && !defined(existing.drops)) existing.drops = newNode.drops;
      if (defined(newNode.clock) && !defined(existing.clock)) existing.clock = newNode.clock;
      return newPath;
    }

    if (newGhosts > 0)
      newNode.displayPly = newNode.ply + 1;

    const curNode = nodeAtPathOrNull(path);
    if (curNode && curNode.uci && countGhosts(curNode.fen) > 0) {

      const parent = (path.length >= 2) ? nodeAtPathOrNull(path.substr(0, path.length - 2)) : undefined;
      const nodeIndex = parent ? getChildIndex(parent, curNode) : -1;

      //Merge new node properties with head of line curnode
      ops.mergeNodes(curNode, newNode, false, coordSystem);
      newNode.uci = curNode.uci; //Pass back new uci to determine accurate path (no ambiguities)

      //If the capture sequence is now equal to another same level sibling in all relevant ways we remove the current node as it is a duplicate
      if (parent && nodeIndex != -1) {

        var duplicateIndex = -1;
        for (let i = 0; i < parent.children.length; i++) {
          if (i !== nodeIndex) {
            const child = parent.children[i];
            if (child.san === curNode.san && child.fen === curNode.fen && (!puzzleEditor || child.uci === curNode.uci)) {
              duplicateIndex = i;
              break;
            }
          }
        }

        if (duplicateIndex !== -1) {
          //We merged to an existing node, overwrite with the current uci as the capture-path might have changed
          parent.children[duplicateIndex].uci = curNode.uci;
          //Keep the existing id
          curNode.id = parent.children[duplicateIndex].id;
          //Remove our (empty) copy
          parent.children.splice(nodeIndex, 1);
        } else if (parent.children.some(child => child.san === curNode.san && (child.fen !== curNode.fen || (puzzleEditor && child.uci !== curNode.uci))))
          setAmbs(curNode, parent);

      }

      if (path.length < 2)
        return curNode.id;
      else
        return path.substr(0, path.length - 2) + curNode.id;

    } else if (!curNode && path.length >= 2) {
      const parent = nodeAtPathOrNull(path.substr(0, path.length - 2));
      if (parent && parent.captLen && parent.captLen > 1 && parent.children.length != 0) {
        // verify node was previously delivered and merged already
        existing = parent.children.find(function(c) { return c.fen === newNode.fen && c.san === newNode.san; });
        if (existing) {
          if (defined(newNode.dests) && !defined(existing.dests)) existing.dests = newNode.dests;
          if (defined(newNode.destsUci) && !defined(existing.destsUci)) existing.destsUci = newNode.destsUci;
          if (defined(newNode.drops) && !defined(existing.drops)) existing.drops = newNode.drops;
          if (defined(newNode.clock) && !defined(existing.clock)) existing.clock = newNode.clock;
          return path.substr(0, path.length - 2) + existing.id;
        }
      }
    }

    if (coordSystem) {
      newNode.alg = san2alg(newNode.san);
    }

    return updateAt(path, function (parent: Tree.Node) {
      parent.children.push(newNode);
    }) ? newPath : undefined;

  }

  function addNodes(nodes: Tree.Node[], path: Tree.Path, puzzleEditor: Boolean = false, coordSystem?: number): Tree.Path | undefined {
    var node = nodes[0];
    if (!node) return path;
    const newPath = addNode(node, path, puzzleEditor, coordSystem);
    return newPath ? addNodes(nodes.slice(1), newPath, puzzleEditor, coordSystem) : undefined;
  }

  function deleteNodeAt(path: Tree.Path): void {
    ops.removeChild(parentNode(path), treePath.last(path));
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
    return !comment.text ? deleteCommentAt(comment.id, path) : updateAt(path, function (node) {
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
      var comments = (node.comments || []).filter(function (c) {
        return c.id !== id
      });
      node.comments = comments.length ? comments : undefined;
    });
  }

  function setGlyphsAt(glyphs: Tree.Glyph[], path: Tree.Path) {
    return updateAt(path, function (node) {
      node.glyphs = glyphs;
    });
  }

  function setClockAt(clock: Tree.Clock | undefined, path: Tree.Path) {
    return updateAt(path, function (node) {
      node.clock = clock;
    });
  }

  function parentNode(path: Tree.Path): Tree.Node {
    return nodeAtPath(treePath.init(path));
  }

  function getParentClock(node: Tree.Node, path: Tree.Path): Tree.Clock | undefined {
    if (!('parentClock' in node) || !node.parentClock) {
      const par = path && parentNode(path);
      if (!par) node.parentClock = node.clock;
      else if (!('clock' in par)) node.parentClock = undefined;
      else node.parentClock = par.clock;
    }
    return node.parentClock;
  }

  return {
    root,
    lastPly(displayPly?: boolean): number {
      const n = lastNode();
      return (displayPly && n.displayPly) ? n.displayPly : n.ply;
    },
    nodeAtPath,
    getNodeList,
    longestValidPath: (path: string) => longestValidPathFrom(root, path),
    getOpening,
    updateAt,
    setAmbs,
    addNode,
    addNodes,
    addDests(dests: string, path: Tree.Path, opening?: Tree.Opening, alternatives?: Tree.Alternative[], destsUci?: Uci[]) {
      return updateAt(path, function (node: Tree.Node) {
        if (dests.length > 1 && dests[0] === '#')
          node.captLen = readCaptureLength(dests);
        node.dests = dests;
        if (opening) node.opening = opening;
        if (alternatives) node.alternatives = alternatives;
        if (destsUci) node.destsUci = destsUci;
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
    setClockAt,
    pathIsMainline,
    lastMainlineNode(path: Tree.Path): Tree.Node {
      return lastMainlineNodeFrom(root, path);
    },
    pathExists,
    deleteNodeAt,
    promoteAt,
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
    getParentClock
  };

}
