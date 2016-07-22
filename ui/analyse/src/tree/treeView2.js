var m = require('mithril');
var contextMenu = require('../contextMenu');
var raf = require('chessground').util.requestAnimationFrame;
var util = require('../util');
var defined = util.defined;
var game = require('game').game;

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

function renderIndex(ply, withDots) {
  return {
    tag: 'index',
    children: [
      plyToTurn(ply) + (withDots ? (ply % 2 === 1 ? '.' : '...') : '')
    ]
  };
}

function renderChildrenOf(ctx, node, opts) {
  var cs = node.children;
  var main = cs[0];
  if (!main) return;
  if (opts.isMainline) {
    var isWhite = main.ply % 2 === 1;
    if (!cs[1]) return [
      isWhite ? renderIndex(main.ply, false) : null,
      renderMoveAndChildrenOf(ctx, main, opts)
    ];
    return [
      isWhite ? renderIndex(main.ply, false) : null,
      renderMoveOf(ctx, main, {
        parentPath: opts.parentPath,
        isMainline: true
      }),
      isWhite ? emptyMove() : null,
      m('interrupt', renderLines(ctx, cs.slice(1), {
        parentPath: opts.parentPath
      })),
      isWhite ? [
        renderIndex(main.ply, false),
        emptyMove()
      ] : null,
      renderChildrenOf(ctx, main, {
        parentPath: opts.parentPath + main.id,
        isMainline: true
      })
    ];
  }
  if (!cs[1]) return renderMoveAndChildrenOf(ctx, main, opts);
  return renderLines(ctx, cs, opts);
}

function renderLines(ctx, nodes, opts) {
  return {
    tag: 'lines',
    children: nodes.map(function(n) {
      return lineTag(renderMoveAndChildrenOf(ctx, n, {
        parentPath: opts.parentPath,
        isMainline: false,
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
  return opts.isMainline ? renderMainlineMoveOf(ctx, node, opts) : renderVariationMoveOf(ctx, node, opts);
}

function renderMainlineMoveOf(ctx, node, opts) {
  var path = opts.parentPath + node.id;
  var attrs = {
    p: path
  };
  var eval = opts.isMainline ? (node.eval || node.ceval || {}) : {};
  var classes = [];
  if (path === ctx.ctrl.vm.path) classes.push('active');
  if (path === ctx.ctrl.vm.contextMenuPath) classes.push('context_menu');
  if (path === ctx.ctrl.vm.initialPath && game.playable(ctx.ctrl.data)) classes.push('current');
  if (ctx.conceal) classes.push(ctx.conceal);
  if (classes.length) attrs.class = classes.join(' ');
  return moveTag(attrs, [
    util.fixCrazySan(node.san),
    node.glyphs ? renderGlyphs(node.glyphs) : null,
    defined(eval.cp) ? renderEval(util.renderEval(eval.cp)) : (
      defined(eval.mate) ? renderEval('#' + eval.mate) : null
    )
  ]);
}

function renderVariationMoveOf(ctx, node, opts) {
  var withIndex = opts.withIndex || node.ply % 2 === 1;
  var path = opts.parentPath + node.id;
  var attrs = {
    p: path
  };
  var classes = [];
  if (path === ctx.ctrl.vm.path) classes.push('active');
  if (path === ctx.ctrl.vm.contextMenuPath) classes.push('context_menu');
  if (ctx.conceal) classes.push(ctx.conceal);
  // if (!isMainline && (node.comments || node.shapes)) classes.push('annotated');
  if (classes.length) attrs.class = classes.join(' ');
  return moveTag(attrs, [
    withIndex ? renderIndex(node.ply, true) : null,
    util.fixCrazySan(node.san),
    node.glyphs ? renderGlyphs(node.glyphs) : null
  ]);
}

function renderMoveAndChildrenOf(ctx, node, opts) {
  return [
    renderMoveOf(ctx, node, opts),
    renderChildrenOf(ctx, node, {
      parentPath: opts.parentPath + node.id,
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

function emptyMove() {
  return moveTag({
    class: 'empty'
  }, '...');
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
