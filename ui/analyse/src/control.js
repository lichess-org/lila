var path = require('./path');

function canGoForward(ctrl) {
  var tree = ctrl.analyse.tree;
  var ok = false;
  ctrl.vm.path.forEach(function(step) {
    for (i = 0, nb = tree.length; i < nb; i++) {
      var move = tree[i];
      if (step.ply === move.ply && step.variation) {
        tree = move.variations[step.variation - 1];
        break;
      } else ok = step.ply < move.ply;
    }
  });
  return ok;
}

function canEnterVariation(ctrl) {
  var tree = ctrl.analyse.tree;
  var ok = false;
  ctrl.vm.path.forEach(function(step) {
    for (i = 0, nb = tree.length; i < nb; i++) {
      var move = tree[i];
      if (step.ply === move.ply) {
        if (step.variation) {
          tree = move.variations[step.variation - 1];
          break;
        } else ok = move.variations.length > 0;
      }
    }
  });
  return ok;
}

module.exports = {

  canGoForward: canGoForward,

  next: function(ctrl) {
    if (!canGoForward(ctrl)) return;
    var p = ctrl.vm.path;
    p[p.length - 1].ply++;
    ctrl.userJump(p);
  },

  prev: function(ctrl) {
    var p = ctrl.vm.path;
    var len = p.length;
    if (len === 1) {
      if (p[0].ply === 0) return;
      p[0].ply--;
    } else {
      if (p[len - 1].ply > p[len - 2].ply) p[len - 1].ply--;
      else {
        p.pop();
        p[len - 2].variation = null;
        if (p[len - 2].ply > 1) p[len - 2].ply--;
      }
    }
    ctrl.userJump(p);
  },

  last: function(ctrl) {
    ctrl.userJump([{
      ply: ctrl.analyse.tree[ctrl.analyse.tree.length - 1].ply,
      variation: null
    }]);
  },

  first: function(ctrl) {
    ctrl.userJump([{
      ply: 0,
      variation: null
    }]);
  },

  enterVariation: function(ctrl) {
    if (canEnterVariation(ctrl))
      ctrl.userJump(path.withVariation(ctrl.vm.path, 1));
  },

  exitVariation: function(ctrl) {
    var p = ctrl.vm.path
    if (p.length > 1)
      ctrl.userJump(path.withoutVariation(p));
  }
};
