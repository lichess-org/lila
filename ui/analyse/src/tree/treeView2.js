var m = require('mithril');
var contextMenu = require('../contextMenu');
var raf = require('chessground').util.requestAnimationFrame;
var util = require('../util');
var defined = util.defined;
var empty = util.empty;
var game = require('game').game;
var treePath = require('./path');

var autoScroll = util.throttle(300, false, function(el) {
  raf(function() {
    var target = el.querySelector('.active') || el.querySelector('index:first-child');
    if (!target) return;
    var cont = el.parentNode;
    cont.scrollTop = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight;
  });
});

function pathContains(ctx, path) {
  return treePath.contains(ctx.ctrl.vm.path, path);
}

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
  var conceal = opts.noConceal ? null : (opts.conceal || ctx.concealOf(true)(opts.parentPath + main.id, main));
  if (conceal === 'hide') return;
  if (opts.isMainline) {
    var isWhite = main.ply % 2 === 1;
    var commentTags = renderMainlineCommentsOf(ctx, main, {
      conceal: conceal,
      withColor: true
    });
    if (!cs[1] && empty(commentTags)) return [
      isWhite ? renderIndex(main.ply, false) : null,
      renderMoveAndChildrenOf(ctx, main, {
        parentPath: opts.parentPath,
        isMainline: true,
        conceal: conceal
      })
    ];
    var mainChildren = renderChildrenOf(ctx, main, {
      parentPath: opts.parentPath + main.id,
      isMainline: true,
      conceal: conceal
    });
    var passOpts = {
      parentPath: opts.parentPath,
      isMainline: true,
      conceal: conceal
    };
    return [
      isWhite ? renderIndex(main.ply, false) : null,
      renderMoveOf(ctx, main, passOpts),
      isWhite ? emptyMove(passOpts) : null,
      m('interrupt', [
        commentTags,
        renderLines(ctx, cs.slice(1), {
          parentPath: opts.parentPath,
          isMainline: true,
          conceal: conceal,
          noConceal: !conceal
        })
      ]),
      isWhite && mainChildren ? [
        renderIndex(main.ply, false),
        emptyMove(passOpts)
      ] : null,
      mainChildren
    ];
  }
  if (!cs[1]) return renderMoveAndChildrenOf(ctx, main, opts);
  return renderLines(ctx, cs, opts);
}

function renderLines(ctx, nodes, opts) {
  return {
    tag: 'lines',
    attrs: {
      class: (nodes[1] ? '' : 'single') // + (opts.conceal ? ' ' + opts.conceal : '')
    },
    children: nodes.map(function(n) {
      return lineTag(renderMoveAndChildrenOf(ctx, n, {
        parentPath: opts.parentPath,
        isMainline: false,
        withIndex: true,
        noConceal: opts.noConceal
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
  var eval = node.eval || node.ceval || {};
  var classes = [];
  if (path === ctx.ctrl.vm.path) classes.push('active');
  if (path === ctx.ctrl.vm.contextMenuPath) classes.push('context_menu');
  if (path === ctx.ctrl.vm.initialPath && game.playable(ctx.ctrl.data)) classes.push('current');
  if (opts.conceal) classes.push(opts.conceal);
  if (classes.length) attrs.class = classes.join(' ');
  return moveTag(attrs, [
    util.fixCrazySan(node.san),
    node.glyphs ? renderGlyphs(node.glyphs) : null,
    defined(eval.cp) ? renderEval(util.renderEval(eval.cp)) : (
      defined(eval.mate) ? renderEval('#' + eval.mate) : null
    ),
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
  else if (pathContains(ctx, path)) classes.push('parent');
  if (path === ctx.ctrl.vm.contextMenuPath) classes.push('context_menu');
  if (opts.conceal) classes.push(ctx.conceal);
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
    renderVariationCommentsOf(ctx, node),
    renderChildrenOf(ctx, node, {
      parentPath: opts.parentPath + node.id,
      isMainline: opts.isMainline,
      noConceal: opts.noConceal
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

function emptyMove(opts) {
  return moveTag({
    class: 'empty' + (opts.conceal ? ' ' + opts.conceal : '')
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

function renderMainlineCommentsOf(ctx, node, opts) {
  if (!ctx.ctrl.vm.comments || empty(node.comments)) return null;
  var colorClass = opts.withColor ? (node.ply % 2 === 0 ? 'black ' : 'white ') : '';
  var klass;
  return node.comments.map(function(comment) {
    if (comment.text.indexOf('Inaccuracy.') === 0) klass = 'inaccuracy';
    else if (comment.text.indexOf('Mistake.') === 0) klass = 'mistake';
    else if (comment.text.indexOf('Blunder.') === 0) klass = 'blunder';
    if (opts.conceal) klass += ' ' + opts.conceal;
    return renderMainlineComment(comment, colorClass, klass);
  });
}

function renderMainlineComment(comment, colorClass, commentClass) {
  return {
    tag: 'comment',
    attrs: {
      class: colorClass + commentClass
    },
    children: [truncateComment(comment.text, 400)]
  };
}

function renderVariationCommentsOf(ctx, node) {
  if (!ctx.ctrl.vm.comments || empty(node.comments)) return null;
  return node.comments.map(function(comment) {
    return renderVariationComment(comment);
  });
}

function renderVariationComment(comment) {
  return {
    tag: 'comment',
    children: [truncateComment(comment.text, 300)]
  };
}

function truncateComment(text, len) {
  if (text.length <= len) return text;
  return text.slice(0, len - 10) + ' [...]';
}

function eventPath(e, ctrl) {
  return e.target.getAttribute('p') || e.target.parentNode.getAttribute('p');
}

module.exports = function(ctrl, conceal) {
  var root = ctrl.tree.root;
  var ctx = {
    ctrl: ctrl,
    concealOf: function(isMainline) {
      return function(path, node) {
        if (!conceal || (isMainline && conceal.ply >= node.ply)) return null;
        if (treePath.contains(ctrl.vm.path, path)) return null;
        return conceal.owner ? 'conceal' : 'hide'
      };
    }
  };
  var commentTags = renderMainlineCommentsOf(ctx, root, {
    withColor: false,
    conceal: false
  });
  return m('div.tview2', {
    onmousedown: function(e) {
      if (e.button !== undefined && e.button !== 0) return; // only touch or left click
      var path = eventPath(e, ctrl);
      if (path) ctrl.userJump(path);
    },
    config: function(el, isUpdate) {
      if (ctrl.vm.autoScrollRequested || !isUpdate) {
        autoScroll(el);
        ctrl.vm.autoScrollRequested = false;
      }
    },
    oncontextmenu: function(e) {
      var path = eventPath(e, ctrl);
      contextMenu.open(e, {
        path: path,
        root: ctrl
      });
      return false;
    },
  }, [
    commentTags ? m('interrupt', commentTags) : null,
    root.ply % 2 === 1 ? [
      renderIndex(root.ply, false),
      emptyMove({})
    ] : null,
    renderChildrenOf(ctx, root, {
      parentPath: '',
      isMainline: true
    })
  ]);
};
