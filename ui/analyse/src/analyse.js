module.exports = function(game, analysis) {

  var makeTree = function(sans, fromPly) {
    return sans.map(function(san, i) {
      return {
        ply: fromPly + i,
        san: san,
        comments: [],
        variations: []
      };
    });
  }

  var applyAnalysis = function(tree, analysed) {
    analysed.forEach(function(ana, i) {
      if (!tree[i]) return;
      if (ana.mate) tree[i].mate = ana.mate;
      else if (ana.eval) tree[i].eval = ana.eval;
      if (ana.comment) tree[i].comments.push(ana.comment);
      if (ana.variation) tree[i].variations.push(makeTree(ana.variation.split(' '), i + 1));
    });
  };

  this.tree = makeTree(game.moves, 1);
  if (analysis) applyAnalysis(this.tree, analysis.moves);

  this.moveList = function(path) {
    var tree = this.tree;
    var moves = [];
    path.forEach(function(step) {
      for (i = 0, nb = tree.length; i < nb; i++) {
        var move = tree[i];
        if (step.ply === move.ply && step.variation) {
          tree = move.variations[step.variation - 1];
          break;
        } else if (step.ply >= move.ply) moves.push(move.san);
        else break;
      }
    });
    return moves;
  }.bind(this);

  this.explore = function(path, san) {
    var tree = this.tree;
    var curMove = null;
    path.forEach(function(step) {
      for (i = 0, nb = tree.length; i < nb; i++) {
        var move = tree[i];
        if (step.ply === move.ply && step.variation) {
          tree = move.variations[step.variation - 1];
          break;
        } else if (step.ply == move.ply) curMove = move;
        else if (step.ply < move.ply) break;
      }
    });
    curMove.variations = [[{
      ply: curMove.ply + 1,
      san: san,
      comments: [],
      variations: []
    }]];
  }.bind(this);
}
