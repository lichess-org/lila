module.exports = {
  all: function(ctrl) {
    var s = '';
    ctrl.analyse.tree.forEach(function(node, i) {
      if (i === 0) return;
      if (i > ctrl.vm.path[0].ply) return;
      if (i % 2 === 1) s += ((i + 1) / 2) + '. '
      else s += '';
      s += node.san + ' ';
    });
    return s.trim();
  },
  since: function(ctrl, ply) {
    var s = '';
    ctrl.analyse.tree.forEach(function(node, i) {
      if (i === 0 || i <= ply) return;
      if (i > ctrl.vm.path[0].ply) return;
      if (i % 2 === 1) s += ((i + 1) / 2) + '. '
      else s += '';
      s += node.san + ' ';
    });
    return s.trim();
  }
};
