import { Eval, winningChances } from 'ceval';
import { Prop, prop } from 'common/common';
import { path as treePath } from 'tree';
import AnalyseCtrl from '../ctrl';
import { Redraw } from '../interfaces';
import { detectFourfold } from '../nodeFinder';

declare type Verdict = 'goodMove' | 'inaccuracy' | 'mistake' | 'blunder';

export interface Comment {
  prev: Tree.Node;
  node: Tree.Node;
  path: Tree.Path;
  verdict: Verdict;
  best?: {
    usi: Usi;
  };
}

interface Hinting {
  mode: 'move' | 'piece';
  usi: Usi;
}

export interface PracticeCtrl {
  onCeval(): void;
  onJump(): void;
  isMyTurn(): boolean;
  comment: Prop<Comment | null>;
  running;
  hovering;
  hinting;
  resume;
  playableDepth;
  reset(): void;
  preUserJump(from: Tree.Path, to: Tree.Path): void;
  postUserJump(from: Tree.Path, to: Tree.Path): void;
  onUserMove(): void;
  playCommentBest(): void;
  commentShape(enable: boolean): void;
  hint(): void;
  currentNode(): Tree.Node;
  bottomColor(): Color;
  redraw: Redraw;
}

export function make(root: AnalyseCtrl, playableDepth: () => number): PracticeCtrl {
  const running = prop(true),
    comment = prop<Comment | null>(null),
    hovering = prop<any>(null),
    hinting = prop<Hinting | null>(null),
    played = prop(false);

  function ensureCevalRunning() {
    if (!root.showComputer()) root.toggleComputer();
    if (!root.ceval.enabled()) root.toggleCeval();
    if (root.threatMode()) root.toggleThreatMode();
  }

  function commentable(node: Tree.Node, bonus: number = 0): boolean {
    if (node.tbhit || root.outcome(node)) return true;
    const ceval = node.ceval;
    return ceval ? ceval.depth + bonus >= 15 || (ceval.depth >= 13 && !ceval.cloud && ceval.millis > 3000) : false;
  }

  function playable(node: Tree.Node): boolean {
    const ceval = node.ceval;
    return ceval
      ? ceval.depth >= Math.min(ceval.maxDepth || 99, playableDepth()) ||
          (ceval.depth >= 15 && (ceval.cloud || ceval.millis > 5000))
      : false;
  }

  function tbhitToEval(hit: Tree.TablebaseHit | undefined | null) {
    return (
      hit &&
      (hit.winner
        ? {
            mate: hit.winner === 'sente' ? 10 : -10,
          }
        : { cp: 0 })
    );
  }
  function nodeBestUsi(node: Tree.Node): Usi | undefined {
    const usi = (node.tbhit && node.tbhit.best) || (node.ceval && node.ceval.pvs[0].moves[0]);
    return usi;
  }

  function makeComment(prev: Tree.Node, node: Tree.Node, path: Tree.Path): Comment {
    let verdict: Verdict, best;
    const outcome = root.outcome(node);

    if (outcome && outcome.winner) verdict = 'goodMove';
    else {
      const nodeEval: Eval =
        tbhitToEval(node.tbhit) || (node.fourfold || (outcome && !outcome.winner) ? { cp: 0 } : (node.ceval as Eval));
      const prevEval: Eval = tbhitToEval(prev.tbhit) || prev.ceval!;
      const shift = -winningChances.povDiff(root.bottomColor(), nodeEval, prevEval);

      best = nodeBestUsi(prev)!;
      if (best === node.usi) best = null;

      if (!best) verdict = 'goodMove';
      else if (shift < 0.025) verdict = 'goodMove';
      else if (shift < 0.06) verdict = 'inaccuracy';
      else if (shift < 0.14) verdict = 'mistake';
      else verdict = 'blunder';
    }
    return {
      prev,
      node,
      path,
      verdict,
      best: best
        ? {
            usi: best,
          }
        : undefined,
    };
  }

  function isMyTurn(): boolean {
    return root.turnColor() === root.bottomColor();
  }

  function checkCeval() {
    const node = root.node;
    if (!running()) {
      comment(null);
      return root.redraw();
    }
    ensureCevalRunning();
    if (isMyTurn()) {
      const h = hinting();
      if (h) {
        h.usi = nodeBestUsi(node) || h.usi;
        root.setAutoShapes();
      }
    } else {
      comment(null);
      if (node.usi && commentable(node)) {
        const parentNode = root.tree.parentNode(root.path);
        if (commentable(parentNode, +1)) comment(makeComment(parentNode, node, root.path));
        else {
          /*
           * Looks like the parent node didn't get enough analysis time
           * to be commentable :-/ it can happen if the player premoves
           * or just makes a move before the position is sufficiently analysed.
           * In this case, fall back to comparing to the position before,
           * Since computer moves are supposed to preserve eval anyway.
           */
          const olderNode = root.tree.parentNode(treePath.init(root.path));
          if (commentable(olderNode, +1)) comment(makeComment(olderNode, node, root.path));
        }
      }
      if (!played() && playable(node)) {
        root.playUsi(nodeBestUsi(node)!);
        played(true);
      } else root.redraw();
    }
  }

  function resume() {
    running(true);
    checkCeval();
  }

  window.lishogi.requestIdleCallback(checkCeval);

  return {
    onCeval: checkCeval,
    onJump() {
      played(false);
      hinting(null);
      detectFourfold(root.nodeList, root.node);
      checkCeval();
    },
    isMyTurn,
    comment,
    running,
    hovering,
    hinting,
    resume,
    playableDepth,
    reset() {
      comment(null);
      hinting(null);
    },
    preUserJump(from: Tree.Path, to: Tree.Path) {
      if (from !== to) {
        running(false);
        comment(null);
      }
    },
    postUserJump(from: Tree.Path, to: Tree.Path) {
      if (from !== to && isMyTurn()) resume();
    },
    onUserMove() {
      running(true);
    },
    playCommentBest() {
      const c = comment();
      if (!c) return;
      root.jump(treePath.init(c.path));
      if (c.best) root.playUsi(c.best.usi);
    },
    commentShape(enable: boolean) {
      const c = comment();
      if (!enable || !c || !c.best) hovering(null);
      else
        hovering({
          usi: c.best.usi,
        });
      root.setAutoShapes();
    },
    hint() {
      const best = root.node.ceval ? root.node.ceval.pvs[0].moves[0] : null,
        prev = hinting();
      if (!best || (prev && prev.mode === 'move')) hinting(null);
      else
        hinting({
          mode: prev ? 'move' : 'piece',
          usi: best,
        });
      root.setAutoShapes();
    },
    currentNode: () => root.node,
    bottomColor: root.bottomColor,
    redraw: root.redraw,
  };
}
