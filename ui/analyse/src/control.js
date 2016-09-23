var treePath = require('./tree/path');

function canEnterVariation(ctrl) {
  return ctrl.vm.node.children.length > 1;
}

function sharedStart(p1, p2) {
  var L = p1.length,
    i = 0;
  while (i < L && p1.charAt(i) === p2.charAt(i)) i++;
  return p1.substring(0, i);
}

module.exports = {

  canGoForward: function(ctrl) {
    return ctrl.vm.node.children.length > 0;
  },

  next: function(ctrl) {
    var child = ctrl.vm.node.children[0];
    if (!child) return;
    ctrl.userJumpIfCan(ctrl.vm.path + child.id);
  },

  prev: function(ctrl) {
    ctrl.userJumpIfCan(treePath.init(ctrl.vm.path));
  },

  last: function(ctrl) {
    ctrl.userJumpIfCan(treePath.fromNodeList(ctrl.vm.mainline));
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
    if (ctrl.tree.pathIsMainline(ctrl.vm.path)) return;
    var found, path = treePath.root;
    ctrl.vm.nodeList.slice(1, -1).forEach(function(n) {
      path += n.id;
      if (n.children[1]) found = path;
    });
    if (found) ctrl.userJump(found);
  }
};
