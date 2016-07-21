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

function renderVariations(ctx, nodes, parentPath) {
  return {
    tag: 'wrap',
    children: nodes.map(function(n) {
      return [
        renderMoveOf(ctx, n, parentPath),
        renderChildrenOf(ctx, n, parentPath)
      ];
    })
  };
}

function renderChildrenOf(ctx, node, parentPath) {
  var main = node.children[0];
  if (!main) return;
  var path = parentPath + main.id;
  var variations = node.children.slice(1);
  return [
    renderMoveOf(ctx, main, path),
    variations[0] ? renderVariations(ctx, variations, path) : null
  ];
}

function renderMoveOf(ctx, node, path) {
  var attrs = {
    p: path
  };
  var classes = path === ctx.ctrl.vm.path ? ['active'] : [];
  if (path === ctx.ctrl.vm.contextMenuPath) classes.push('context_menu');
  if (path === ctx.ctrl.vm.initialPath && game.playable(ctx.ctrl.data)) classes.push('current');
  if (ctx.conceal) classes.push(ctx.conceal);
  // if (!isMainline && (node.comments || node.shapes)) classes.push('annotated');
  if (classes.length) attrs.class = classes.join(' ');
  return [
    renderIndex(node.ply),
    moveTag(attrs, [
      util.fixCrazySan(node.san),
      node.glyphs ? renderGlyphs(node.glyphs) : null
    ])
  ];
}

function moveTag(attrs, content) {
  return {
    tag: 'move',
    attrs: attrs,
    children: content
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
  var ctx = {
    ctrl: ctrl,
    conceal: conceal
  };
  return m('div.tview2', {
    onmousedown: function(e) {
      if (e.button !== undefined && e.button !== 0) return; // only touch or left click
      var path = e.target.getAttribute('p');
      if (path) ctrl.userJump(path);
    },
  }, renderChildrenOf(ctx, root, ''));
};
