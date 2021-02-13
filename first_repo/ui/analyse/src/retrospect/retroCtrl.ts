import { opposite } from 'chessground/util';
import { evalSwings } from '../nodeFinder';
import { winningChances } from 'ceval';
import { path as treePath } from 'tree';
import { isEmpty, prop } from 'common';
import { OpeningData } from '../explorer/interfaces';
import AnalyseCtrl from '../ctrl';

export interface RetroCtrl {
  isSolving(): boolean;
  trans: Trans;
  [key: string]: any;
}

type Feedback = 'find' | 'eval' | 'win' | 'fail' | 'view';

export function make(root: AnalyseCtrl, color: Color): RetroCtrl {
  const game = root.data.game;
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
    const colorModulo = color == 'white' ? 1 : 0;
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
      openingUcis: [],
    });
    // fetch opening explorer moves
    if (
      game.variant.key === 'standard' &&
      game.division &&
      (!game.division.middle || fault.node.ply < game.division.middle)
    ) {
      root.explorer.fetchMasterOpening(prev.node.fen).then((res: OpeningData) => {
        const cur = current();
        const ucis: Uci[] = [];
        res!.moves.forEach(m => {
          if (m.white + m.draws + m.black > 1) ucis.push(m.uci);
        });
        if (ucis.includes(fault.node.uci!)) {
          explorerCancelPlies.push(fault.node.ply);
          setTimeout(jumpToNext, 100);
        } else {
          cur.openingUcis = ucis;
          current(cur);
        }
      });
    }
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
      if (cur.openingUcis.includes(node.uci)) onWin();
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
    return node.ceval ? node.ceval.depth >= 18 || (node.ceval.depth >= 14 && node.ceval.millis > 7000) : false;
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
    return (node.ply % 2 === 0) !== (color === 'white') && !isPlySolved(node.ply);
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
      if (root.data.game.variant.key !== 'racingKings') root.flip();
      else {
        root.retro = make(root, opposite(color));
        redraw();
      }
    },
    close: root.toggleRetro,
    trans: root.trans,
    noarg: root.trans.noarg,
    node: () => root.node,
    redraw,
  };
}
