var treePath = require('./tree/path');
var empty = require('./util').empty;

function canEnterVariation(ctrl) {
  return ctrl.vm.node.children.length > 1;
}

module.exports = {

  canGoForward: function(ctrl) {
    return ctrl.vm.node.children.length > 0;
  },

  next: function(ctrl) {
    var child = ctrl.vm.node.children[0];
    if (!child) return;
    ctrl.userJump(ctrl.vm.path + child.id);
  },

  prev: function(ctrl) {
    ctrl.userJump(treePath.init(ctrl.vm.path));
  },

  last: function(ctrl) {
    ctrl.userJump(treePath.fromNodeList(ctrl.vm.mainline));
  },

  first: function(ctrl) {
    ctrl.userJump(treePath.root);
  },

  enterVariation: function(ctrl) {
    var child = ctrl.vm.node.children[1];
    if (!child) return;
    ctrl.userJump(ctrl.vm.path + child.id);
  },

  exitVariation: function(ctrl) {
    var cur = ctrl.vm.node;
    var nl = ctrl.vm.nodeList;
    for (var i = nl - 2; i >= 0; i--) {
      if (nl[i].children[0].id !== cur.id)
        ctrl.userJump(ctrl.tree.nodeListToPath(nl.slice(0, i)));
    }
  }
};
