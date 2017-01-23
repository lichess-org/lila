var m = require('mithril');
var raf = require('chessground').util.requestAnimationFrame;
var throttle = require('common').throttle;
var defined = require('common').defined;
var normalizeEval = require('chess').renderEval;
var treePath = require('tree').path;


var autoScrollNow = function(ctrl, el) {
  var cont = el.parentNode;
  raf(function() {
    var target = el.querySelector('.active');
    if (!target) {
      cont.scrollTop = ctrl.vm.path === treePath.root ? 0 : 99999;
      return;
    }
    cont.scrollTop = target.offsetTop - cont.offsetHeight / 2 + target.offsetHeight;
  });
};

var autoScroll = throttle(300, false, autoScrollNow);

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
  if (opts.isMainline) {
    var isWhite = main.ply % 2 === 1;
    if (!cs[1]) return [
      isWhite ? renderIndex(main.ply, false) : null,
      renderMoveAndChildrenOf(ctx, main, {
        parentPath: opts.parentPath,
        isMainline: true
      })
    ];
    var mainChildren = renderChildrenOf(ctx, main, {
      parentPath: opts.parentPath + main.id,
      isMainline: true
    });
    var passOpts = {
      parentPath: opts.parentPath,
      isMainline: true
    };
    return [
      isWhite ? renderIndex(main.ply, false) : null,
      renderMoveOf(ctx, main, passOpts),
      isWhite ? emptyMove() : null,
      m('interrupt', [
        renderLines(ctx, cs.slice(1), {
          parentPath: opts.parentPath,
          isMainline: true
        })
      ]),
      isWhite && mainChildren ? [
        renderIndex(main.ply, false),
        emptyMove()
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
      class: nodes[1] ? '' : 'single'
    },
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
  var classes = [];
  if (path === ctx.ctrl.vm.path) classes.push('active');
  if (path === ctx.ctrl.vm.initialPath) classes.push('current');
  else if (node.ply < ctx.ctrl.vm.initialNode.ply) classes.push('hist');
  if (node.puzzle) {
    classes.push(node.puzzle);
  }
  if (classes.length) attrs.class = classes.join(' ');
  return moveTag(attrs, renderMove(ctx, node));
}

function renderGlyph(glyph) {
  return {
    tag: 'glyph',
    attrs: {
      title: glyph.name
    },
    children: [glyph.symbol]
  };
}

function puzzleGlyph(ctx, node) {
  switch (node.puzzle) {
    case 'good':
    case 'win':
      return renderGlyph({
        name: ctx.ctrl.trans.noarg('bestMove'),
        symbol: '✓'
      });
    case 'fail':
      return renderGlyph({
        name: ctx.ctrl.trans.noarg('puzzleFailed'),
        symbol: '✗'
      });
    case 'retry':
      return renderGlyph({
        name: ctx.ctrl.trans.noarg('goodMove'),
        symbol: '?!'
      });
  }
}

function renderMove(ctx, node) {
  var eval = node.eval || node.ceval || {};
  return [
    node.san,
    defined(eval.cp) ? renderEval(normalizeEval(eval.cp)) : (
      defined(eval.mate) ? renderEval('#' + eval.mate) : null
    ),
    puzzleGlyph(ctx, node)
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
  if (node.puzzle) classes.push(node.puzzle);
  if (classes.length) attrs.class = classes.join(' ');
  return moveTag(attrs, [
    withIndex ? renderIndex(node.ply, true) : null,
    node.san,
    puzzleGlyph(ctx, node)
  ]);
}

function renderMoveAndChildrenOf(ctx, node, opts) {
  var path = opts.parentPath + node.id;
  return [
    renderMoveOf(ctx, node, opts),
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

function emptyMove() {
  return moveTag({
    class: 'empty'
  }, '...');
}

function renderEval(e) {
  return {
    tag: 'eval',
    children: [e]
  };
}

function eventPath(e, ctrl) {
  return e.target.getAttribute('p') || e.target.parentNode.getAttribute('p');
}

module.exports = {
  render: function(ctrl) {
    var root = ctrl.getTree().root;
    var ctx = {
      ctrl: ctrl,
      showComputer: false
    };
    return m('div.tview2', {
      config: function(el, isUpdate) {
        if (ctrl.vm.autoScrollNow) {
          autoScrollNow(ctrl, el);
          ctrl.vm.autoScrollNow = false;
          ctrl.vm.autoScrollRequested = false;
        }
        else if (ctrl.vm.autoScrollRequested || !isUpdate) {
          if (isUpdate || ctrl.vm.path !== treePath.root) autoScroll(ctrl, el);
          ctrl.vm.autoScrollRequested = false;
        }
        if (isUpdate) return;
        el.addEventListener('mousedown', function(e) {
          if (defined(e.button) && e.button !== 0) return; // only touch or left click
          var path = eventPath(e, ctrl);
          if (path) ctrl.userJump(path);
          m.redraw();
        });
      },
    }, [
      root.ply % 2 === 1 ? [
        renderIndex(root.ply, false),
        emptyMove()
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
