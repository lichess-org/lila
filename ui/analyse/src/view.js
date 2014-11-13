var m = require('mithril');
var chessground = require('chessground');
var classSet = require('chessground').util.classSet;
var game = require('game').game;
var partial = require('chessground').util.partial;
var renderStatus = require('game').view.status;
var mod = require('game').view.mod;
var treePath = require('./path');
var control = require('./control');

function renderEval(e) {
  e = Math.round(e / 10) / 10;
  return (e > 0 ? '+' : '') + e;
}

function autoScroll(movelist) {
  var plyEl = movelist.querySelector('.active');
  if (plyEl) movelist.scrollTop = plyEl.offsetTop - movelist.offsetHeight / 2 + plyEl.offsetHeight / 2;
}

var emptyMove = m('em.move.empty', '...');

function renderMove(ctrl, move, path) {
  if (!move) return emptyMove;
  var pathStr = treePath.write(treePath.withPly(path, move.ply));
  return {
    tag: 'a',
    attrs: {
      class: 'move' + (pathStr === ctrl.vm.pathStr ? ' active' : ''),
      'data-path': pathStr
    },
    children: [
      move.san,
      move.eval ? m('span', renderEval(move.eval)) : (
        move.mate ? m('span', '#' + move.mate) : null)
    ]
  };
}

function plyToTurn(ply) {
  return Math.floor((ply - 1) / 2) + 1;
}

function renderVariation(ctrl, variation, path) {
  var turns = [];
  if (variation[0].ply % 2 === 0) {
    variation = variation.slice(0);
    var move = variation.shift();
    turns.push({
      turn: plyToTurn(move.ply),
      black: move
    });
  }
  for (i = 0, nb = variation.length; i < nb; i += 2) turns.push({
    turn: plyToTurn(variation[i].ply),
    white: variation[i],
    black: variation[i + 1]
  });
  return m('div.variation', turns.map(function(turn) {
    return renderVariationTurn(ctrl, turn, path);
  }));
}

function renderVariationTurn(ctrl, turn, path) {
  var wMove = turn.white ? renderMove(ctrl, turn.white, path) : null;
  var bMove = turn.black ? renderMove(ctrl, turn.black, path) : null;
  if (turn.white) return [turn.turn + '.', wMove, (turn.black ? [m.trust('&nbsp;'), bMove] : ''), ' '];
  return [turn.turn + '...', bMove, ' '];
}

function renderOpening(ctrl, opening) {
  return m('div.comment.opening', [
    opening.code,
    ': ',
    opening.name
  ]);
}

function renderMeta(ctrl, move, path) {
  if (!ctrl.vm.comments) return;
  var opening = ctrl.data.game.opening;
  opening = (move && opening && opening.size == move.ply) ? renderOpening(ctrl, opening) : null;
  if (!move || (!opening && !move.comments.length && !move.variations.length)) return;
  return [
    opening,
    move.comments.length ? move.comments.map(function(comment) {
      return m('div.comment', comment);
    }) : null,
    move.variations.length ? move.variations.map(function(variation, i) {
      return renderVariation(ctrl, variation, treePath.withVariation(path, i + 1));
    }) : null,
  ];
}

function renderIndex(turn) {
  return {
    tag: 'div',
    attrs: {
      class: 'index'
    },
    children: turn.turn
  };
}

function renderTurnDiv(children) {
  return {
    tag: 'div',
    attrs: {
      class: 'turn',
    },
    children: children
  };
}

function renderTurn(ctrl, turn, path) {
  var index = renderIndex(turn);
  var wMove = turn.white ? renderMove(ctrl, turn.white, path) : null;
  var wMeta = renderMeta(ctrl, turn.white, path);
  var bMove = turn.black ? renderMove(ctrl, turn.black, path) : null;
  var bMeta = renderMeta(ctrl, turn.black, path);
  if (turn.white) {
    if (wMeta) return [
      renderTurnDiv([index, wMove, emptyMove]),
      wMeta,
      turn.black ? [
        renderTurnDiv([index, emptyMove, bMove]),
        bMeta
      ] : null,
    ];
    return [
      renderTurnDiv([index, wMove, bMove]),
      bMeta
    ];
  }
  return [
    renderTurnDiv([index, emptyMove, bMove]),
    bMeta
  ];
}

function renderTree(ctrl, tree) {
  var turns = [];
  for (i = 0, nb = tree.length; i < nb; i += 2) turns.push({
    turn: Math.floor(i / 2) + 1,
    white: tree[i],
    black: tree[i + 1]
  });
  var path = treePath.default();
  return turns.map(function(turn) {
    return renderTurn(ctrl, turn, path);
  });
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
      onclick: function(e) {
        var path = e.target.getAttribute('data-path') || e.target.parentNode.getAttribute('data-path');
        if (path) {
          ctrl.jump(treePath.read(path));
          m.redraw();
        }
      }
    },
    tree);
}

function visualBoard(ctrl) {
  return m('div.lichess_board_wrap',
    m('div.lichess_board.' + ctrl.data.game.variant.key, {
        config: function(el, isUpdate) {
          if (!isUpdate) el.addEventListener('wheel', function(e) {
            if (e.deltaY > 0) control.next(ctrl);
            else if (e.deltaY < 0) control.prev(ctrl);
            m.redraw();
            e.preventDefault();
            return false;
          });
        }
      },
      chessground.view(ctrl.chessground)));
}

function blindBoard(ctrl) {
  return m('div.lichess_board_blind', [
    m('div.textual', {
      config: function(el, isUpdate) {
        if (!isUpdate) blind.init(el, ctrl);
      }
    }),
    chessground.view(ctrl.chessground)
  ]);
}

function buttons(ctrl) {
  var nbMoves = ctrl.data.game.moves.length;
  return [
    m('div.game_control', [
      m('a.button.hint--bottom', {
        'data-hint': ctrl.trans('flipBoard'),
        href: ctrl.router.Round.watcher(ctrl.data.game.id, ctrl.data.opponent.color).url
      }, m('span[data-icon=B]')),
      m('a.button.hint--bottom', {
        'data-hint': ctrl.trans('boardEditor'),
        href: ctrl.data.game.id + '/edit?fen=' + ctrl.vm.situation.fen
      }, m('span[data-icon=m]')),
      m('a.button.hint--bottom', {
        'data-hint': ctrl.trans('continueFromHere'),
        onclick: function() {
          ctrl.vm.continue = !ctrl.vm.continue
        }
      }, m('span[data-icon=U]')),
      m('div.jumps.hint--bottom', {
        'data-hint': 'Tip: use your keyboard arrow keys!'
      }, [
        ['first', 'W', control.first],
        ['prev', 'Y', control.prev],
        ['next', 'X', control.next],
        ['last', 'V', control.last]
      ].map(function(b) {
        var enabled = true;
        return m('a', {
          class: 'button ' + b[0] + ' ' + classSet({
            disabled: (ctrl.broken || !enabled),
            glowing: ctrl.vm.late && b[0] === 'last'
          }),
          'data-icon': b[1],
          onclick: enabled ? partial(b[2], ctrl) : null
        });
      }))
    ]),
    ctrl.vm.continue ? m('div.continue', [
      m('a.button', {
        href: ctrl.router.Round.continue(ctrl.data.game.id, 'ai').url + '?fen=' + ctrl.vm.situation.fen
      }, ctrl.trans('playWithTheMachine')),
      m('a.button', {
        href: ctrl.router.Round.continue(ctrl.data.game.id, 'friend').url + '?fen=' + ctrl.vm.situation.fen
      }, ctrl.trans('playWithAFriend'))
    ]) : null
  ];
}

module.exports = function(ctrl) {
  return [
    m('div.top', [
      m('div.lichess_game', {
        config: function(el, isUpdate, context) {
          if (isUpdate) return;
          $('body').trigger('lichess.content_loaded');
        }
      }, [
        ctrl.data.blind ? blindBoard(ctrl) : visualBoard(ctrl),
        m('div.lichess_ground',
          m('div.replay', {
              config: function(el, isUpdate) {
                autoScroll(el);
                if (!isUpdate) setTimeout(partial(autoScroll, el), 100);
              }
            },
            renderAnalyse(ctrl)))
      ])
    ]),
    m('div.underboard', [
      m('div.center', buttons(ctrl)),
      m('div.right')
    ])
  ];
};
