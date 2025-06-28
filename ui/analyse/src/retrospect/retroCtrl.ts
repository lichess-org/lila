import { opposite } from '@lichess-org/chessground/util';
import { evalSwings } from '../nodeFinder';
import { winningChances } from 'lib/ceval/ceval';
import { path as treePath } from 'lib/tree/tree';
import { isEmpty, type Prop, prop } from 'lib';
import type { OpeningData } from '../explorer/interfaces';
import type AnalyseCtrl from '../ctrl';

export interface RetroCtrl {
  isSolving(): boolean;
  current: Prop<Retrospection | null>;
  feedback: Prop<Feedback>;
  color: Color;
  isPlySolved(ply: Ply): boolean;
  onJump(): void;
  jumpToNext(): void;
  skip(): void;
  viewSolution(): void;
  hideComputerLine(node: Tree.Node): boolean;
  showBadNode(): Tree.Node | undefined;
  onCeval(): void;
  onMergeAnalysisData(): void;
  completion(): [number, number];
  reset(): void;
  flip(): void;
  preventGoingToNextMove(): boolean;
  close(): void;
  node(): Tree.Node;
  redraw: Redraw;
}

interface NodeWithPath {
  node: Tree.Node;
  path: string;
}

interface Retrospection {
  fault: NodeWithPath;
  prev: NodeWithPath;
  solution: NodeWithPath;
  openingUcis: Uci[];
}

type Feedback = 'find' | 'eval' | 'win' | 'fail' | 'view' | 'offTrack';

export function make(root: AnalyseCtrl, color: Color): RetroCtrl {
  const game = root.data.game;
  let candidateNodes: Tree.Node[] = [];
  const explorerCancelPlies: number[] = [];
  let solvedPlies: number[] = [];
  const current = prop<Retrospection | null>(null);
  const feedback = prop<Feedback>('find');

  function safeRedraw() {
    if (!site.blindMode) root.redraw();
  }

  function isPlySolved(ply: Ply): boolean {
    return solvedPlies.includes(ply);
  }

  function findNextNode(): Tree.Node | undefined {
    const colorModulo = color === 'white' ? 1 : 0;
    candidateNodes = evalSwings(
      root.mainline,
      n => n.ply % 2 === colorModulo && !explorerCancelPlies.includes(n.ply),
    );
    return candidateNodes.find(n => !isPlySolved(n.ply));
  }

  function jumpToNext(): void {
    feedback('find');
    const node = findNextNode();
    if (!node) {
      current(null);
      return safeRedraw();
    }
    const fault = {
      node,
      path: root.mainlinePlyToPath(node.ply),
    };
    const prevPath = treePath.init(fault.path);
    const prev = {
      node: root.tree.nodeAtPath(prevPath),
      path: prevPath,
    };
    const solutionNode = prev.node.children.find(n => !!n.comp)!;
    current({
      fault,
      prev,
      solution: {
        node: solutionNode,
        path: prevPath + solutionNode.id,
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
        const cur = current()!;
        const ucis: Uci[] = [];
        res.moves.forEach(m => {
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
    safeRedraw();
  }

  function onJump(): void {
    const node = root.node,
      fb = feedback(),
      cur = current();
    if (!cur) return;
    if (
      (fb === 'eval' && cur.fault.node.ply !== node.ply) ||
      (fb === 'offTrack' && cur.prev.path === root.path)
    ) {
      feedback('find');
      root.setAutoShapes();
      return;
    }
    if (isSolving() && cur.fault.node.ply === node.ply) {
      if (cur.openingUcis.includes(node.uci!) || node.san?.endsWith('#') || node.comp)
        onWin(); // found in opening explorer, checkmate ends the game, or comp solution line
      else if (node.eval)
        onFail(); // the move that was played in the game
      else {
        feedback('eval');
        if (!root.ceval.enabled()) root.toggleCeval();
        checkCeval();
      }
    } else if (isSolving() && cur.prev.path !== root.path) feedback('offTrack');
    root.setAutoShapes();
  }

  function isCevalReady(node: Tree.Node): boolean {
    return node.ceval
      ? node.ceval.depth >= 18 || (node.ceval.depth >= 14 && (node.ceval.millis ?? 0) > 6000)
      : false;
  }

  function checkCeval(): void {
    const node = root.node,
      cur = current();
    if (!cur || feedback() !== 'eval' || cur.fault.node.ply !== node.ply) return;
    if (isCevalReady(node)) {
      const diff = winningChances.povDiff(color, node.ceval!, cur.prev.node.eval!);
      if (diff > -0.04) onWin();
      else onFail();
    }
  }

  function onWin(): void {
    solveCurrent();
    if (site.blindMode) jumpToNext();
    feedback('win');
    safeRedraw();
  }

  function onFail(): void {
    feedback('fail');
    const bad = {
      node: root.node,
      path: root.path,
    };
    root.userJump(current()!.prev.path);
    if (!root.tree.pathIsMainline(bad.path) && isEmpty(bad.node.children)) root.tree.deleteNodeAt(bad.path);
    safeRedraw();
  }

  function viewSolution() {
    feedback('view');
    root.userJump(current()!.solution.path);
    solveCurrent();
  }

  function skip() {
    solveCurrent();
    jumpToNext();
  }

  function solveCurrent() {
    if (current()) solvedPlies.push(current()!.fault.node.ply);
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
        safeRedraw();
      }
    },
    preventGoingToNextMove: () => {
      const cur = current();
      return isSolving() && !!cur && root.path === cur.prev.path;
    },
    close: root.toggleRetro,
    node: () => root.node,
    redraw: root.redraw,
  };
}
