// returns null if not deep enough to know
function isDrawish(node) {
  if (!hasSolidEval(node)) return null;
  return !node.ceval.mate && Math.abs(node.ceval.cp) < 150;
}
// returns null if not deep enough to know
function isWinning(node, goalCp) {
  if (!hasSolidEval(node)) return null;
  var cp = node.ceval.mate > 0 ? 99999 : (node.ceval.mate < 0 ? -99999 : node.ceval.cp);
  return goalCp > 0 ? cp >= goalCp : cp <= goalCp;
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

// returns null = ongoing, true = win, false = fail
module.exports = function(root, goal, nbMoves) {
  if (isTheirMate(root)) return false;
  var node = root.vm.node;
  if (!node.uci) return null;
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
