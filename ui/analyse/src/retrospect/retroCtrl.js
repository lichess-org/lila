var nodeFinder = require('../nodeFinder');

module.exports = function(root) {

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
      if (node.ply % 2 === colorModulo && !$.fp.contains(solvedPlies, node.ply)) return node;
    }
  };

  var jumpToNext = function() {
  };

  var color = root.bottomColor();
  var solvedPlies = [];
  var node = findNextNode();

  return {
    node: node,
    color: color
  };
};
