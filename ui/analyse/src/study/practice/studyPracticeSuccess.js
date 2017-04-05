// returns null if not deep enough to know
function isDrawish(node) {
  if (!hasSolidEval(node)) return null;
  return !node.ceval.mate && Math.abs(node.ceval.cp) < 150;
}
// returns null if not deep enough to know
function isWinning(node, goalCp, color) {
  if (!hasSolidEval(node)) return null;
  var cp = node.ceval.mate > 0 ? 99999 : (node.ceval.mate < 0 ? -99999 : node.ceval.cp);
  return color === 'white' ? cp >= goalCp : cp <= goalCp;
}
// returns null if not deep enough to know
function myMateIn(node, color) {
  if (!hasSolidEval(node)) return null;
  if (!node.ceval.mate) return false;
  var mateIn = node.ceval.mate * (color === 'white' ? 1 : -1);
  return mateIn > 0 ? mateIn : false;
}

function hasSolidEval(node) {
  return node.ceval && node.ceval.depth >= 16;
}

function isMate(root) {
  return root.gameOver() === 'checkmate';
}

function isMyMate(root) {
  return isMate(root) && root.turnColor() !== root.bottomColor();
}

function isTheirMate(root) {
  return isMate(root) && root.turnColor() === root.bottomColor();
}

function hasBlundered(comment) {
  return comment && (comment.verdict === 'mistake' || comment.verdict === 'blunder');
}

// returns null = ongoing, true = win, false = fail
module.exports = function(root, goal, nbMoves) {
  var node = root.vm.node;
  if (!node.uci) return null;
  if (isTheirMate(root)) return false;
  if (isMyMate(root)) return true;
  if (hasBlundered(root.practice.comment())) return false;
  switch (goal.result) {
    case 'drawIn':
    case 'equalIn':
      if (node.threefold) return true;
      if (isDrawish(node) === false) return false;
      if (nbMoves > goal.moves) return false;
      if (root.gameOver() === 'draw') return true;
      if (nbMoves >= goal.moves) return isDrawish(node);
      break;
    case 'evalIn':
      if (nbMoves >= goal.moves) return isWinning(node, goal.cp, root.bottomColor());
      break;
    case 'mateIn':
      if (nbMoves > goal.moves) return false;
      var mateIn = myMateIn(node, root.bottomColor());
      if (mateIn === null) return null;
      if (!mateIn || mateIn + nbMoves > goal.moves) return false;
      break;
    case 'promotion':
      if (!node.uci[4]) return null;
      return isWinning(node, goal.cp, root.bottomColor());
    case 'mate':
  }
  return null;
};
