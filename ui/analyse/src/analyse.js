module.exports = function(game, analysis) {

  var plyToTurn = function(ply) {
    return Math.floor((ply - 1) / 2) + 1;
  }

  var makeTurns = function(moves, fromPly) {
    if (moves.length < 1) return [];
    var turns = [];
    if (fromPly % 2 === 0) {
      turns.push({
        turn: plyToTurn(fromPly),
        black: {
          ply: fromPly,
          san: moves.shift()
        }
      });
      fromPly++;
    }
    for (var i = 0, nb = moves.length; i < nb; i += 2) {
      turns.push({
        turn: plyToTurn(fromPly + i),
        white: {
          ply: fromPly + i,
          san: moves[i]
        },
        black: moves[i + 1] ? {
          ply: fromPly + i + 1,
          san: moves[i + 1]
        } : null
      });
    }
    return turns;
  }

  var applyAnalysis = function(turns, analysed) {
    analysed.forEach(function(ana, i) {
      var ply = i + 1;
      var turn = plyToTurn(ply);
      var index = turn - 1;
      var color = ply % 2 === 0 ? 'black' : 'white';
      var move = turns[index][color];
      if (!move) return;
      if (ana.mate) move.mate = ana.mate;
      else if (ana.eval) move.eval = ana.eval;
      if (ana.comment) move.comments = [ana.comment];
      if (ana.variation) move.variations = [makeTurns(ana.variation.split(' '), ply)];
    });
  };

  this.turns = makeTurns(game.moves, 1);
  if (analysis) applyAnalysis(this.turns, analysis.moves);

  var moveList = function(path) {
    var turns = this.turns;
    var moves = [];
    path.forEach(function(step) {
      turns.forEach(function(turn) {
        ['white', 'black'].forEach(function(color) {
          if (!turn[color].ply) return;
          if (step.ply > turn[color].ply)
            moves.push(turn[color].san);
          else if (step.ply === turn[color].ply) {
            if (typeof step.variation !== 'undefined')
              turns = turn[color].variations[step.variation];
            else
              moves.push(turn[color].san);
          }
        });
      });
    });
    return moves;
  };
}
