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
  return util.throttle(500, false, function autoScroll() {
    raf(function() {
      var plyEl = el.querySelector('.active') || el.querySelector('.turn:first-child');
      if (plyEl) el.scrollTop = plyEl.offsetTop - el.offsetHeight / 2 + plyEl.offsetHeight / 2;
    });
  });
}

var emptyMove = m('move.empty', '...');

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
  return m('div', {
    class: klass + ' ' + classSet({
      variation: true,
      menu: showMenu
    })
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
    renderVariationContent(ctrl, variation, path)
  ]);
}

function renderVariationNested(ctrl, variation, path) {
  return m('span.variation', [
    '(',
    renderVariationContent(ctrl, variation, path),
    ')'
  ]);
}

function renderVariationContent(ctrl, variation, path) {
  var turns = [];
  if (variation[0].ply % 2 === 0) {
    variation = variation.slice(0);
    var move = variation.shift();
    turns.push({
      turn: plyToTurn(move.ply),
      black: move
    });
  }
  var visiting = treePath.contains(path, ctrl.vm.path);
  var maxPlies = Math.min(visiting ? 999 : (path[2] ? 2 : 4), variation.length);
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

function renderOpening(ctrl, opening) {
  return m('div.comment.opening', opening.code + ': ' + opening.name);
}

function renderMeta(ctrl, move, path) {
  if (!ctrl.vm.comments) return;
  var opening = ctrl.data.game.opening;
  opening = (move && opening && opening.size === move.ply) ? renderOpening(ctrl, opening) : null;
  if (!move || (!opening && empty(move.comments) && empty(move.variations))) return;
  var children = [];
  if (opening) children.push(opening);
  var colorClass = move.ply % 2 === 0 ? 'black ' : 'white ';
  var commentClass;
  if (!empty(move.comments)) move.comments.forEach(function(comment) {
    if (comment.indexOf('Inaccuracy.') === 0) commentClass = 'inaccuracy';
    else if (comment.indexOf('Mistake.') === 0) commentClass = 'mistake';
    else if (comment.indexOf('Blunder.') === 0) commentClass = 'blunder';
    children.push(m('div', {
      class: 'comment ' + colorClass + commentClass
    }, comment));
  });
  if (!empty(move.variations)) move.variations.forEach(function(variation, i) {
    if (empty(variation)) return;
    children.push(renderVariation(
      ctrl,
      variation,
      treePath.withVariation(path, i + 1),
      i === 0 ? colorClass + commentClass : null
    ));
  });
  return m('div', {
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
  var bMove = bPath ? renderMove(ctrl, turn.black, bPath) : null;
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
  return m('div.analyse', {
      onmousedown: function(e) {
        var el = e.target.tagName === 'MOVE' ? e.target : e.target.parentNode;
        if (el.tagName !== 'MOVE') return;
        var path = el.getAttribute('data-path') ||
          '' + (2 * parseInt($(el).siblings('index').text()) - 2 + $(el).index());
        if (path) ctrl.userJump(treePath.read(path));
      },
      onclick: function(e) {
        return false;
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
  return m('div.copyables', [
    m('label.name', 'FEN'),
    m('input.copyable[readonly][spellCheck=false]', {
      value: ctrl.vm.step.fen
    }),
    m('div.pgn', [
      m('label.name', 'PGN'),
      m('textarea.copyable[readonly][spellCheck=false]', {
        value: pgnExport.renderStepsTxt(ctrl.analyse.getSteps(ctrl.vm.path))
      })
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

function buttons(ctrl) {
  var make = function(icon, effect) {
    return m('button.button', {
      'data-icon': icon,
      onclick: partial(effect, ctrl)
    });
  }
  return [
    m('div.game_control', [
      m('div', [
        m('div.jumps', [
          make('Y', control.prev),
          make('W', control.first)
        ]),
        m('div.jumps', [
          make('X', control.next),
          make('V', control.last)
        ])
      ]),
      m('div', [
        m('button.button', {
          onclick: ctrl.explorer.toggle,
          'data-icon': ']',
          class: ctrl.explorer.enabled() ? 'active' : ''
        }),
        ctrl.explorer.allowed ? m('button.button.menu', {
          onclick: ctrl.actionMenu.toggle,
          'data-icon': '[',
          class: ctrl.actionMenu.open ? 'active' : ''
        }) : null
      ])
    ])
  ];
}

module.exports = function(ctrl) {
  return [
    m('div', {
      class: classSet({
        top: true,
        ceval_displayed: ctrl.ceval.allowed(),
        explorer_displayed: ctrl.explorer.enabled(),
        gauge_displayed: ctrl.showEvalGauge(),
        crazy: ctrl.data.game.variant.key === 'crazyhouse'
      })
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
            m('div.replay', {
                config: function(el, isUpdate) {
                  if (!isUpdate) ctrl.vm.autoScroll = autoScroll(el);
                }
              },
              renderAnalyse(ctrl)),
            explorerView.renderExplorer(ctrl)
          ],
          buttons(ctrl),
          ctrl.actionMenu.open ? null : crazyView.pocket(ctrl, ctrl.data.player.color, 'bottom')
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
