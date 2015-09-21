var treePath = require('./path');
var defined = require('./util').defined;

module.exports = function(steps, analysis) {

  this.tree = steps;

  this.firstPly = function() {
    return this.tree[0].ply;
  }.bind(this);

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
  };

  this.getSteps = function(path) {
    var tree = this.tree;
    var steps = [];
    for (var j in path) {
      var p = path[j];
      for (var i = 0, nb = tree.length; i < nb; i++) {
        if (p.ply === tree[i].ply) {
          if (p.variation) {
            tree = tree[i].variations[p.variation - 1];
            break;
          }
          steps.push(tree[i]);
          return steps;
        } else steps.push(tree[i]);
      }
    }
  }.bind(this);

  this.getStepsAfterPly = function(path, ply) {
    return this.getSteps(path).filter(function(step) {
      return step.ply > ply;
    });
  }.bind(this);

  this.addStep = function(step, path) {
    var nextPath = treePath.withPly(path, treePath.currentPly(path) + 1);
    var tree = this.tree;
    var curStep = null;
    nextPath.forEach(function(p) {
      for (i = 0, nb = tree.length; i < nb; i++) {
        var step = tree[i];
        if (p.ply === step.ply) {
          if (p.variation) {
            tree = step.variations[p.variation - 1];
            break;
          } else curStep = step;
        } else if (p.ply < step.ply) break;
      }
    });
    if (curStep) {
      curStep.variations = curStep.variations || [];
      if (curStep.san === step.san) return nextPath;
      for (var i = 0; i < curStep.variations.length; i++) {
        if (curStep.variations[i][0].san === step.san)
          return treePath.withVariation(nextPath, i + 1);
      }
      curStep.variations.push([step]);
      return treePath.withVariation(nextPath, curStep.variations.length);
    }
    tree.push(step);
    return nextPath;
  }.bind(this);

  this.addSteps = function(steps, path) {
    var step = steps[0];
    if (!step) return path;
    var newPath = this.addStep(step, path);
    return this.addSteps(steps.slice(1), newPath);
  }.bind(this);

  this.addDests = function(dests, path) {
    var tree = this.tree;
    for (var j in path) {
      var p = path[j];
      for (var i = 0, nb = tree.length; i < nb; i++) {
        if (p.ply === tree[i].ply) {
          if (p.variation) {
            tree = tree[i].variations[p.variation - 1];
            break;
          }
          tree[i].dests = dests;
          return;
        }
      }
    }
  }.bind(this);

  this.addEval = function(path, eval) {
    var tree = this.tree;
    for (var j in path) {
      var p = path[j];
      for (var i = 0, nb = tree.length; i < nb; i++) {
        if (p.ply === tree[i].ply) {
          if (p.variation) {
            tree = tree[i].variations[p.variation - 1];
            break;
          }
          tree[i].eval = eval.cp;
          return;
        }
      }
    }
  }.bind(this);
}
