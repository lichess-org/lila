var m = require('mithril');
var chessground = require('chessground');
var game = require('game').game;
var status = require('game').status;
var opposite = chessground.util.opposite;
var renderClock = require('../clock/view');
var renderCorrespondenceClock = require('../correspondenceClock/view');
var renderReplay = require('../replay/view');
var renderStatus = require('./status');
var renderUser = require('game').view.user;
var button = require('./button');
var m = require('mithril');

function compact(x) {
  if (Object.prototype.toString.call(x) === '[object Array]') {
    var elems = x.filter(function(n) {
      return n !== undefined
    });
    return elems.length > 0 ? elems : null;
  }
  return x;
}

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

var loader = m('div.loader', m('span'));

function renderTableEnd(ctrl) {
  var d = ctrl.data;
  var buttons = compact(ctrl.vm.redirecting ? null : [
    button.backToTournament(ctrl) || [
      button.joinRematch(ctrl) ||
      button.answerOpponentRematch(ctrl) ||
      button.cancelRematch(ctrl) ||
      button.rematch(ctrl)
    ],
    button.analysis(ctrl)
  ]);
  return [
    renderReplay(ctrl.replay),
    buttons ? m('div.control.buttons', buttons) : null,
    renderPlayer(ctrl, d.player)
  ];
}

function renderTableWatch(ctrl) {
  var d = ctrl.data;
  var buttons = compact(ctrl.vm.redirecting ? null : [
    button.viewRematch(ctrl),
    button.viewTournament(ctrl),
    button.analysis(ctrl)
  ]);
  return [
    renderReplay(ctrl.replay),
    buttons ? m('div.control.buttons', buttons) : null,
    renderPlayer(ctrl, d.player)
  ];
}

function renderTablePlay(ctrl) {
  var d = ctrl.data;
  var buttons = compact([
    button.forceResign(ctrl),
    button.threefoldClaimDraw(ctrl),
    button.cancelDrawOffer(ctrl),
    button.answerOpponentDrawOffer(ctrl),
    button.cancelTakebackProposition(ctrl),
    button.answerOpponentTakebackProposition(ctrl), (game.mandatory(d) && game.nbMoves(d, d.player.color) === 0) ? m('div.text[data-icon=j]',
      ctrl.trans('youHaveNbSecondsToMakeYourFirstMove', 30)
    ) : null
  ]);
  return [
    renderReplay(ctrl.replay),
    m('div.control.icons', [
      button.standard(ctrl, game.abortable, 'L', 'abortGame', 'abort'),
      button.standard(ctrl, game.takebackable, 'i', 'proposeATakeback', 'takeback-yes'),
      button.standard(ctrl, game.drawable, '2', 'offerDraw', 'draw-yes'),
      button.standard(ctrl, game.resignable, 'b', 'resign', 'resign')
    ]),
    buttons ? m('div.control.buttons', buttons) : null,
    renderPlayer(ctrl, d.player)
  ];
}

function whosTurn(ctrl, color) {
  if (status.finished(ctrl.data)) return;
  return m('div.whos_turn',
    ctrl.data.game.player == color ? ctrl.trans(
      ctrl.data.game.player == ctrl.data.player.color ? 'yourTurn' : 'waitingForOpponent'
    ) : ''
  );
}

module.exports = function(ctrl) {
  var clockRunningColor = ctrl.isClockRunning() ? ctrl.data.game.player : null;
  return m('div.table_wrap', [
    (ctrl.clock && !ctrl.data.blind) ? renderClock(
      ctrl.clock,
      ctrl.data.opponent.color,
      "top", clockRunningColor
    ) : (
      ctrl.data.correspondence ? renderCorrespondenceClock(
        ctrl.correspondenceClock, ctrl.trans, ctrl.data.opponent.color, "top", ctrl.data.game.player
      ) : whosTurn(ctrl, ctrl.data.opponent.color)
    ),
    m('div', {
      class: 'table' + (status.finished(ctrl.data) ? ' finished' : '')
    }, [
      renderPlayer(ctrl, ctrl.data.opponent),
      m('div.table_inner',
        ctrl.data.player.spectator ? renderTableWatch(ctrl) : (
          game.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
        )
      )
    ]), (ctrl.clock && !ctrl.data.blind) ? [
      renderClock(ctrl.clock, ctrl.data.player.color, "bottom", clockRunningColor),
      button.moretime(ctrl)
    ] : (
      ctrl.data.correspondence ? renderCorrespondenceClock(
        ctrl.correspondenceClock, ctrl.trans, ctrl.data.player.color, "bottom", ctrl.data.game.player
      ) : whosTurn(ctrl, ctrl.data.player.color))
  ]);
}
