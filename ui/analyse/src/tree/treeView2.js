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

function renderChildrenOf(ctx, node, parentPath) {
  // if (i++ > 10000) {
  //   console.log('stoooooooooooooooooooooooop!');
  //   return;
  // }
  // console.log(node, 'renderChildrenOf');
  if (!node.children[0]) return;
  if (node.children[1]) return renderLines(ctx, node.children, parentPath);
  return renderMoveOf(ctx, node.children[0], parentPath);
}

function renderLines(ctx, nodes, parentPath) {
  return {
    tag: 'lines',
    children: nodes.map(function(n) {
      return lineTag(renderMoveOf(ctx, n, parentPath, true));
    })
  };
}

function lineTag(content) {
  return {
    tag: 'line',
    children: content
  };
}

function renderMoveOf(ctx, node, parentPath, withIndex) {
  withIndex = withIndex || node.ply % 2 === 1;
  var path = parentPath + node.id;
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
    moveTag(attrs, [
      withIndex ? renderIndex(node.ply) : null,
      util.fixCrazySan(node.san),
      node.glyphs ? renderGlyphs(node.glyphs) : null
    ]),
    renderChildrenOf(ctx, node, path)
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
      var path = e.target.getAttribute('p') || e.target.parentNode.getAttribute('p');
      if (path) ctrl.userJump(path);
    },
  }, renderChildrenOf(ctx, root, ''));
};
