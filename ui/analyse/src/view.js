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

function renderTd(ctrl, move, ply) {
  var analysis = ctrl.data.analysis.moves[ply - 1];
  return move ? {
    tag: 'td',
    attrs: {
      class: 'move' + (ply === ctrl.vm.ply ? ' active' : ''),
      'data-ply': ply
    },
    children: [
      move,
      analysis.eval ? m('span', renderEval(analysis.eval)) : (
        analysis.mate ? m('span', '#' + analysis.mate) : null)
    ]
  } : null;
}

function renderMovelist(ctrl) {
  var moves = ctrl.data.game.moves;
  var pairs = [];
  for (var i = 0; i < moves.length; i += 2) pairs.push([moves[i], moves[i + 1]]);
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
  var trs = pairs.map(function(pair, i) {
    return m('tr', [
      m('td.index', i + 1),
      renderTd(ctrl, pair[0], 2 * i + 1),
      renderTd(ctrl, pair[1], 2 * i + 2)
    ]);
  });
  if (result) {
    trs.push(m('tr', m('td.result[colspan=3]', result)));
    var winner = game.getPlayer(ctrl.data, ctrl.data.game.winner);
    trs.push(m('tr.status', m('td[colspan=3]', [
      renderStatus(ctrl),
      winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
    ])));
  }
  return m('table',
    m('tbody', {
        onclick: function(e) {
          var ply = e.target.getAttribute('data-ply') || e.target.parentNode.getAttribute('data-ply');
          if (ply) ctrl.jump(parseInt(ply));
        }
      },
      trs));
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
            renderMovelist(ctrl)))
      ])
    ]),
    m('div.underboard', [
      m('div.center', buttons(ctrl)),
      m('div.right', [
        [ctrl.data.opponent, ctrl.data.player].map(partial(mod.blursOf, ctrl)), [ctrl.data.opponent, ctrl.data.player].map(partial(mod.holdOf, ctrl))
      ])
    ])
  ];
};
