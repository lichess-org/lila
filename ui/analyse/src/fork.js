var m = require('mithril');
var treeView = require('./tree/treeView');

module.exports = {
  ctrl: function(root) {
    var prev = null;
    var selected = 0;
    var displayed = function() {
      return root.vm.node.children.length > 1;
    };
    return {
      state: function() {
        var node = root.vm.node;
        if (!prev || prev.id !== node.id) {
          prev = node;
          selected = 0;
        }
        return {
          node: node,
          selected: selected,
          displayed: displayed()
        };
      },
      next: function() {
        if (displayed()) {
          selected = Math.min(root.vm.node.children.length - 1, selected + 1);
          return true;
        }
      },
      prev: function() {
        if (displayed()) {
          selected = Math.max(0, selected - 1);
          return true;
        }
      },
      proceed: function(id) {
        if (displayed()) {
          root.userJumpIfCan(root.vm.path + (id || root.vm.node.children[selected].id));
          return true;
        }
      }
    };
  },
  view: function(root, concealOf) {
    if (root.embed) return;
    var state = root.fork.state();
    if (!state.displayed) return;
    var isMainline = concealOf && root.tree.pathIsMainline(root.vm.path);
    return m('div.fork',
      state.node.children.map(function(node, i) {
        var conceal = isMainline && concealOf(true)(root.vm.path + node.id, node);
        if (!conceal) return m('move', {
          class: i === state.selected ? 'selected' : '',
          onclick: function() {
            root.fork.proceed(node.id);
          }
        }, [
          treeView.renderIndex(node.ply, true),
          treeView.renderMove(node)
        ]);
      })
    );
  }
};
