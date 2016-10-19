var m = require('mithril');
var contextMenu = require('../contextMenu');
var raf = require('chessground').util.requestAnimationFrame;
var util = require('../util');
var empty = util.empty;
var game = require('game').game;
var treePath = require('./path');
var commentAuthorText = require('../study/studyComments').authorText;

var autoScroll = util.throttle(300, false, function(ctrl, el) {
  var cont = el.parentNode;
  raf(function() {
    var target = el.querySelector('.active');
    if (!target) {
      cont.scrollTop = ctrl.vm.path === treePath.root ? 0 : 99999;
      return;
    }
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

function noCompNode(node) {
  return !node.comp;
}
function nonEmpty(x) {
  return !!x;
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
    }).filter(nonEmpty);
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
        noConceal: opts.noConceal,
        truncate: n.comp && !pathContains(ctx, opts.parentPath + n.id) ? 3 : null
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
  var classes = [];
  if (path === ctx.ctrl.vm.path) classes.push('active');
  if (path === ctx.ctrl.vm.contextMenuPath) classes.push('context_menu');
  if (path === ctx.ctrl.vm.initialPath && game.playable(ctx.ctrl.data)) classes.push('current');
  if (opts.conceal) classes.push(opts.conceal);
  if (classes.length) attrs.class = classes.join(' ');
  return moveTag(attrs, renderMove(node));
}

function renderMove(node) {
  var eval = node.eval || node.ceval || {};
  return [
    util.fixCrazySan(node.san),
    node.glyphs ? renderGlyphs(node.glyphs) : null,
    util.defined(eval.cp) ? renderEval(util.renderEval(eval.cp)) : (
      util.defined(eval.mate) ? renderEval('#' + eval.mate) : null
    ),
  ];
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
  var path = opts.parentPath + node.id;
  if (opts.truncate === 0) return moveTag({
    p: path
  }, [m('index', '[...]')]);
  return [
    renderMoveOf(ctx, node, opts),
    renderVariationCommentsOf(ctx, node),
    renderChildrenOf(ctx, node, {
      parentPath: path,
      isMainline: opts.isMainline,
      noConceal: opts.noConceal,
      truncate: opts.truncate ? opts.truncate - 1 : null
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
  if (!ctx.ctrl.vm.comments || empty(node.comments)) return [];
  var colorClass = opts.withColor ? (node.ply % 2 === 0 ? 'black ' : 'white ') : '';
  return node.comments.map(function(comment) {
    if (comment.by === 'lichess' && !ctx.showComputer) return;
    var klass = '';
    if (comment.text.indexOf('Inaccuracy.') === 0) klass = 'inaccuracy';
    else if (comment.text.indexOf('Mistake.') === 0) klass = 'mistake';
    else if (comment.text.indexOf('Blunder.') === 0) klass = 'blunder';
    if (opts.conceal) klass += ' ' + opts.conceal;
    return renderMainlineComment(comment, colorClass + klass, node.comments.length > 1);
  });
}

function renderMainlineComment(comment, klass, withAuthor) {
  return {
    tag: 'comment',
    attrs: {
      class: klass
    },
    children: [
      withAuthor ? m('span.by', commentAuthorText(comment.by)) : null,
      truncateComment(comment.text, 400)
    ]
  };
}

function renderVariationCommentsOf(ctx, node) {
  if (!ctx.ctrl.vm.comments || empty(node.comments)) return [];
  return node.comments.map(function(comment) {
    if (comment.by === 'lichess' && !ctx.showComputer) return;
    return renderVariationComment(comment, node.comments.length > 1);
  });
}

function renderVariationComment(comment, withAuthor) {
  return {
    tag: 'comment',
    children: [
      withAuthor ? m('span.by', commentAuthorText(comment.by)) : null,
      truncateComment(comment.text, 300)
    ]
  };
}

function truncateComment(text, len) {
  if (text.length <= len) return text;
  return text.slice(0, len - 10) + ' [...]';
}

function eventPath(e, ctrl) {
  return e.target.getAttribute('p') || e.target.parentNode.getAttribute('p');
}

var noop = function() {};

function emptyConcealOf() {
  return noop;
}

module.exports = {
  render: function(ctrl, concealOf) {
    var root = ctrl.tree.root;
    var ctx = {
      ctrl: ctrl,
      concealOf: concealOf || emptyConcealOf,
      showComputer: ctrl.vm.showComputer()
    };
    var commentTags = renderMainlineCommentsOf(ctx, root, {
      withColor: false,
      conceal: false
    });
    return m('div.tview2', {
      config: function(el, isUpdate) {
        if (ctrl.vm.autoScrollRequested || !isUpdate) {
          if (isUpdate || ctrl.vm.path !== treePath.root) autoScroll(ctrl, el);
          ctrl.vm.autoScrollRequested = false;
        }
        if (isUpdate) return;
        el.oncontextmenu = function(e) {
          var path = eventPath(e, ctrl);
          contextMenu.open(e, {
            path: path,
            root: ctrl
          });
          m.redraw();
          return false;
        };
        el.addEventListener('mousedown', function(e) {
          if (e.button !== undefined && e.button !== 0) return; // only touch or left click
          var path = eventPath(e, ctrl);
          if (path) ctrl.userJump(path);
          m.redraw();
        });
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
  },
  renderIndex: renderIndex,
  renderMove: renderMove
};
