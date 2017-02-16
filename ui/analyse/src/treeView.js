var m = require('mithril');
var contextMenu = require('./contextMenu');
var raf = require('chessground').util.requestAnimationFrame;
var empty = require('common').empty;
var throttle = require('common').throttle;
var defined = require('common').defined;
var game = require('game').game;
var fixCrazySan = require('chess').fixCrazySan;
var treePath = require('tree').path;
var treeOps = require('tree').ops;
var moveView = require('./moveView');
var commentAuthorText = require('./study/studyComments').authorText;

var autoScroll = throttle(300, false, function(ctrl, el) {
  raf(function() {
    var cont = el.parentNode;
    if (!cont) return;
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
      isWhite ? moveView.renderIndex(main.ply, false) : null,
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
      isWhite ? moveView.renderIndex(main.ply, false) : null,
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
        moveView.renderIndex(main.ply, false),
        emptyMove(passOpts)
      ] : null,
      mainChildren
    ];
  }
  if (!cs[1]) return renderMoveAndChildrenOf(ctx, main, opts);
  return renderInlined(ctx, cs, opts) || renderLines(ctx, cs, opts);
}

function renderInlined(ctx, nodes, opts) {
  if (!nodes[1] || nodes[2]) return;
  var found;
  if (!treeOps.hasBranching(nodes[1], 4)) found = [0, 1];
  else if (!treeOps.hasBranching(nodes[0], 4)) found = [1, 0];
  if (found) return renderMoveAndChildrenOf(ctx, nodes[found[0]], {
    parentPath: opts.parentPath,
    isMainline: false,
    noConceal: opts.noConceal,
    inline: nodes[found[1]]
  });
}

function renderLines(ctx, nodes, opts) {
  return {
    tag: 'lines',
    attrs: {
      class: (nodes[1] ? '' : 'single') // + (opts.conceal ? ' ' + opts.conceal : '')
    },
    children: nodes.map(function(n) {
      if (n.comp && ctx.ctrl.retro && ctx.ctrl.retro.hideComputerLine(n, opts.parentPath))
        return lineTag('Learn from this mistake');
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
  if (path.length > ctx.ctrl.vm.gamePath.length && !ctx.ctrl.vm.synthetic && !ctx.ctrl.vm.ongoing) classes.push('nongame');
  else if (ctx.ctrl.retro && ctx.ctrl.retro.current() && ctx.ctrl.retro.current().prev.path === path) classes.push('current');
  if (opts.conceal) classes.push(opts.conceal);
  if (classes.length) attrs.class = classes.join(' ');
  return moveTag(attrs, moveView.renderMove(ctx, node));
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
    withIndex ? moveView.renderIndex(node.ply, true) : null,
    fixCrazySan(node.san),
    node.glyphs ? moveView.renderGlyphs(node.glyphs) : null
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
    opts.inline ? renderInline(ctx, opts.inline, opts) : null,
    renderChildrenOf(ctx, node, {
      parentPath: path,
      isMainline: opts.isMainline,
      noConceal: opts.noConceal,
      truncate: opts.truncate ? opts.truncate - 1 : null
    })
  ];
}

function renderInline(ctx, node, opts) {
  return m('inline', [
    renderMoveAndChildrenOf(ctx, node, {
      withIndex: true,
      parentPath: opts.parentPath,
      isMainline: false,
      noConceal: opts.noConceal,
      truncate: opts.truncate
    })
  ]);
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
    return renderMainlineComment(comment, colorClass + klass, node.comments.length > 1, ctx);
  });
}

function renderMainlineComment(comment, klass, withAuthor, ctx) {
  return {
    tag: 'comment',
    attrs: {
      class: klass
    },
    children: [
      withAuthor ? m('span.by', commentAuthorText(comment.by)) : null,
      truncateComment(comment.text, 400, ctx)
    ]
  };
}

function renderVariationCommentsOf(ctx, node) {
  if (!ctx.ctrl.vm.comments || empty(node.comments)) return [];
  return node.comments.map(function(comment) {
    if (comment.by === 'lichess' && !ctx.showComputer) return;
    return renderVariationComment(comment, node.comments.length > 1, ctx);
  });
}

function renderVariationComment(comment, withAuthor, ctx) {
  return {
    tag: 'comment',
    children: [
      withAuthor ? m('span.by', commentAuthorText(comment.by)) : null,
      truncateComment(comment.text, 300, ctx)
    ]
  };
}

function truncateComment(text, len, ctx) {
  if (ctx.ctrl.embed) return text;
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
      showComputer: ctrl.vm.showComputer() && !ctrl.retro,
      showGlyphs: !!ctrl.study || ctrl.vm.showComputer(),
      showEval: !!ctrl.study || ctrl.vm.showComputer()
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
          if (path !== null) contextMenu.open(e, {
            path: path,
            root: ctrl
          });
          m.redraw();
          return false;
        };
        el.addEventListener('mousedown', function(e) {
          if (defined(e.button) && e.button !== 0) return; // only touch or left click
          var path = eventPath(e, ctrl);
          if (path) ctrl.userJump(path);
          m.redraw();
        });
      },
    }, [
      empty(commentTags) ? null : m('interrupt', commentTags),
      root.ply % 2 === 1 ? [
        moveView.renderIndex(root.ply, false),
        emptyMove({})
      ] : null,
      renderChildrenOf(ctx, root, {
        parentPath: '',
        isMainline: true
      })
    ]);
  }
};
