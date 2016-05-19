var m = require('mithril');
var chessground = require('chessground');
var classSet = chessground.util.classSet;
var partial = chessground.util.partial;
var util = require('../util');
var defined = util.defined;
var empty = util.empty;
var treePath = require('./path');
var treeOps = require('./ops');

function renderEvalTag(e) {
  return {
    tag: 'eval',
    children: [e]
  };
}

var emptyMove = m('move.empty', '...');
var nullMove = m('move.empty', '');

function renderMove(ctrl, node, path, isMainline, conceal) {
  if (!node) return emptyMove;
  var eval = isMainline ? (node.eval || node.ceval || {}) : {};
  var attrs = isMainline ? {} : {
    'data-path': path
  };
  var classes = path === ctrl.vm.path ? ['active'] : [];
  if (path === ctrl.vm.contextMenuPath) classes.push('context_menu');
  if (path === ctrl.vm.initialPath) classes.push('current');
  if (conceal) classes.push(conceal);
  if (classes.length) attrs.class = classes.join(' ');
  return {
    tag: 'move',
    attrs: attrs,
    children: [
      defined(eval.cp) ? renderEvalTag(util.renderEval(eval.cp)) : (
        defined(eval.mate) ? renderEvalTag('#' + eval.mate) : null
      ),
      node.san[0] === 'P' ? node.san.slice(1) : node.san,
      node.glyphs ? renderGlyphs(node.glyphs) : null
    ]
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

function renderVariation(ctrl, node, parent, klass, depth, concealOf) {
  var path = parent.path + node.id;
  var visiting = treePath.contains(ctrl.vm.path, path);
  return m('div', {
    class: klass + ' variation ' + (visiting ? ' visiting' : '')
  }, renderVariationContent(ctrl, node, parent.path, visiting, concealOf));
}

function renderVariationNested(ctrl, node, path, concealOf) {
  return m('span.variation', [
    '(',
    renderVariationContent(ctrl, node, path, treePath.contains(ctrl.vm.path, path, false), concealOf),
    ')'
  ]);
}

function renderVariationContent(ctrl, node, path, full, concealOf) {

  var turns = [];
  var mainline = treeOps.mainlineNodeList(node);
  var initPly = node.ply;
  var makeTurnColor = function(i) {
    if (mainline[i]) return {
      node: mainline[i],
      prev: mainline[i - 1]
    };
  }
  var maxPlies = Math.min(full ? 999 : 6, mainline.length);
  if (initPly % 2 === 1)
    for (var i = 0; i < maxPlies; i += 2) turns.push({
      turn: Math.floor((initPly + i) / 2) + 1,
      white: makeTurnColor(i),
      black: makeTurnColor(i + 1)
    });
  else {
    turns.push({
      turn: Math.floor(initPly / 2),
      white: null,
      black: makeTurnColor(0)
    });
    for (var i = 1; i < maxPlies; i += 2) turns.push({
      turn: Math.floor((initPly + i) / 2) + 1,
      white: makeTurnColor(i),
      black: makeTurnColor(i + 1)
    });
  }
  var dom;
  return turns.map(function(turn) {
    dom = renderVariationTurn(ctrl, turn, path, concealOf);
    path += (turn.white && turn.white.node.id || '') + (turn.black && turn.black.node.id || '');
    return dom;
  });
}

function renderVariationMeta(ctrl, node, prev, path, concealOf) {
  if (!prev || empty(prev.children[1])) return;
  return prev.children.slice(1).map(function(child, i) {
    return renderVariationNested(ctrl, child, treePath.init(path), concealOf);
  });
}

function renderVariationTurn(ctrl, turn, path, concealOf) {
  var wPath, wMove, wMeta, bPath, bMove, bMeta, dom;
  if (turn.white) {
    wPath = path = path + turn.white.node.id;
    wMove = renderMove(ctrl, turn.white.node, wPath, false, concealOf(wPath, turn.white.node));
    wMeta = renderVariationMeta(ctrl, turn.white.node, turn.white.prev, wPath, concealOf);
  }
  if (turn.black) {
    bPath = path = path + turn.black.node.id;
    bMove = renderMove(ctrl, turn.black.node, bPath, false, concealOf(bPath, turn.black.node));
    bMeta = renderVariationMeta(ctrl, turn.black.node, turn.black.prev, bPath, concealOf);
  }

  if (wMove) {
    if (wMeta) return [
      renderIndex(turn.turn + '.'),
      wMove,
      wMeta,
      bMove ? [
        bMove,
        bMeta
      ] : null
    ];
    return [renderIndex(turn.turn + '.'), wMove, (bMove ? [' ', bMove, bMeta] : '')];
  }
  return [renderIndex(turn.turn + '...'), bMove, bMeta];
}

function renderCommentOpening(ctrl, opening) {
  return m('div.comment.opening', opening.eco + ' ' + opening.name);
}

function truncateComment(text) {
  if (text.length <= 140) return text;
  return text.slice(0, 125) + ' [...]';
}

function renderComment(comment, colorClass, commentClass) {
  return m('div', {
    class: 'comment ' + colorClass + commentClass
  }, truncateComment(comment.text));
}

function returnNull() {
  return null;
}

function renderMeta(ctrl, node, prev, path, concealOf) {
  var opening = ctrl.data.game.opening;
  opening = (node && opening && opening.ply === node.ply) ? renderCommentOpening(ctrl, opening) : null;
  if (!node || (!opening && empty(node.comments) && !prev.children[1])) return;
  var dom = [];
  if (opening) dom.push(opening);
  var colorClass = node.ply % 2 === 0 ? 'black ' : 'white ';
  var commentClass;
  var commentConceal = concealOf(path, node);
  if (ctrl.vm.comments && !empty(node.comments) && commentConceal !== 'hide')
    node.comments.forEach(function(comment) {
      if (comment.text.indexOf('Inaccuracy.') === 0) commentClass = 'inaccuracy';
      else if (comment.text.indexOf('Mistake.') === 0) commentClass = 'mistake';
      else if (comment.text.indexOf('Blunder.') === 0) commentClass = 'blunder';
      if (commentConceal) commentClass += ' ' + commentConceal;
      dom.push(renderComment(comment, colorClass, commentClass));
    });
  if (prev.children[1]) prev.children.slice(1).forEach(function(child, i) {
    // hide computer line as a comment
    if (i === 0 && !empty(node.comments) && !ctrl.vm.comments) return;
    var basePath = treePath.init(path);
    if (commentConceal !== 'hide' || treePath.contains(ctrl.vm.path, basePath + child.id))
      dom.push(renderVariation(
        ctrl,
        child, {
          node: prev,
          path: basePath
        },
        i === 0 ? colorClass + commentClass : null,
        1,
        commentConceal === null ? returnNull : concealOf
      ));
  });
  if (dom.length) return m('div', {
    class: 'meta'
  }, dom);
}

function renderIndex(txt) {
  return {
    tag: 'index',
    children: [txt]
  };
}

function renderMainlineTurnEl(children) {
  return {
    tag: 'turn',
    children: children
  };
}

function concealAction(ctrl, conceal) {
  return function(isMainline) {
    return function(path, node) {
      if (!conceal || (isMainline && conceal.ply >= node.ply)) return null;
      if (treePath.contains(ctrl.vm.path, path)) return null;
      return conceal.owner ? 'conceal' : 'hide'
    };
  };
}

function renderMainlineTurn(ctrl, turn, path, conceal) {
  var index = renderIndex(turn.turn);
  var wPath, wMove, wMeta, bPath, bMove, bMeta, con, dom;
  var concealOf = concealAction(ctrl, conceal);
  if (turn.white) {
    wPath = path = path + turn.white.node.id;
    con = concealOf(true)(wPath, turn.white.node);
    wMove = renderMove(ctrl, turn.white.node, wPath, true, con);
    wMeta = renderMeta(ctrl, turn.white.node, turn.white.prev, wPath, concealOf(false));
  }
  if (turn.black) {
    bPath = path = path + turn.black.node.id;
    con = concealOf(true)(bPath, turn.black.node);
    bMove = renderMove(ctrl, turn.black.node, bPath, true, con);
    bMeta = renderMeta(ctrl, turn.black.node, turn.black.prev, bPath, concealOf(false));
  }
  if (wMove) {
    if (wMeta) dom = [
      renderMainlineTurnEl([index, wMove, emptyMove]),
      wMeta,
      bMove ? [
        renderMainlineTurnEl([index, emptyMove, bMove]),
        bMeta
      ] : null,
    ];
    else dom = [
      renderMainlineTurnEl([index, wMove, bMove]),
      bMeta
    ];
  } else dom = [
    renderMainlineTurnEl([index, emptyMove, bMove]),
    bMeta
  ];
  return {
    dom: dom,
    path: path
  };
}

module.exports = {
  renderMainline: function(ctrl, mainline, conceal) {
    var turns = [];
    var initPly = mainline[0].ply;
    var makeTurnColor = function(i) {
      if (mainline[i]) return {
        node: mainline[i],
        prev: mainline[i - 1]
      };
    }
    if (initPly % 2 === 0)
      for (var i = 1, nb = mainline.length; i < nb; i += 2) turns.push({
        turn: Math.floor((initPly + i) / 2) + 1,
        white: makeTurnColor(i),
        black: makeTurnColor(i + 1)
      });
    else {
      turns.push({
        turn: Math.floor(initPly / 2) + 1,
        white: null,
        black: makeTurnColor(1)
      });
      for (var i = 2, nb = mainline.length; i < nb; i += 2) turns.push({
        turn: Math.floor((initPly + i) / 2) + 1,
        white: makeTurnColor(i),
        black: makeTurnColor(i + 1)
      });
    }

    var tags = [],
      path = treePath.root;

    var initialComments = mainline[0].comments || [];
    if (initialComments.length) tags.push(m('div.meta',
      initialComments.map(function(comment) {
        return renderComment(comment, '', 'undefined');
      })));

    for (var i = 0, len = turns.length; i < len; i++) {
      res = renderMainlineTurn(ctrl, turns[i], path, conceal);
      path = res.path;
      tags.push(res.dom);
    }

    return tags;
  },
  eventPath: function(e, ctrl) {
    var el = e.target.tagName === 'MOVE' ? e.target : e.target.parentNode;
    if (el.tagName !== 'MOVE' || el.classList.contains('empty')) return;
    var path = el.getAttribute('data-path');
    if (path) return path;
    var ply = 2 * parseInt($(el).siblings('index').text()) - 2 + $(el).index();
    if (ply) return ctrl.mainlinePathToPly(ply);
  }
};
