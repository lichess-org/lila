var m = require('mithril');
var chessground = require('chessground');
var round = require('../round');
var status = require('../status');
var opposite = chessground.util.opposite;
var renderClock = require('../clock/view');
var renderStatus = require('./status');
var renderUser = require('./user');
var button = require('./button');

function renderPlayer(ctrl, player) {
  return player.ai ? m('div.username.on-game', [
    ctrl.trans('aiNameLevelAiLevel', 'Stockfish', player.ai),
    m('span.status')
  ]) : m('div', {
      class: 'username ' + player.color + (player.onGame ? ' on-game' : '')
    },
    renderUser(ctrl, player)
  );
}

function renderKing(ctrl, color) {
  var loader = ctrl.data.reloading || ctrl.data.redirecting;
  return m('div.no-square', loader ? m('div.loader', m('span')) : m('div.cg-piece.king.' + color));
}

function renderResult(ctrl) {
  var winner = round.getPlayer(ctrl.data, ctrl.data.game.winner);
  return winner ? m('div.player.' + winner.color, [
      renderKing(ctrl, winner.color),
      m('p', [
        renderStatus(ctrl),
        m('br'),
        ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious')
      ])
    ]) :
    m('div.player', m('p', renderStatus(ctrl)));
}

function renderTableEnd(ctrl) {
  var d = ctrl.data;
  return [
    m('div.current_player', renderResult(ctrl)),
    m('div.control.buttons', ctrl.data.redirecting ? null : [
      button.backToTournament(ctrl) || (
        d.opponent.ai ? button.rematch(ctrl) : [
          m('div.separator'),
          button.answerOpponentRematch(ctrl) || button.cancelRematch(ctrl) || button.rematch(ctrl)
        ]),
      button.newGame(ctrl)
    ])
  ];
}

function renderTableWatch(ctrl) {
  var d = ctrl.data;
  return [
    m('div.current_player', (status.finished(d) || status.aborted(d)) ? renderResult(ctrl) : (
      m('div.player', [
        renderKing(ctrl, d.game.player),
        m('p', ctrl.trans(d.game.player == 'white' ? 'whitePlays' : 'blackPlays'))
      ]))),
    m('div.separator'),
    renderPlayer(ctrl, d.player),
    m('div.control.buttons', ctrl.data.redirecting ? null : [
      button.viewRematch(ctrl),
      button.viewTournament(ctrl)
    ])
  ];
}

function renderTablePlay(ctrl) {
  var d = ctrl.data;
  return [
    m('div.current_player',
      m('div.player', [
        renderKing(ctrl, d.game.player),
        m('p', ctrl.trans(d.game.player == d.player.color ? 'yourTurn' : 'waiting'))
      ])
    ),
    m('div.control.icons', [
      button.standard(ctrl, round.abortable, 'L', 'abortGame', 'abort'),
      button.standard(ctrl, round.takebackable, 'i', 'proposeATakeback', 'takeback-yes'),
      button.standard(ctrl, round.drawable, '2', 'offerDraw', 'draw-yes'),
      button.standard(ctrl, round.resignable, 'b', 'resign', 'resign')
    ]),
    button.forceResign(ctrl),
    button.threefoldClaimDraw(ctrl),
    button.cancelDrawOffer(ctrl),
    button.answerOpponentDrawOffer(ctrl),
    button.cancelTakebackProposition(ctrl),
    button.answerOpponentTakebackProposition(ctrl), (round.mandatory(d) && round.nbMoves(d, d.player.color) === 0) ? m('div[data-icon=j]',
      ctrl.trans('youHaveNbSecondsToMakeYourFirstMove', 30)
    ) : null
  ];
}

module.exports = function(ctrl) {
  var clockRunningColor = ctrl.isClockRunning() ? ctrl.data.game.player : null;
  return m('div.table_wrap', [
    (ctrl.clock && !ctrl.data.blindMode) ? renderClock(ctrl.clock, opposite(ctrl.data.player.color), "top", clockRunningColor) : null,
    m('div', {
      class: 'table onbg' + (status.finished(ctrl.data) ? ' finished' : '')
    }, [
      renderPlayer(ctrl, ctrl.data.opponent),
      m('div.separator'),
      m('div.table_inner',
        ctrl.data.player.spectator ? renderTableWatch(ctrl) : (
          round.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
        )
      )
    ]), (ctrl.clock && !ctrl.data.blindMode) ? [
      renderClock(ctrl.clock, ctrl.data.player.color, "bottom", clockRunningColor),
      button.moretime(ctrl)
    ] : null
  ])
}
