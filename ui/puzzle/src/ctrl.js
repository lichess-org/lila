var m = require('mithril');
var makeTree = require('tree').tree;
var treeOps = require('tree').ops;
var cevalCtrl = require('ceval').ctrl;
var k = Mousetrap;
var chessground = require('chessground');
var partial = chessground.util.partial;
var xhr = require('./xhr');

module.exports = function(data, i18n) {

  var vm = {
    loading: false
  };

  var tree = makeTree(treeOps.reconstruct(data.game.treeParts));

  var setPath = function(path) {
    vm.path = path;
    vm.nodeList = tree.getNodeList(path);
    vm.node = treeOps.last(vm.nodeList);
    vm.mainline = treeOps.mainlineNodeList(tree.root);
  }.bind(this);

  setPath('');

  setPath(initialPath);

  console.log(data);

  return {
    vm: vm,
    trans: lichess.trans(i18n)
  };
}
