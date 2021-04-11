import { winningChances, Eval } from 'ceval';
import { path as treePath } from 'tree';
import { detectThreefold } from '../nodeFinder';
import { tablebaseGuaranteed } from '../explorer/explorerCtrl';
import AnalyseCtrl from '../ctrl';
import { Redraw } from '../interfaces';
import { defined, prop, Prop } from 'common';
import { altCastles } from 'chess';
import { parseUci } from 'chessops/util';
import { makeSan } from 'chessops/san';

declare type Verdict = 'goodMove' | 'inaccuracy' | 'mistake' | 'blunder';

export interface Comment {
  prev: Tree.Node;
  node: Tree.Node;
  path: Tree.Path;
  verdict: Verdict;
  best?: {
    uci: Uci;
    san: San;
  };
}

interface Hinting {
  mode: 'move' | 'piece';
  uci: Uci;
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
  const variant = root.data.game.variant.key,
    running = prop(true),
    comment = prop<Comment | null>(null),
    hovering = prop<any>(null),
    hinting = prop<Hinting | null>(null),
    played = prop(false);

  function ensureCevalRunning() {
    if (!root.showComputer()) root.toggleComputer();
    if (!root.ceval.enabled()) root.toggleCeval();
    if (root.threatMode()) root.toggleThreatMode();
  }

  function commentable(node: Tree.Node, bonus = 0): boolean {
    if (node.tbhit || root.outcome(node)) return true;
    const ceval = node.ceval;
    return ceval ? ceval.depth + bonus >= 15 || (ceval.depth >= 13 && ceval.millis > 3000) : false;
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
            mate: hit.winner === 'white' ? 10 : -10,
          }
        : { cp: 0 })
    );
  }
  function nodeBestUci(node: Tree.Node): Uci | undefined {
    return (node.tbhit && node.tbhit.best) || (node.ceval && node.ceval.pvs[0].moves[0]);
  }

  function makeComment(prev: Tree.Node, node: Tree.Node, path: Tree.Path): Comment {
    let verdict: Verdict, best;
    const outcome = root.outcome(node);

    if (outcome && outcome.winner) verdict = 'goodMove';
    else {
      const nodeEval: Eval =
        tbhitToEval(node.tbhit) || (node.threefold || (outcome && !outcome.winner) ? { cp: 0 } : (node.ceval as Eval));
      const prevEval: Eval = tbhitToEval(prev.tbhit) || prev.ceval!;
      const shift = -winningChances.povDiff(root.bottomColor(), nodeEval, prevEval);

      best = nodeBestUci(prev)!;
      if (best === node.uci || (node.san!.startsWith('O-O') && best === altCastles[node.uci!])) best = null;

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
            uci: best,
            san: root.position(prev).unwrap(
              pos => makeSan(pos, parseUci(best)!),
              _ => '--'
            ),
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
    if (tablebaseGuaranteed(variant, node.fen) && !defined(node.tbhit)) return;
    ensureCevalRunning();
    if (isMyTurn()) {
      const h = hinting();
      if (h) {
        h.uci = nodeBestUci(node) || h.uci;
        root.setAutoShapes();
      }
    } else {
      comment(null);
      if (node.san && commentable(node)) {
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
        root.playUci(nodeBestUci(node)!);
        played(true);
      } else root.redraw();
    }
  }

  function checkCevalOrTablebase() {
    if (tablebaseGuaranteed(variant, root.node.fen))
      root.explorer.fetchTablebaseHit(root.node.fen).then(
        hit => {
          if (hit && root.node.fen === hit.fen) root.node.tbhit = hit;
          checkCeval();
        },
        () => {
          if (!defined(root.node.tbhit)) root.node.tbhit = null;
          checkCeval();
        }
      );
    else checkCeval();
  }

  function resume() {
    running(true);
    checkCevalOrTablebase();
  }

  lichess.requestIdleCallback(checkCevalOrTablebase, 800);

  return {
    onCeval: checkCeval,
    onJump() {
      played(false);
      hinting(null);
      detectThreefold(root.nodeList, root.node);
      checkCevalOrTablebase();
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
      if (c.best) root.playUci(c.best.uci);
    },
    commentShape(enable: boolean) {
      const c = comment();
      if (!enable || !c || !c.best) hovering(null);
      else
        hovering({
          uci: c.best.uci,
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
          uci: best,
        });
      root.setAutoShapes();
    },
    currentNode: () => root.node,
    bottomColor: root.bottomColor,
    redraw: root.redraw,
  };
}
