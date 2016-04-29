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

function renderMove(ctrl, node, path, isMainline) {
  if (!node) return emptyMove;
  var eval = isMainline ? (node.eval || node.ceval || {}) : {};
  var attrs = isMainline ? {} : {
    'data-path': path
  };
  var classes = path === ctrl.vm.path ? ['active'] : [];
  if (path === ctrl.vm.contextMenuPath) classes.push('context_menu');
  if (path === ctrl.vm.initialPath) classes.push('current');
  if (classes.length) attrs.class = classes.join(' ');
  return {
    tag: 'move',
    attrs: attrs,
    children: [
      defined(eval.cp) ? renderEvalTag(util.renderEval(eval.cp)) : (
        defined(eval.mate) ? renderEvalTag('#' + eval.mate) : null
      ),
      node.san[0] === 'P' ? node.san.slice(1) : node.san
    ]
  };
}

function renderVariation(ctrl, node, parent, klass, depth) {
  var path = parent.path + node.id;
  var visiting = treePath.contains(ctrl.vm.path, path);
  return m('div', {
    class: klass + ' variation ' + (visiting ? ' visiting' : '')
  }, renderVariationContent(ctrl, node, parent.path, visiting));
}

function renderVariationNested(ctrl, node, path) {
  return m('span.variation', [
    '(',
    renderVariationContent(ctrl, node, path, treePath.contains(ctrl.vm.path, path)),
    ')'
  ]);
}

function renderVariationContent(ctrl, node, path, full) {
  var turns = [];
  var line = treeOps.mainlineNodeList(node);
  if (node.ply % 2 === 0) {
    line = line.slice(0);
    var first = line.shift();
    turns.push({
      turn: util.plyToTurn(first.ply),
      black: first
    });
  }
  var maxPlies = Math.min(full ? 999 : 6, line.length);
  for (i = 0; i < maxPlies; i += 2) turns.push({
    turn: util.plyToTurn(line[i].ply),
    white: line[i],
    black: line[i + 1]
  });
  var dom;
  return turns.map(function(turn) {
    dom = renderVariationTurn(ctrl, turn, path);
    path += (turn.white && turn.white.id || '') + (turn.black && turn.black.id || '');
    return dom;
  });
}

function renderVariationMeta(ctrl, node, path) {
  if (!node || empty(node.children[1])) return;
  return node.children.slice(1).map(function(child, i) {
    return renderVariationNested(ctrl, child, path);
  });
}

function renderVariationTurn(ctrl, turn, path) {
  var wPath = turn.white ? path + turn.white.id : null;
  var wMove = wPath ? renderMove(ctrl, turn.white, wPath) : null;
  var wMeta = renderVariationMeta(ctrl, turn.white, wPath);
  var bPath = turn.black ? (wPath || path) + turn.black.id : null;
  var bMove = bPath ? renderMove(ctrl, turn.black, bPath) : null;
  var bMeta = renderVariationMeta(ctrl, turn.black, bPath);
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

function renderMeta(ctrl, node, prev, path) {
  var opening = ctrl.data.game.opening;
  opening = (node && opening && opening.ply === node.ply) ? renderCommentOpening(ctrl, opening) : null;
  if (!node || (!opening && empty(node.comments) && !prev.children[1])) return;
  var dom = [];
  if (opening) dom.push(opening);
  var colorClass = node.ply % 2 === 0 ? 'black ' : 'white ';
  var commentClass;
  if (ctrl.vm.comments && !empty(node.comments)) node.comments.forEach(function(comment) {
    if (comment.text.indexOf('Inaccuracy.') === 0) commentClass = 'inaccuracy';
    else if (comment.text.indexOf('Mistake.') === 0) commentClass = 'mistake';
    else if (comment.text.indexOf('Blunder.') === 0) commentClass = 'blunder';
    dom.push(m('div', {
      class: 'comment ' + colorClass + commentClass
    }, truncateComment(comment.text)));
  });
  if (prev.children[1]) prev.children.slice(1).forEach(function(child, i) {
    if (i === 0 && !empty(node.comments) && !ctrl.vm.comments) return;
    dom.push(renderVariation(
      ctrl,
      child, {
        node: prev,
        path: treePath.init(path)
      },
      i === 0 ? colorClass + commentClass : null,
      1
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

function renderMainlineTurn(ctrl, turn, path) {
  var index = renderIndex(turn.turn);
  var wPath, wMove, wMeta, bPath, bMove, bMeta, dom;
  if (turn.white) {
    wPath = path = path + turn.white.node.id;
    wMove = renderMove(ctrl, turn.white.node, wPath, 1);
    wMeta = renderMeta(ctrl, turn.white.node, turn.white.prev, wPath);
  }
  if (turn.black) {
    bPath = path = path + turn.black.node.id;
    bMove = renderMove(ctrl, turn.black.node, bPath, 1);
    bMeta = renderMeta(ctrl, turn.black.node, turn.black.prev, bPath);
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
  renderMainline: function(ctrl, mainline) {
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

    for (var i = 0, len = turns.length; i < len; i++) {
      res = renderMainlineTurn(ctrl, turns[i], path);
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
