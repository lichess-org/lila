// returns null if not deep enough to know
function isDrawish(node) {
  var eval = node.ceval;
  if (!eval || eval.depth < 16) return null;
  return !eval.mate && Math.abs(eval.cp) < 150;
};
// returns null if not deep enough to know
function isWinning(node, goalCp) {
  var eval = node.ceval;
  if (!eval || eval.depth < 16) return null;
  var cp = eval.mate > 0 ? 9999 : (eval.mate < 0 ? -9999 : eval.cp);
  return goalCp > 0 ? cp >= goalCp : cp <= goalCp;
};

function isMate(root) {
  return root.gameOver() === 'checkmate';
};
function isMyMate(root) {
  return isMate(root) && root.turnColor() !== root.bottomColor();
};
function isTheirMate(root) {
  return isMate(root) && root.turnColor() === root.bottomColor();
};

// returns null = ongoing, true = win, false = fail
module.exports = function(root, goal, nbMoves) {
  if (isTheirMate(root)) return false;
  var node = root.vm.node;
  switch (goal.result) {
    case 'drawIn':
      if (isDrawish(node) === false) return false;
      if (nbMoves > goal.moves) return false;
      if (root.gameOver() === 'draw') return true;
      if (nbMoves === goal.moves) return isDrawish(node);
      break;
    case 'evalIn':
      if (nbMoves === goal.moves) return isWinning(node, goal.cp);
      break;
    case 'mateIn':
      if (nbMoves > goal.moves) return false;
      if (isMyMate(root)) return true;
      if (nbMoves === goal.moves) return false;
      break;
    case 'mate':
    default:
      if (isMyMate(root)) return true;
  }
  return null;
};
