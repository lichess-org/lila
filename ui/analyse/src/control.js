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

module.exports = {

  next: function(ctrl) {
    if (!canGoForward(ctrl)) return;
    var p = ctrl.vm.path;
    p[p.length - 1].ply++;
    ctrl.jump(p);
  },

  prev: function(ctrl) {
    var p = ctrl.vm.path;
    var len = p.length;
    if (len === 1) {
      if (p[0].ply === 1) return;
      p[0].ply--;
    } else {
      if (p[len - 1].ply > p[len - 2].ply) p[len - 1].ply--;
      else {
        p.pop();
        p[len - 2].variation = null;
        if (p[len - 2].ply > 1) p[len - 2].ply--;
      }
    }
    ctrl.jump(p);
  },

  last: function(ctrl) {
    ctrl.jump([{
      ply: ctrl.analyse.tree[ctrl.analyse.tree.length - 1].ply,
      variation: null
    }]);
  },

  first: function(ctrl) {
    ctrl.jump([{
      ply: 1,
      variation: null
    }]);
  }
};
