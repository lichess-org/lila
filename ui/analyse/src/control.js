var treePath = require('./tree/path');

function canEnterVariation(ctrl) {
  return ctrl.vm.node.children.length > 1;
}
function jumpToPath(ctrl, path) {
  if (ctrl.canJumpTo(path)) ctrl.userJump(path);
}

module.exports = {

  canGoForward: function(ctrl) {
    return ctrl.vm.node.children.length > 0;
  },

  next: function(ctrl) {
    var child = ctrl.vm.node.children[0];
    if (!child) return;
    jumpToPath(ctrl, ctrl.vm.path + child.id);
  },

  prev: function(ctrl) {
    jumpToPath(ctrl, treePath.init(ctrl.vm.path));
  },

  last: function(ctrl) {
    ctrl.jumpToLast();
    jumpToPath(ctrl, treePath.fromNodeList(cctrl.vm.mainline));
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
