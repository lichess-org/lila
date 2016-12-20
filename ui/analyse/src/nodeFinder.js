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

  evalSwings: function(mainline) {
    var found = [];
    var node, prev;
    for (var i = 1; i < mainline.length - 1; i++) {
      node = mainline[i];
      prev = mainline[i - 1];
      if (node.eval && prev.eval) {
        var diff = Math.abs(winningChances.povDiff('white', mainline[i - 1].eval, mainline[i].eval));
        var threshold = 0.07;
        if (diff > threshold && hasCompChild(mainline[i - 1])) found.push(mainline[i]);
      }
    }
    return found;
  }
};
