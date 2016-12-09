var treePath = require('tree').path;

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
  }
};
