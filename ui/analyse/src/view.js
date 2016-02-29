var m = require('mithril');
var chessground = require('chessground');
var classSet = chessground.util.classSet;
var partial = chessground.util.partial;
var util = require('./util');
var defined = util.defined;
var empty = util.empty;
var game = require('game').game;
var renderStatus = require('game').view.status;
var mod = require('game').view.mod;
var router = require('game').router;
var treePath = require('./path');
var control = require('./control');
var actionMenu = require('./actionMenu').view;
var renderPromotion = require('./promotion').view;
var pgnExport = require('./pgnExport');
var forecastView = require('./forecast/forecastView');
var cevalView = require('./ceval/cevalView');
var crazyView = require('./crazy/crazyView');
var explorerView = require('./explorer/explorerView');
var raf = require('chessground').util.requestAnimationFrame;

function renderEvalTag(e) {
  return {
    tag: 'eval',
    children: [e]
  };
}

function autoScroll(el) {
  return util.throttle(300, false, function() {
    raf(function() {
      var plyEl = el.querySelector('.active') || el.querySelector('turn:first-child');
      if (plyEl) el.scrollTop = plyEl.offsetTop - el.offsetHeight / 2 + plyEl.offsetHeight / 2;
    });
  });
}

var emptyMove = m('move.empty', '...');
var nullMove = m('move.empty', '');

function renderMove(ctrl, move, path) {
  if (!move) return emptyMove;
  var pathStr = treePath.write(path);
  var eval = path[1] ? {} : (move.eval || move.ceval || {});
  var attrs = path[1] ? {
    'data-path': pathStr
  } : {};
  var classes = pathStr === ctrl.vm.pathStr ? ['active'] : [];
  if (pathStr === ctrl.vm.initialPathStr) classes.push('current');
  if (classes.length) attrs.class = classes.join(' ');
  return {
    tag: 'move',
    attrs: attrs,
    children: [
      defined(eval.cp) ? renderEvalTag(util.renderEval(eval.cp)) : (
        defined(eval.mate) ? renderEvalTag('#' + eval.mate) : null
      ),
      move.san[0] === 'P' ? move.san.slice(1) : move.san
    ]
  };
}

function plyToTurn(ply) {
  return Math.floor((ply - 1) / 2) + 1;
}

function renderVariation(ctrl, variation, path, klass) {
  var showMenu = ctrl.vm.variationMenu && ctrl.vm.variationMenu === treePath.write(path.slice(0, 1));
  var visiting = treePath.contains(path, ctrl.vm.path);
  return m('div', {
    class: klass + ' variation ' + (showMenu ? ' menu' : '') + (visiting ? ' visiting' : '')
  }, [
    m('span', {
      class: 'menu',
      'data-icon': showMenu ? 'L' : '',
      onclick: partial(ctrl.toggleVariationMenu, path)
    }),
    showMenu ? (function() {
      var promotable = util.synthetic(ctrl.data) ||
        !ctrl.analyse.getStepAtPly(path[0].ply).fixed;
      return [
        m('a', {
          class: 'delete text',
          'data-icon': 'q',
          onclick: partial(ctrl.deleteVariation, path)
        }, 'Delete variation'),
        promotable ? m('a', {
          class: 'promote text',
          'data-icon': 'E',
          onclick: partial(ctrl.promoteVariation, path)
        }, 'Promote to main line') : null
      ];
    })() :
    renderVariationContent(ctrl, variation, path, visiting)
  ]);
}

function renderVariationNested(ctrl, variation, path) {
  return m('span.variation', [
    '(',
    renderVariationContent(ctrl, variation, path, treePath.contains(path, ctrl.vm.path)),
    ')'
  ]);
}

function renderVariationContent(ctrl, variation, path, full) {
  var turns = [];
  if (variation[0].ply % 2 === 0) {
    variation = variation.slice(0);
    var move = variation.shift();
    turns.push({
      turn: plyToTurn(move.ply),
      black: move
    });
  }
  var maxPlies = Math.min(full ? 999 : (path[2] ? 2 : 4), variation.length);
  for (i = 0; i < maxPlies; i += 2) turns.push({
    turn: plyToTurn(variation[i].ply),
    white: variation[i],
    black: variation[i + 1]
  });
  return turns.map(function(turn) {
    return renderVariationTurn(ctrl, turn, path);
  });
}

function renderVariationMeta(ctrl, move, path) {
  if (!move || empty(move.variations)) return;
  return move.variations.map(function(variation, i) {
    return renderVariationNested(ctrl, variation, treePath.withVariation(path, i + 1));
  });
}

function renderVariationTurn(ctrl, turn, path) {
  var wPath = turn.white ? treePath.withPly(path, turn.white.ply) : null;
  var wMove = wPath ? renderMove(ctrl, turn.white, wPath) : null;
  var wMeta = renderVariationMeta(ctrl, turn.white, wPath);
  var bPath = turn.black ? treePath.withPly(path, turn.black.ply) : null;
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

function renderMeta(ctrl, move, path) {
  var opening = ctrl.data.game.opening;
  opening = (move && opening && opening.ply === move.ply) ? renderCommentOpening(ctrl, opening) : null;
  if (!move || (!opening && empty(move.comments) && empty(move.variations))) return;
  var children = [];
  if (opening) children.push(opening);
  var colorClass = move.ply % 2 === 0 ? 'black ' : 'white ';
  var commentClass;
  if (ctrl.vm.comments && !empty(move.comments)) move.comments.forEach(function(comment) {
    if (comment.indexOf('Inaccuracy.') === 0) commentClass = 'inaccuracy';
    else if (comment.indexOf('Mistake.') === 0) commentClass = 'mistake';
    else if (comment.indexOf('Blunder.') === 0) commentClass = 'blunder';
    children.push(m('div', {
      class: 'comment ' + colorClass + commentClass
    }, comment));
  });
  if (!empty(move.variations)) move.variations.forEach(function(variation, i) {
    if (empty(variation)) return;
    if (i === 0 && !empty(move.comments) && !ctrl.vm.comments) return;
    children.push(renderVariation(
      ctrl,
      variation,
      treePath.withVariation(path, i + 1),
      i === 0 ? colorClass + commentClass : null
    ));
  });
  if (children.length) return m('div', {
    class: 'meta'
  }, children);
}

function renderIndex(txt) {
  return {
    tag: 'index',
    children: [txt]
  };
}

function renderTurnEl(children) {
  return {
    tag: 'turn',
    children: children
  };
}

function renderTurn(ctrl, turn, path) {
  var index = renderIndex(turn.turn);
  var wPath = turn.white ? treePath.withPly(path, turn.white.ply) : null;
  var wMove = wPath ? renderMove(ctrl, turn.white, wPath) : null;
  var wMeta = renderMeta(ctrl, turn.white, wPath);
  var bPath = turn.black ? treePath.withPly(path, turn.black.ply) : null;
  var bMove = bPath ? renderMove(ctrl, turn.black, bPath) : nullMove;
  var bMeta = renderMeta(ctrl, turn.black, bPath);
  if (wMove) {
    if (wMeta) return [
      renderTurnEl([index, wMove, emptyMove]),
      wMeta,
      bMove ? [
        renderTurnEl([index, emptyMove, bMove]),
        bMeta
      ] : null,
    ];
    return [
      renderTurnEl([index, wMove, bMove]),
      bMeta
    ];
  }
  return [
    renderTurnEl([index, emptyMove, bMove]),
    bMeta
  ];
}

function renderTree(ctrl, tree) {
  var turns = [];
  var initPly = ctrl.analyse.firstPly();
  if (initPly % 2 === 0)
    for (var i = 1, nb = tree.length; i < nb; i += 2) turns.push({
      turn: Math.floor((initPly + i) / 2) + 1,
      white: tree[i],
      black: tree[i + 1]
    });
  else {
    turns.push({
      turn: Math.floor(initPly / 2) + 1,
      white: null,
      black: tree[1]
    });
    for (var i = 2, nb = tree.length; i < nb; i += 2) turns.push({
      turn: Math.floor((initPly + i) / 2) + 1,
      white: tree[i],
      black: tree[i + 1]
    });
  }

  var path = treePath.default();
  var tags = [];
  for (var i = 0, len = turns.length; i < len; i++)
    tags.push(renderTurn(ctrl, turns[i], path));

  return tags;
}

function renderAnalyse(ctrl) {
  var result;
  if (ctrl.data.game.status.id >= 30) switch (ctrl.data.game.winner) {
    case 'white':
      result = '1-0';
      break;
    case 'black':
      result = '0-1';
      break;
    default:
      result = '½-½';
  }
  var tree = renderTree(ctrl, ctrl.analyse.tree);
  if (result) {
    tree.push(m('div.result', result));
    var winner = game.getPlayer(ctrl.data, ctrl.data.game.winner);
    tree.push(m('div.status', [
      renderStatus(ctrl),
      winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
    ]));
  }
  return m('div.replay', {
      onmousedown: function(e) {
        var el = e.target.tagName === 'MOVE' ? e.target : e.target.parentNode;
        if (el.tagName !== 'MOVE' || el.classList.contains('empty')) return;
        var path = el.getAttribute('data-path') ||
          '' + (2 * parseInt($(el).siblings('index').text()) - 2 + $(el).index());
        if (path) ctrl.userJump(treePath.read(path));
      },
      onclick: function(e) {
        return false;
      },
      config: function(el, isUpdate) {
        if (!isUpdate) {
          ctrl.vm.autoScroll = autoScroll(el);
          ctrl.vm.autoScroll();
        }
      }
    },
    tree);
}

function wheel(ctrl, e) {
  if (e.deltaY > 0) control.next(ctrl);
  else if (e.deltaY < 0) control.prev(ctrl);
  m.redraw();
  e.preventDefault();
  return false;
}

function inputs(ctrl) {
  if (!ctrl.data.userAnalysis) return null;
  if (ctrl.vm.redirecting) return m.trust(lichess.spinnerHtml);
  var pgnText = pgnExport.renderFullTxt(ctrl);
  return m('div.copyables', [
    m('label.name', 'FEN'),
    m('input.copyable.autoselect[spellCheck=false]', {
      value: ctrl.vm.step.fen,
      onchange: function(e) {
        if (e.target.value !== ctrl.vm.step.fen) ctrl.changeFen(e.target.value);
      }
    }),
    m('div.pgn', [
      m('label.name', 'PGN'),
      m('textarea.copyable.autoselect[spellCheck=false]', {
        value: pgnText
      }),
      m('div.action', [
        m('button.button', {
          onclick: function(e) {
            var pgn = $('.copyables .pgn textarea').val();
            if (pgn !== pgnText) ctrl.changePgn(pgn);
          }
        }, 'Import PGN')
      ])
    ])
  ]);
}

function visualBoard(ctrl) {
  return m('div.lichess_board_wrap', [
    m('div.lichess_board.' + ctrl.data.game.variant.key, {
        config: function(el, isUpdate) {
          if (!isUpdate) el.addEventListener('wheel', function(e) {
            return wheel(ctrl, e);
          });
        }
      },
      chessground.view(ctrl.chessground),
      renderPromotion(ctrl)),
    cevalView.renderGauge(ctrl)
  ]);
}

function blindBoard(ctrl) {
  return m('div.lichess_board_blind', [
    m('div.textual', {
      config: function(el, isUpdate) {
        if (isUpdate) return;
        var url = ctrl.data.player.spectator ?
          router.game(ctrl.data, ctrl.data.player.color) :
          router.player(ctrl.data);
        url += '/text';
        $(el).load(url);
      }
    }),
    chessground.view(ctrl.chessground)
  ]);
}

var cachedButtons = (function() {
  var make = function(icon, effect) {
    return m('button', {
      class: 'button',
      'data-act': effect,
      'data-icon': icon
    });
  };
  return m('div', [
    m('div.jumps', [
      make('Y', 'prev'),
      make('W', 'first')
    ]),
    m('div.jumps', [
      make('X', 'next'),
      make('V', 'last')
    ])
  ])
})();

function icon(c) {
  return {
    tag: 'i',
    attrs: {
      'data-icon': c
    }
  };
}

function buttons(ctrl) {
  return m('div.game_control',
    m('div.buttons', {
      onmousedown: function(e) {
        var action = e.target.getAttribute('data-act') || e.target.parentNode.getAttribute('data-act');
        if (action === 'explorer') ctrl.explorer.toggle();
        else if (action === 'menu') ctrl.actionMenu.toggle();
        else if (control[action]) control[action](ctrl);
      }
    }, [
      cachedButtons,
      m('div', [
        (ctrl.actionMenu.open || !ctrl.explorer.authorized) ? null : m('button', {
          id: 'open_explorer',
          'data-hint': 'Opening explorer',
          'data-act': 'explorer',
          class: 'button hint--bottom' + (ctrl.explorer.enabled() ? ' active' : '')
        }, icon(']')),
        m('button', {
          class: 'button menu hint--bottom' + (ctrl.actionMenu.open ? ' active' : ''),
          'data-hint': 'Menu',
          'data-act': 'menu'
        }, icon('['))
      ])
    ])
  );
}

function renderOpeningBox(ctrl) {
  var opening = ctrl.analyse.getOpening(ctrl.vm.path);
  if (opening) return m('div', {
    class: 'opening_box',
    title: opening.eco + ' ' + opening.name
  }, [
    m('strong', opening.eco),
    ' ' + opening.name
  ]);
}

module.exports = function(ctrl) {
  return [
    m('div', {
      class: ctrl.showEvalGauge() ? 'gauge_displayed' : ''
    }, [
      m('div.lichess_game', {
        config: function(el, isUpdate, context) {
          if (isUpdate) return;
          $('body').trigger('lichess.content_loaded');
        }
      }, [
        ctrl.data.blind ? blindBoard(ctrl) : visualBoard(ctrl),
        m('div.lichess_ground', [
          ctrl.actionMenu.open ? null : crazyView.pocket(ctrl, ctrl.data.opponent.color, 'top'),
          ctrl.actionMenu.open ? actionMenu(ctrl) : [
            cevalView.renderCeval(ctrl),
            renderOpeningBox(ctrl),
            renderAnalyse(ctrl),
            explorerView.renderExplorer(ctrl)
          ],
          ctrl.actionMenu.open ? null : crazyView.pocket(ctrl, ctrl.data.player.color, 'bottom'),
          buttons(ctrl)
        ])
      ])
    ]),
    m('div.underboard', [
      m('div.center', inputs(ctrl)),
      m('div.right')
    ]),
    util.synthetic(ctrl.data) ? null : m('div.analeft', [
      ctrl.forecast ? forecastView(ctrl) : null,
      game.playable(ctrl.data) ? m('div.back_to_game',
        m('a', {
          class: 'button text',
          href: ctrl.data.player.id ? router.player(ctrl.data) : router.game(ctrl.data),
          'data-icon': 'i'
        }, ctrl.trans('backToGame'))
      ) : null
    ])
  ];
};
