var m = require('mithril');
var contextMenu = require('../contextMenu');
var util = require('../util');

function plyToTurn(ply) {
  return Math.floor((ply - 1) / 2) + 1;
}

function renderIndex(ply) {
  return {
    tag: 'index',
    children: [
      plyToTurn(ply) + (ply % 2 === 1 ? '.' : '...')
    ]
  };
}

function renderChildrenOf(node) {
  var main = node.children[0];
  if (!main) return;
  var variations = node.children.slice(1);
  return [
    renderMoveOf(main),
    renderChildrenOf(main)
  ];
}

function renderMoveOf(node) {
  return [
    renderIndex(node.ply),
    moveTag([
      util.fixCrazySan(node.san),
      node.glyphs ? renderGlyphs(node.glyphs) : null
    ])
  ];
}

function moveTag(children) {
  return {
    tag: 'move',
    children: children
  };
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

module.exports = function(ctrl, conceal) {
  var root = ctrl.tree.root;
  return m('div.tview2', renderChildrenOf(root));
};
