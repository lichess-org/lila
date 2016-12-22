var winningChances = require('ceval').winningChances;

function hasCompChild(node) {
  return !!node.children.filter(function(c) {
    return c.comp;
  })[0];
}

module.exports = {

  nextGlyphSymbol: function(color, symbol, mainline, fromPly) {
    var len = mainline.length;
    if (!len) return;
    var fromIndex = fromPly - mainline[0].ply;
    for (var i = 1; i < len; i++) {
      var node = mainline[(fromIndex + i) % len];
      var found = (node.ply % 2 === (color === 'white' ? 1 : 0)) && node.glyphs && node.glyphs.filter(function(g) {
        return g.symbol === symbol;
      })[0];
      if (found) return node;
    }
  },

  evalSwings: function(mainline, nodeFilter) {
    var found = [];
    var node, prev;
    var threshold = 0.075;
    for (var i = 1; i < mainline.length; i++) {
      node = mainline[i];
      prev = mainline[i - 1];
      if (nodeFilter(node) && node.eval && prev.eval) {
        var diff = Math.abs(winningChances.povDiff('white', prev.eval, node.eval));
        if (diff > threshold && hasCompChild(prev)) found.push(node);
      }
    }
    return found;
  }
};
