var m = require('mithril');
var chessground = require('chessground');
var classSet = require('chessground').util.classSet;
var game = require('game').game;
var partial = require('chessground').util.partial;
var renderStatus = require('game').view.status;
var mod = require('game').view.mod;

function renderEval(e) {
  e = Math.round(e / 10) / 10;
  return (e > 0 ? '+' : '') + e;
}

function autoScroll(movelist) {
  var plyEl = movelist.querySelector('.active');
  if (plyEl) movelist.scrollTop = plyEl.offsetTop - movelist.offsetHeight / 2 + plyEl.offsetHeight / 2;
}

var emptyMove = m('div.move.empty', '...');

function renderMove(ctrl, move) {
  if (!move) return emptyMove;
  return {
    tag: 'a',
    attrs: {
      class: 'move' + (move.ply === ctrl.vm.ply ? ' active' : ''),
      'data-ply': move.ply
    },
    children: [
      move.san,
      move.eval ? m('span', renderEval(move.eval)) : (
        move.mate ? m('span', '#' + move.mate) : null)
    ]
  };
}

function renderVariation(ctrl, variation) {
  return m('div.variation', variation.map(function(turn) {
    return renderVariationTurn(ctrl, turn);
  }));
}

function renderVariationTurn(ctrl, turn) {
  var wMove = turn.white ? renderMove(ctrl, turn.white) : null;
  var bMove = turn.black ? renderMove(ctrl, turn.black) : null;
  if (turn.white) return [turn.turn + '.', wMove, (turn.black ? [m.trust('&nbsp;'), bMove] : ''), ' '];
  return [turn.turn + '...', bMove, ' '];
}

function renderMeta(ctrl, move) {
  if (!move || !move.comments || !move.variations) return;
  return [
    move.comments ? move.comments.map(function(comment) {
      return m('div.comment', comment);
    }) : null,
    move.variations ? move.variations.map(function(variation) {
      return renderVariation(ctrl, variation);
    }) : null,
  ];
}

function renderTurn(ctrl, turn) {
  var index = m('div.index', turn.turn);
  var wMove = turn.white ? renderMove(ctrl, turn.white) : null;
  var bMove = turn.black ? renderMove(ctrl, turn.black) : null;
  var wMeta = renderMeta(ctrl, turn.white);
  var bMeta = renderMeta(ctrl, turn.black);
  if (turn.white) {
    if (turn.white.comments || turn.white.variations) return [
      m('div.turn', [index, wMove, emptyMove]),
      wMeta,
      turn.black ? [
        m('div.turn', [index, emptyMove, bMove]),
        bMeta
      ] : null,
    ];
    return [
      m('div.turn', [index, wMove, bMove]),
      bMeta
    ];
  }
  return [
    m('div.turn', [index, emptyMove, bMove]),
    bMeta
  ];
}

function renderTurns(ctrl, turns) {
  return turns.map(function(turn) {
    return renderTurn(ctrl, turn);
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
  var turns = renderTurns(ctrl, ctrl.analyse.turns);
  if (result) {
    turns.push(m('div.result', result));
    var winner = game.getPlayer(ctrl.data, ctrl.data.game.winner);
    turns.push(m('div.status', [
      renderStatus(ctrl),
      winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
    ]));
  }
  return m('div.analyse', {
      onclick: function(e) {
        var ply = e.target.getAttribute('data-ply') || e.target.parentNode.getAttribute('data-ply');
        if (ply) ctrl.jump(parseInt(ply));
        m.redraw();
      }
    },
    turns);
}

function visualBoard(ctrl) {
  return m('div.lichess_board_wrap',
    m('div.lichess_board.' + ctrl.data.game.variant.key,
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
      m('a.button.flip.hint--bottom', {
        'data-hint': ctrl.trans('flipBoard'),
        onclick: ctrl.flip
      }, m('span[data-icon=B]')),
      m('a.button.hint--bottom', {
        'data-hint': ctrl.trans('boardEditor'),
        href: ctrl.router.Editor.game(ctrl.data.game.id).url + '?fen=' + ctrl.vm.situation.fen
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
        ['first', 'W', 1],
        ['prev', 'Y', ctrl.vm.ply - 1],
        ['next', 'X', ctrl.vm.ply + 1],
        ['last', 'V', nbMoves]
      ].map(function(b) {
        var enabled = ctrl.vm.ply != b[2] && b[2] >= 1 && b[2] <= nbMoves;
        return m('a', {
          class: 'button ' + b[0] + ' ' + classSet({
            disabled: (ctrl.broken || !enabled),
            glowing: ctrl.vm.late && b[0] === 'last'
          }),
          'data-icon': b[1],
          onclick: enabled ? partial(ctrl.jump, b[2]) : null
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
