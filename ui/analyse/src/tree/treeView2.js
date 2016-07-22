var m = require('mithril');
var contextMenu = require('../contextMenu');
var raf = require('chessground').util.requestAnimationFrame;
var util = require('../util');

var autoScroll = util.throttle(300, false, function(el) {
  raf(function() {
    var target = el.querySelector('.active') || el.querySelector('move:first-child');
    if (!target) return;
    var cont = el.parentNode;
    var scroll = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight / 2;
    // console.log(scroll);
    // cont.scrollTop = scroll;
  });
});

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

function renderChildrenOf(ctx, node, opts) {
  var cs = node.children;
  if (!cs[0]) return;
  if (!cs[1]) return renderMoveOf(ctx, cs[0], opts);
  if (opts.isMainline) {
    return [
      renderLines(ctx, cs.slice(1), {
        parentPath: opts.parentPath,
        isMainline: false
      }),
      renderMoveOf(ctx, cs[0], {
        parentPath: opts.parentPath,
        isMainline: true,
        withIndex: true
      })
    ];
  }
  return renderLines(ctx, cs, opts);
}

function renderLines(ctx, nodes, opts) {
  return {
    tag: 'lines',
    children: nodes.map(function(n, i) {
      return lineTag(renderMoveOf(ctx, n, {
        parentPath: opts.parentPath,
        isMainline: opts.isMainline && i === 0,
        withIndex: true
      }));
    })
  };
}

function lineTag(content) {
  return {
    tag: 'line',
    children: content
  };
}

function renderMoveOf(ctx, node, opts) {
  var withIndex = opts.withIndex || node.ply % 2 === 1;
  var path = opts.parentPath + node.id;
  var attrs = {
    p: path
  };
  var classes = path === ctx.ctrl.vm.path ? ['active'] : [];
  if (path === ctx.ctrl.vm.contextMenuPath) classes.push('context_menu');
  if (path === ctx.ctrl.vm.initialPath && game.playable(ctx.ctrl.data)) classes.push('current');
  if (opts.isMainline) classes.push('main');
  if (ctx.conceal) classes.push(ctx.conceal);
  // if (!isMainline && (node.comments || node.shapes)) classes.push('annotated');
  if (classes.length) attrs.class = classes.join(' ');
  return [
    moveTag(attrs, [
      withIndex ? renderIndex(node.ply) : null,
      util.fixCrazySan(node.san),
      node.glyphs ? renderGlyphs(node.glyphs) : null
    ]),
    renderChildrenOf(ctx, node, {
      parentPath: path,
      isMainline: opts.isMainline
    })
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
    config: function(el, isUpdate) {
      if (ctrl.vm.autoScrollRequested || !isUpdate) {
        autoScroll(el);
        ctrl.vm.autoScrollRequested = false;
      }
    }
  }, renderChildrenOf(ctx, root, {
    parentPath: '',
    isMainline: true
  }));
};
