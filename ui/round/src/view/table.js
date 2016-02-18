var m = require('mithril');
var chessground = require('chessground');
var classSet = chessground.util.classSet;
var partial = chessground.util.partial;
var game = require('game').game;
var status = require('game').status;
var opposite = chessground.util.opposite;
var socket = require('../socket');
var clockView = require('../clock/view');
var renderCorrespondenceClock = require('../correspondenceClock/view');
var renderReplay = require('./replay');
var renderUser = require('./user');
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
    m('span.status.hint--top', {
      'data-hint': 'Artificial intelligence is ready'
    }, m('span', {
      'data-icon': '3'
    }))
  ]) : m('div', {
      class: 'username ' + player.color + (player.onGame ? ' on-game' : '')
    },
    renderUser(ctrl, player)
  );
}

function isSpinning(ctrl) {
  return ctrl.vm.loading || ctrl.vm.redirecting;
}

function spinning(ctrl) {
  if (isSpinning(ctrl)) return m.trust(lichess.spinnerHtml);
}

function renderTableEnd(ctrl) {
  var d = ctrl.data;
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
    renderPlayer(ctrl, d.player)
  ];
}

function renderTableWatch(ctrl) {
  var d = ctrl.data;
  var buttons = compact(spinning(ctrl) || button.watcherFollowUp(ctrl));
  return [
    renderReplay(ctrl),
    buttons ? m('div.control.buttons', buttons) : null,
    renderPlayer(ctrl, d.player)
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
    renderReplay(ctrl), (ctrl.vm.moveToSubmit || ctrl.vm.dropToSubmit || ctrl.forceResignable()) ? null : (
      isSpinning(ctrl) ? null : m('div.control.icons', [
        game.abortable(d) ? button.standard(ctrl, null, 'L', 'abortGame', 'abort') :
        button.standard(ctrl, game.takebackable, 'i', 'proposeATakeback', 'takeback-yes', ctrl.takebackYes),
        button.standard(ctrl, game.drawable, '2', 'offerDraw', 'draw-yes'),
        ctrl.vm.resignConfirm ? button.resignConfirm(ctrl) : button.standard(ctrl, game.resignable, 'b', 'resign', 'resign-confirm', ctrl.resign)
      ])
    ),
    buttons ? m('div.control.buttons', buttons) : null,
    renderPlayer(ctrl, d.player)
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

function goBerserk(ctrl) {
  if (!game.berserkableBy(ctrl.data)) return;
  if (ctrl.vm.goneBerserk[ctrl.data.player.color]) return;
  return m('button', {
    class: 'button berserk hint--bottom-left',
    'data-hint': "GO BERSERK! Half the time, bonus point",
    onclick: ctrl.goBerserk
  }, m('span', {
    'data-icon': '`'
  }));
}

function tourRank(d, color, position) {
  if (d.tournament && d.tournament.ranks) return m('div', {
    class: 'tournament_rank ' + position,
    title: 'Current tournament rank'
  }, '#' + d.tournament.ranks[color]);
}

function renderClock(ctrl, color, position) {
  var time = ctrl.clock.data[color];
  var running = ctrl.isClockRunning() && ctrl.data.game.player === color;
  return [
    m('div', {
      class: 'clock clock_' + color + ' clock_' + position + ' ' + classSet({
        'outoftime': !time,
        'running': running,
        'emerg': time < ctrl.clock.data.emerg
      })
    }, [
      clockView.showBar(ctrl.clock, time, ctrl.vm.goneBerserk[color]),
      m('div.time', m.trust(clockView.formatClockTime(ctrl.clock, time * 1000, running))),
      ctrl.data.player.color === color ? goBerserk(ctrl) : null
    ]),
    position === 'bottom' ? button.moretime(ctrl) : null,
    tourRank(ctrl.data, color, position)
  ];
}

function anyClock(ctrl, color, position) {
  if (ctrl.clock && !ctrl.data.blind) return renderClock(ctrl, color, position);
  else if (ctrl.data.correspondence && ctrl.data.game.turns > 1)
    return renderCorrespondenceClock(
      ctrl.correspondenceClock, ctrl.trans, color, position, ctrl.data.game.player
    );
  else return whosTurn(ctrl, color);
}

module.exports = function(ctrl) {
  var showCorrespondenceClock = ctrl.data.correspondence && ctrl.data.game.turns > 1;
  return m('div.table_wrap', [
    anyClock(ctrl, ctrl.data.opponent.color, 'top'),
    m('div', {
      class: 'table' + (status.finished(ctrl.data) ? ' finished' : '')
    }, [
      renderPlayer(ctrl, ctrl.data.opponent),
      m('div.table_inner',
        ctrl.data.player.spectator ? renderTableWatch(ctrl) : (
          game.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
        )
      )
    ]),
    anyClock(ctrl, ctrl.data.player.color, 'bottom')
  ]);
}
