var defined = require('./util').defined;

function toPov(color, diff) {
  return color === 'white' ? diff : -diff;
}

function cpWinningChances(cp) {
  var normalized = Math.min(Math.max(-1000, cp), 1000);
  return 2 / (1 + Math.exp(-0.005 * normalized)) - 1;
}

function mateWinningChances(mate) {
  var cp = (21 - Math.min(10, Math.abs(mate))) * 100;
  var signed = cp * (mate > 0 ? 1 : -1);
  return cpWinningChances(signed);
}

function evalWinningChances(eval) {
  return defined(eval.cp) ?
    cpWinningChances(eval.cp) :
    mateWinningChances(eval.mate);
}

function povChances(color, eval) {
  return toPov(color, evalWinningChances(eval));
}

module.exports = {
  // winning chances for a color
  // 1  infinitely winning
  // -1 infinitely losing
  povChances: povChances,
  // computes the difference, in winning chances, between two evaluations
  // 1  = e1 is infinately better than e2
  // -1 = e1 is infinately worse  than e2
  povDiff: function(color, e1, e2) {
    return (povChances(color, e1) - povChances(color, e2)) / 2;
  }
}
