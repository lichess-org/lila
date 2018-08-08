import { countGhosts } from 'draughtsground/fen'
import { fenCompare } from 'draughts'

function mainlineChild(node: Tree.Node): Tree.Node | undefined {
  return node.children[0];
}

export function withMainlineChild<T>(node: Tree.Node, f: (node: Tree.Node) => T): T | undefined {
  const next = mainlineChild(node);
  return next ? f(next) : undefined;
}

export function findInMainline(fromNode: Tree.Node, predicate: (node: Tree.Node) => boolean): Tree.Node | undefined {
  const findFrom = function(node: Tree.Node): Tree.Node | undefined {
    if (predicate(node)) return node;
    return withMainlineChild(node, findFrom);
  };
  return findFrom(fromNode);
}

// returns a list of nodes collected from the original one
export function collect(from: Tree.Node, pickChild: (node: Tree.Node) => Tree.Node | undefined): Tree.Node[] {
  let nodes = [from], n = from, c;
  while(c = pickChild(n)) {
    nodes.push(c);
    n = c;
  }
  return nodes;
}

function pickFirstChild(node: Tree.Node): Tree.Node | undefined {
  return node.children[0];
}

export function mainlineNodeList(from: Tree.Node): Tree.Node[] {
  return collect(from, pickFirstChild);
}

function collectVariations(variation: Tree.Node[], from: Tree.Node, pickChild: (node: Tree.Node) => Tree.Node[] | undefined): Tree.Node[][] {
    const children = pickChild(from);
    if (!children || children.length == 0) return [variation];
    const variations = Array<Tree.Node[]>();
    children.forEach(
        node => collectVariations(variation.concat([node]), node, pickChild).forEach(
            variation => { variations.push(variation); }));
    return variations;
}

function pickAllChildren(node: Tree.Node): Tree.Node[] | undefined {
    return node.children;
}

export function allVariationsNodeList(from: Tree.Node): Tree.Node[][] {
    return collectVariations([], from, pickAllChildren);
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
  for (let i in nodeList) {
    if (predicate(nodeList[i])) path += nodeList[i].id;
    else break;
  }
  return path;
}

export function removeChild(parent: Tree.Node, id: string): void {
  parent.children = parent.children.filter(function(n) {
    return n.id !== id;
  });
}

export function countChildrenAndComments(node: Tree.Node) {
  const count = {
    nodes: 1,
    comments: (node.comments || []).length
  };
  node.children.forEach(function(child) {
    const c = countChildrenAndComments(child);
    count.nodes += c.nodes;
    count.comments += c.comments;
  });
  return count;
}

export function copyNode(node: Tree.Node, copyChildren: boolean = false): Tree.Node {
    return {
        id: node.id,
        ply: node.ply,
        displayPly: node.displayPly,
        uci: node.uci,
        fen: node.fen,
        children: copyChildren ? node.children : new Array(),
        comments: node.comments,
        gamebook: node.gamebook,
        dests: node.dests,
        captLen: node.captLen,
        threat: node.threat,
        ceval: node.ceval,
        eval: node.eval,
        opening: node.opening,
        glyphs: node.glyphs,
        clock: node.clock,
        parentClock: node.parentClock,
        shapes: node.shapes,
        comp: node.comp,
        san: node.san,
        threefold: node.threefold,
        fail: node.fail,
        puzzle: node.puzzle
    } as Tree.Node;
}

export function adjustMergedPlies(merged: Tree.Node[]) {

    if (merged.length > 1) {

        var situation = "";
        for (let i = 0; i < merged.length; i++) {
            if (i != 0) situation += ", "
            situation += merged[i].ply + "/" + merged[i].displayPly;
        }
        alert(situation);

        const targetPly = merged[merged.length - 1].ply;
        for (let i = 0; i < merged.length - 1; i++) {
            if (merged[i].ply === targetPly) {
                merged[i].displayPly = merged[i].ply;
                merged[i].ply = merged[i].ply - 1;
            }
        }

        situation = "";
        for (let i = 0; i < merged.length; i++) {
            if (i != 0) situation += ", "
            situation += merged[i].ply + "/" + merged[i].displayPly;
        }
        alert(situation);

    }
}

export function expandMergedNodes(nodeList: Tree.Node[]): Tree.Node[] {
    var node: Tree.Node, nodes = [];
    for (var i in nodeList) {
        node = nodeList[i];
        if (node.mergedNodes && node.mergedNodes.length != 0) {
            for (var m of node.mergedNodes)
                nodes.push(m);
        }
        else nodes.push(node);
    }
    return nodes;
}

export function mergeExpandedNodes(parent: Tree.Node): Tree.Node {

    var mergedParent = copyNode(parent);
    if (parent.children.length !== 0) {

        //First child is mainline
        var newNode = parent.children[0];
        if (countGhosts(newNode.fen) > 0) 
            newNode.displayPly = newNode.ply + 1;

        if (countGhosts(parent.fen) > 0) {
            mergeNodes(mergedParent, newNode);
            mergedParent.children = newNode.children;
            mergedParent = mergeExpandedNodes(mergedParent);
        }
        else mergedParent.children.push(mergeExpandedNodes(newNode));

        for (var i = 1; i < parent.children.length; i++)
            mergedParent.children.push(mergeExpandedNodes(parent.children[i]));

    }
    return mergedParent;
}

export function mergeNodes(curNode: Tree.Node, newNode: Tree.Node, copyChildren = false) {

    if (curNode.mergedNodes)
        curNode.mergedNodes.push(copyNode(newNode, copyChildren));
    else
        curNode.mergedNodes = [copyNode(curNode, copyChildren), copyNode(newNode, copyChildren)];

    curNode.id = curNode.id.slice(0, 1) + newNode.id.slice(1, 2);
    curNode.fen = newNode.fen;

    if (curNode.dests && newNode.dests)
        curNode.dests = newNode.dests;

    if (curNode.san && newNode.san) {
        const curX = curNode.san.indexOf('x'), newX = newNode.san.indexOf('x');
        if (curX != -1 && newX != -1)
            curNode.san = curNode.san.slice(0, curX) + newNode.san.slice(newX);
    }

    if (curNode.uci && newNode.uci) {
        curNode.uci = curNode.uci + newNode.uci.substr(2, 2);
    }

    if (countGhosts(newNode.fen) == 0 && curNode.displayPly)
        curNode.ply = curNode.displayPly;

    curNode.puzzle = newNode.puzzle;
    curNode.eval = newNode.eval;
    if (newNode.glyphs) curNode.glyphs = newNode.glyphs;
    newNode.comments && newNode.comments.forEach(function (c) {
        if (!curNode.comments) curNode.comments = [c];
        else if (!curNode.comments.filter(function (d) {
            return d.text === c.text;
        }).length) curNode.comments.push(c);
    });

}

export function reconstruct(parts: any): Tree.Node {

    const root = parts[0], nb = parts.length;

    let prevNode: Tree.Node | undefined = undefined;
    let node = root, i: number;
    root.id = '';
    for (i = 1; i < nb; i++) {

        const n = parts[i];

        const ghosts = countGhosts(node.fen);
        if (ghosts !== 0) {

            if (node.mergedNodes)
                node.mergedNodes.push(n);
            else
                node.mergedNodes = [copyNode(node), n];

            node.id = node.id.slice(0, 1) + n.id.slice(1, 2);
            node.fen = n.fen;
            node.ply = n.ply;

            const curX = node.san.indexOf('x'), newX = n.san.indexOf('x');
            if (curX != -1 && newX != -1)
                node.san = node.san.slice(0, curX) + n.san.slice(newX);

            if (ghosts === 1)
                node.uci = node.uci.substr(0, 4) + n.uci.substr(2, 2);
            else
                node.uci = node.uci + n.uci.substr(2, 2);

            if (prevNode && node.san.indexOf('x') !== -1)
                prevNode.captLen = node.uci.length / 2 - 1;

        } else {

            if (prevNode && node.san.indexOf('x') !== -1)
                prevNode.captLen = node.uci.length / 2 - 1;

            if (countGhosts(n.fen) !== 0)
                n.displayPly = n.ply + 1;

            if (node.children) {
                node.children.forEach(function(child: Tree.Node) {
                    if (countGhosts(child.fen) !== 0)
                        child.displayPly = child.ply + 1;
                });
                node.children.unshift(n);
            }
            else node.children = [n];

            prevNode = node;
            node = n;

        }

    }

    node.children = node.children || [];
    return root;

}

// adds n2 into n1, returns if any halfway multicapture variation was merged, thus changing the currently played move
export function merge(n1: Tree.Node, n2: Tree.Node, n2Expanded?: Tree.Node): Tree.Node | undefined {
    n1.eval = n2.eval;
    if (n2.glyphs) n1.glyphs = n2.glyphs;
    n2.comments && n2.comments.forEach(function (c) {
        if (!n1.comments) n1.comments = [c];
        else if (!n1.comments.filter(function (d) {
            return d.text === c.text;
        }).length) n1.comments.push(c);
    });
    var mergedChildren: Tree.Node | undefined = undefined;
    n2.children.forEach(function (c) {
        const existing = childById(n1, c.id);
        if (existing) mergedChildren = merge(existing, c, n2Expanded);
        else if (n2Expanded) {
            var ghostChild = false;
            for (var i = 0; !ghostChild && i < n1.children.length; i++) {
                if (countGhosts(n1.children[i].fen) > 0) {
                    var expandedChild = n2Expanded.children.length != 0 ? n2Expanded.children[0] : undefined;
                    while (expandedChild && !fenCompare(expandedChild.fen, n2.fen))
                        expandedChild = expandedChild.children.length != 0 ? expandedChild.children[0] : undefined;
                    if (expandedChild && fenCompare(expandedChild.fen, n2.fen) && expandedChild.children.length != 0 && countGhosts(expandedChild.children[0].fen) > 0) {
                        //Found the corresponding node in the expanded tree
                        var walkPly = expandedChild.children[0].ply;
                        var childNode: Tree.Node | undefined = expandedChild.children[0];
                        var matchNode = copyNode(childNode);
                        while (childNode && childNode.ply <= walkPly && !fenCompare(matchNode.fen, n1.children[i].fen)) {
                            childNode = childNode.children.length != 0 ? childNode.children[0] : undefined;
                            if (childNode) {
                                mergeNodes(matchNode, childNode);
                            }
                        }
                        if (fenCompare(matchNode.fen, n1.children[i].fen)) {
                            while (childNode && childNode.ply <= walkPly) {
                                childNode = childNode.children.length != 0 ? childNode.children[0] : undefined;
                                if (childNode) {
                                    mergeNodes(n1.children[i], childNode);
                                }
                            }
                            const existing = childById(n1, c.id);
                            if (existing) {
                                merge(existing, c, n2Expanded);
                                mergedChildren = n1.children[i];
                                ghostChild = true;
                            }
                        }
                    }
                }
            }
            if (!ghostChild) n1.children.push(c);
        }
        else n1.children.push(c);
    });
    return mergedChildren;
}

export function hasBranching(node: Tree.Node, maxDepth: number): boolean {
  return maxDepth <= 0 || !!node.children[1] || (
    node.children[0] && hasBranching(node.children[0], maxDepth - 1)
  );
}

export function updateAll(root: Tree.Node, f: (node: Tree.Node) => void): void {
  // applies f recursively to all nodes
  function update(node: Tree.Node) {
    f(node);
    node.children.forEach(update);
  };
  update(root);
}
