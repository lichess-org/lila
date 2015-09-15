module.exports = {
  all: function(ctrl) {
    var s = '';
    ctrl.analyse.getNodes(ctrl.vm.path).forEach(function(node, i) {
      if (i === 0) return;
      if (i % 2 === 1) s += ((i + 1) / 2) + '. '
      else s += '';
      s += node.san + ' ';
    });
    return s.trim();
  },
  arraySince: function(ctrl, ply) {
    return ctrl.analyse.tree.filter(function(node, i) {
      return i > ply && i <= ctrl.vm.path[0].ply;
    }).map(function(node) {
      return node.san;
    });
  },
  renderSanSince: function(sans, ply) {
    var s = ply % 2 === 0 ? '' : ((ply + 1) / 2) + '...&nbsp;';
    sans.forEach(function(san, i) {
      var p = ply + i + 1;
      if (p % 2 === 1) s += ((p + 1) / 2) + '.&nbsp;'
      else s += '';
      s += san + ' ';
    });
    return s.trim();
  }
};
