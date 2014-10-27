var m = require('mithril');
var chessground = require('chessground');
var analyse = require('./analyse');
var partial = require('chessground').util.partial;
var renderStatus = require('game').view.status;

function renderTd(move, ply, curPly) {
  return move ? {
    tag: 'td',
    attrs: {
      class: 'move' + (ply === curPly ? ' active' : ''),
      'data-ply': ply
    },
    children: move
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
      renderTd(pair[0], 2 * i + 1, ctrl.vm.ply),
      renderTd(pair[1], 2 * i + 2, ctrl.vm.ply)
    ]);
  });
  if (result) {
    trs.push(m('tr', m('td.result[colspan=3]', result)));
    var winner = analyse.getPlayer(ctrl.data, ctrl.data.game.winner);
    trs.push(m('tr.status', m('td[colspan=3]', [
      renderStatus(ctrl),
      winner ? ', ' + ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious') : null
    ])));
  }
  return m('table',
    m('tbody', {
        onclick: function(e) {
          var ply = e.target.getAttribute('data-ply');
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
          m('div.replay',
            renderMovelist(ctrl)))
      ])
    ]),
    m('div.underboard', [
      m('div.right', [
        // [ctrl.data.opponent, ctrl.data.player].map(partial(blursOf, ctrl)), [ctrl.data.opponent, ctrl.data.player].map(partial(holdOf, ctrl))
      ])
    ])
  ];
};
