var m = require('mithril');
var game = require('game').game;
var status = require('game').status;
var renderClock = require('../clock/view').renderClock;
var renderCorrespondenceClock = require('../correspondenceClock/view');
var renderReplay = require('./replay');
var renderUser = require('./user');
var button = require('./button');

function playerAt(ctrl, position) {
  return ctrl.vm.flip ^ (position === 'top') ? ctrl.data.opponent : ctrl.data.player;
}

function topPlayer(ctrl) {
  return playerAt(ctrl, 'top');
}

function bottomPlayer(ctrl) {
  return playerAt(ctrl, 'bottom');
}

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
  return player.ai ? m('div.username.user_link.online', [
      m('i.line'),
      m('name', renderUser.aiName(ctrl, player))
    ]) :
    renderUser.userHtml(ctrl, player);
}

function isSpinning(ctrl) {
  return ctrl.vm.loading || ctrl.vm.redirecting;
}

function spinning(ctrl) {
  if (isSpinning(ctrl)) return m.trust(lichess.spinnerHtml);
}

function renderTableEnd(ctrl) {
  var buttons = compact(spinning(ctrl) || [
    button.backToTournament(ctrl) || [
      button.answerOpponentRematch(ctrl) ||
      button.cancelRematch(ctrl) ||
      button.followUp(ctrl)
    ]
  ]);
  return [
    renderReplay(ctrl),
    buttons ? m('div.control.buttons', buttons) : null,
    renderPlayer(ctrl, bottomPlayer(ctrl))
  ];
}

function renderTableWatch(ctrl) {
  var buttons = compact(spinning(ctrl) || button.watcherFollowUp(ctrl));
  return [
    renderReplay(ctrl),
    buttons ? m('div.control.buttons', buttons) : null,
    renderPlayer(ctrl, bottomPlayer(ctrl))
  ];
}

function renderTablePlay(ctrl) {
  var d = ctrl.data;
  var buttons = spinning(ctrl) || button.submitMove(ctrl) || compact([
    button.forceResign(ctrl),
    button.threefoldClaimDraw(ctrl),
    button.cancelDrawOffer(ctrl),
    button.answerOpponentDrawOffer(ctrl),
    button.cancelTakebackProposition(ctrl),
    button.answerOpponentTakebackProposition(ctrl), (d.tournament && game.nbMoves(d, d.player.color) === 0) ? m('div.suggestion',
      m('div.text[data-icon=j]',
        ctrl.trans('youHaveNbSecondsToMakeYourFirstMove', d.tournament.nbSecondsForFirstMove)
      )) : null
  ]);
  return [
    renderReplay(ctrl), (ctrl.vm.moveToSubmit || ctrl.vm.dropToSubmit) ? null : (
      isSpinning(ctrl) ? null : m('div', {
        class: 'control icons'
      }, [
        game.abortable(d) ? button.standard(ctrl, null, 'L', 'abortGame', 'abort') :
        button.standard(ctrl, game.takebackable, 'i', 'proposeATakeback', 'takeback-yes', ctrl.takebackYes),
        button.standard(ctrl, game.drawable, '2', 'offerDraw', 'draw-yes'),
        ctrl.vm.resignConfirm ? button.resignConfirm(ctrl) : button.standard(ctrl, game.resignable, 'b', 'resign', 'resign-confirm', ctrl.resign)
      ])
    ),
    buttons ? m('div.control.buttons', buttons) : null,
    renderPlayer(ctrl, bottomPlayer(ctrl))
  ];
}

function whosTurn(ctrl, color) {
  var d = ctrl.data;
  if (status.finished(d) || status.aborted(d)) return;
  return m('div.whos_turn',
    d.game.player === color ? (
      d.player.spectator ? ctrl.trans(d.game.player + 'Plays') : ctrl.trans(
        d.game.player === d.player.color ? 'yourTurn' : 'waitingForOpponent'
      )
    ) : ''
  );
}

function anyClock(ctrl, position) {
  var player = playerAt(ctrl, position);
  if (ctrl.clock) return renderClock(ctrl, player, position);
  else if (ctrl.data.correspondence && ctrl.data.game.turns > 1)
    return renderCorrespondenceClock(
      ctrl.correspondenceClock, ctrl.trans, player.color, position, ctrl.data.game.player
    );
  else return whosTurn(ctrl, player.color);
}

module.exports = function(ctrl) {
  return m('div.table_wrap', [
    anyClock(ctrl, 'top'),
    m('div', {
      class: 'table'
    }, [
      renderPlayer(ctrl, topPlayer(ctrl)),
      m('div.table_inner',
        ctrl.data.player.spectator ? renderTableWatch(ctrl) : (
          game.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
        )
      )
    ]),
    anyClock(ctrl, 'bottom')
  ]);
}
