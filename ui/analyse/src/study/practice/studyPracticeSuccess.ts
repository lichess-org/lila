import AnalyseCtrl from '../../ctrl';
import { Goal } from './interfaces';
import { Comment } from '../../practice/practiceCtrl';

// returns null if not deep enough to know
const isDrawish = (node: Tree.Node): boolean | null =>
  hasSolidEval(node) ? !node.ceval!.mate && Math.abs(node.ceval!.cp!) < 150 : null;

// returns null if not deep enough to know
const isWinning = (ctrl: AnalyseCtrl, node: Tree.Node, goalCp: number, color: Color): boolean | null => {
  if (!hasSolidEval(node)) {
    const pos = ctrl.position(node).unwrap();
    return pos.isStalemate() || pos.isInsufficientMaterial() ? false : null;
  }
  const cp = node.ceval!.mate! > 0 ? 99999 : node.ceval!.mate! < 0 ? -99999 : node.ceval!.cp;
  return color === 'white' ? cp! >= goalCp : cp! <= goalCp;
};

// returns null if not deep enough to know
const myMateIn = (node: Tree.Node, color: Color): number | boolean | null => {
  if (!hasSolidEval(node)) return null;
  if (!node.ceval!.mate) return false;
  const mateIn = node.ceval!.mate! * (color === 'white' ? 1 : -1);
  return mateIn > 0 ? mateIn : false;
};

const hasSolidEval = (node: Tree.Node) => node.ceval && node.ceval.depth >= 16;

const hasBlundered = (comment: Comment | null) =>
  comment && (comment.verdict === 'mistake' || comment.verdict === 'blunder');

// returns null = ongoing, true = win, false = fail
export default function (root: AnalyseCtrl, goal: Goal, nbMoves: number): boolean | null {
  const node = root.node;
  if (!node.uci) return null;
  const outcome = root.outcome();
  if (outcome && outcome.winner && outcome.winner !== root.bottomColor()) return false;
  if (outcome && outcome.winner && outcome.winner === root.bottomColor()) return true;
  if (hasBlundered(root.practice!.comment())) return false;
  switch (goal.result) {
    case 'drawIn':
    case 'equalIn':
      if (node.threefold) return true;
      if (isDrawish(node) === false) return false;
      if (nbMoves > goal.moves!) return false;
      if (outcome && !outcome.winner) return true;
      if (nbMoves >= goal.moves!) return isDrawish(node);
      break;
    case 'evalIn':
      if (nbMoves >= goal.moves!) return isWinning(root, node, goal.cp!, root.bottomColor());
      break;
    case 'mateIn': {
      if (nbMoves > goal.moves!) return false;
      const mateIn = myMateIn(node, root.bottomColor());
      if (mateIn === null) return null;
      if (!mateIn || (mateIn as number) + nbMoves > goal.moves!) return false;
      break;
    }
    case 'promotion':
      if (!node.uci[4]) return null;
      return isWinning(root, node, goal.cp!, root.bottomColor());
    case 'mate':
      if (node.threefold) return false;
      if (isDrawish(node)) return false;
  }
  return null;
}
