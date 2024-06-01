import { winningChances } from 'ceval';
import { isEmpty, prop } from 'common/common';
import { opposite } from 'shogiground/util';
import { path as treePath } from 'tree';
import AnalyseCtrl from '../ctrl';
import { evalSwings } from '../nodeFinder';

export interface RetroCtrl {
  isSolving(): boolean;
  trans: Trans;
  [key: string]: any;
  variant: VariantKey;
  initialSfen: Sfen | undefined;
  offset: number;
}

type Feedback = 'find' | 'eval' | 'win' | 'fail' | 'view';

export function make(root: AnalyseCtrl, color: Color): RetroCtrl {
  let candidateNodes: Tree.Node[] = [];
  const explorerCancelPlies: number[] = [];
  let solvedPlies: number[] = [];
  const current = prop<any>(null);
  const feedback = prop<Feedback>('find');

  const redraw = root.redraw;

  function isPlySolved(ply: Ply): boolean {
    return solvedPlies.includes(ply);
  }

  function findNextNode(): Tree.Node | undefined {
    const colorModulo = color == 'sente' ? 1 : 0;
    candidateNodes = evalSwings(root.mainline, n => n.ply % 2 === colorModulo && !explorerCancelPlies.includes(n.ply));
    return candidateNodes.find(n => !isPlySolved(n.ply));
  }

  function jumpToNext(): void {
    feedback('find');
    const node = findNextNode();
    if (!node) {
      current(null);
      return redraw();
    }
    const fault = {
      node,
      path: root.mainlinePathToPly(node.ply),
    };
    const prevPath = treePath.init(fault.path);
    const prev = {
      node: root.tree.nodeAtPath(prevPath),
      path: prevPath,
    };
    const solutionNode = prev.node.children.find(n => !!n.comp);
    current({
      fault,
      prev,
      solution: {
        node: solutionNode,
        path: prevPath + solutionNode!.id,
      },
      openingUsis: [],
    });
    root.userJump(prev.path);
    redraw();
  }

  function onJump(): void {
    const node = root.node,
      fb = feedback(),
      cur = current();
    if (!cur) return;
    if (fb === 'eval' && cur.fault.node.ply !== node.ply) {
      feedback('find');
      root.setAutoShapes();
      return;
    }
    if (isSolving() && cur.fault.node.ply === node.ply) {
      if (cur.openingUsis.includes(node.usi)) onWin();
      // found in opening explorer
      else if (node.comp) onWin();
      // the computer solution line
      else if (node.eval) onFail();
      // the move that was played in the game
      else {
        feedback('eval');
        if (!root.ceval.enabled()) root.toggleCeval();
        checkCeval();
      }
    }
    root.setAutoShapes();
  }

  function isCevalReady(node: Tree.Node): boolean {
    return node.ceval ? node.ceval.depth >= 18 || (node.ceval.depth >= 14 && (node.ceval.millis ?? 0) > 7000) : false;
  }

  function checkCeval(): void {
    var node = root.node,
      cur = current();
    if (!cur || feedback() !== 'eval' || cur.fault.node.ply !== node.ply) return;
    if (isCevalReady(node)) {
      var diff = winningChances.povDiff(color, node.ceval!, cur.prev.node.eval);
      if (diff > -0.035) onWin();
      else onFail();
    }
  }

  function onWin(): void {
    solveCurrent();
    feedback('win');
    redraw();
  }

  function onFail(): void {
    feedback('fail');
    const bad = {
      node: root.node,
      path: root.path,
    };
    root.userJump(current().prev.path);
    if (!root.tree.pathIsMainline(bad.path) && isEmpty(bad.node.children)) root.tree.deleteNodeAt(bad.path);
    redraw();
  }

  function viewSolution() {
    feedback('view');
    root.userJump(current().solution.path);
    solveCurrent();
  }

  function skip() {
    solveCurrent();
    jumpToNext();
  }

  function solveCurrent() {
    solvedPlies.push(current().fault.node.ply);
  }

  function hideComputerLine(node: Tree.Node): boolean {
    return (node.ply % 2 === 0) !== (color === 'sente') && !isPlySolved(node.ply);
  }

  function showBadNode(): Tree.Node | undefined {
    const cur = current();
    if (cur && isSolving() && cur.prev.path === root.path) return cur.fault.node;
    return undefined;
  }

  function isSolving(): boolean {
    const fb = feedback();
    return fb === 'find' || fb === 'fail';
  }

  jumpToNext();

  function onMergeAnalysisData() {
    if (isSolving() && !current()) jumpToNext();
  }

  return {
    current,
    color,
    isPlySolved,
    onJump,
    jumpToNext,
    skip,
    viewSolution,
    hideComputerLine,
    showBadNode,
    onCeval: checkCeval,
    onMergeAnalysisData,
    feedback,
    isSolving,
    completion: () => [solvedPlies.length, candidateNodes.length],
    reset() {
      solvedPlies = [];
      jumpToNext();
    },
    flip() {
      root.retro = make(root, opposite(color));
      redraw();
    },
    close: root.toggleRetro,
    trans: root.trans,
    noarg: root.trans.noarg,
    node: () => root.node,
    redraw,
    variant: root.data.game.variant.key,
    initialSfen: root.data.game.initialSfen,
    offset: root.plyOffset(),
  };
}
