var treePath = require('./path');
var defined = require('./util').defined;

module.exports = function(steps, analysis) {

  this.tree = steps;

  this.getStep = function(path) {
    var tree = this.tree;
    for (var j in path) {
      var p = path[j];
      for (var i = 0, nb = tree.length; i < nb; i++) {
        if (p.ply === tree[i].ply) {
          if (p.variation) {
            tree = tree[i].variations[p.variation - 1];
            break;
          }
          return tree[i];
        }
      }
    }
  }

  // this.moveList = function(path) {
  //   var tree = this.tree;
  //   var moves = [];
  //   path.forEach(function(step) {
  //     for (var i = 0, nb = tree.length; i < nb; i++) {
  //       var move = tree[i];
  //       if (step.ply == move.ply && step.variation) {
  //         tree = move.variations[step.variation - 1];
  //         break;
  //       } else if (step.ply >= move.ply) moves.push(move.san);
  //       else break;
  //     }
  //   });
  //   return moves;
  // }.bind(this);

  this.explore = function(path, san) {
    var nextPath = treePath.withPly(path, treePath.currentPly(path) + 1);
    var tree = this.tree;
    var curMove = null;
    nextPath.forEach(function(step) {
      for (i = 0, nb = tree.length; i < nb; i++) {
        var move = tree[i];
        if (step.ply == move.ply) {
          if (step.variation) {
            tree = move.variations[step.variation - 1];
            break;
          } else curMove = move;
        } else if (step.ply < move.ply) break;
      }
    });
    if (curMove) {
      if (curMove.san == san) return nextPath;
      for (var i = 0; i < curMove.variations.length; i++) {
        if (curMove.variations[i][0].san == san) {
          return treePath.withVariation(nextPath, i + 1);
        }
      }
      curMove.variations.push([{
        ply: curMove.ply,
        san: san,
        comments: [],
        variations: []
      }]);
      return treePath.withVariation(nextPath, curMove.variations.length);
    }
    tree.push({
      ply: treePath.currentPly(nextPath),
      san: san,
      comments: [],
      variations: []
    });
    return nextPath;
  }.bind(this);
}
