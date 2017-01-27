var m = require('mithril');
var fixCrazySan = require('chess').fixCrazySan;
var normalizeEval = require('chess').renderEval;
var defined = require('common').defined;

function plyToTurn(ply) {
  return Math.floor((ply - 1) / 2) + 1;
}

function renderGlyphs(glyphs) {
  return glyphs.map(function(glyph) {
    return {
      tag: 'glyph',
      attrs: {
        title: glyph.name
      },
      children: [glyph.symbol]
    };
  });
}

function renderEval(e) {
  return {
    tag: 'eval',
    children: [e]
  };
}

function renderIndex(ply, withDots) {
  return {
    tag: 'index',
    children: [
      plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? '.' : '...') : '')
    ]
  };
}

function renderMove(ctx, node) {
  var eval = node.eval || node.ceval || {};
  return [
    fixCrazySan(node.san), (node.glyphs && ctx.showGlyphs) ? renderGlyphs(node.glyphs) : null,
    ctx.showEval ? (
      defined(eval.cp) ? renderEval(normalizeEval(eval.cp)) : (
        defined(eval.mate) ? renderEval('#' + eval.mate) : null
      )
    ) : null,
  ];
}

module.exports = {
  renderIndexAndMove: function(ctx, node) {
    return node.uci ? [
      renderIndex(node.ply, ctx.withDots),
      renderMove(ctx, node)
    ] : m('span.init', 'Initial position');
  },
  renderIndex: renderIndex,
  renderMove: renderMove,
  renderGlyphs: renderGlyphs
};
