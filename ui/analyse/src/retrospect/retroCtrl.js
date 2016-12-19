var nodeFinder = require('../nodeFinder');

module.exports = function(root) {

  var color = root.bottomColor();
  var solvedPlies = [];
  var node;

  var isPlySolved = function(ply) {
    return $.fp.contains(solvedPlies, ply)
  };

  var candidateNodes = function() {
    var c = root.bottomColor();
    return nodeFinder.evalSwings(root.vm.mainline);
  };

  var findNextNode = function() {
    var candidates = candidateNodes();
    candidates.forEach(function(n) {
      n.glyphs = [{
        name: 'hehe',
        symbol: '@'
      }];
    });
    var colorModulo = root.bottomColor() === 'white' ? 1 : 0;
    for (var i in candidates) {
      var node = candidates[i];
      if (node.ply % 2 === colorModulo && !isPlySolved(node.ply)) return node;
    }
  };

  var jumpToNext = function() {
    node = findNextNode();
    if (!node) return;
    root.jumpToMain(node.ply);
    setTimeout(function() {
      root.jumpToMain(node.ply - 1);
    }, 500);
  };

  var addNode = function(node, path) {
    console.log(node, path);
  };

  jumpToNext();

  return {
    node: node,
    color: color,
    isPlySolved: isPlySolved,
    addNode: addNode
  };
};
