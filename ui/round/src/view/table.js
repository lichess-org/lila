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

function loader() {
  return m('div.loader.fast');
}

function renderTableEnd(ctrl) {
  var d = ctrl.data;
  var buttons = compact(ctrl.vm.redirecting ? loader() : [
    button.backToTournament(ctrl) || [
      button.joinRematch(ctrl) ||
      button.answerOpponentRematch(ctrl) ||
      button.cancelRematch(ctrl) ||
      button.rematch(ctrl)
    ],
    button.newOpponent(ctrl),
    button.analysis(ctrl)
  ]);
  return [
    renderReplay(ctrl),
    buttons ? m('div.control.buttons', buttons) : null,
    renderPlayer(ctrl, d.player)
  ];
}

function renderTableWatch(ctrl) {
  var d = ctrl.data;
  var buttons = compact(ctrl.vm.redirecting ? loader() : [
    button.viewRematch(ctrl),
    button.viewTournament(ctrl),
    button.analysis(ctrl)
  ]);
  return [
    renderReplay(ctrl),
    buttons ? m('div.control.buttons', buttons) : null,
    renderPlayer(ctrl, d.player)
  ];
}

function renderTablePlay(ctrl) {
  var d = ctrl.data;
  var buttons = button.submitMove(ctrl) || compact([
    button.forceResign(ctrl),
    button.threefoldClaimDraw(ctrl),
    button.cancelDrawOffer(ctrl),
    button.answerOpponentDrawOffer(ctrl),
    button.cancelTakebackProposition(ctrl),
    button.answerOpponentTakebackProposition(ctrl), (d.tournament && game.nbMoves(d, d.player.color) === 0) ? m('div.text[data-icon=j]',
      ctrl.trans('youHaveNbSecondsToMakeYourFirstMove', 15)
    ) : null
  ]);
  return [
    renderReplay(ctrl),
    ctrl.vm.moveToSubmit ? null : (
      button.feedback(ctrl) || m('div.control.icons', [
        game.abortable(d) ? button.standard(ctrl, null, 'L', 'abortGame', 'abort') :
        button.standard(ctrl, game.takebackable, 'i', 'proposeATakeback', 'takeback-yes', partial(ctrl.takebackYes)),
        button.standard(ctrl, game.drawable, '2', 'offerDraw', 'draw-yes'),
        button.standard(ctrl, game.resignable, 'b', 'resign', 'resign')
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

var berserkIcon = m('span.berserk.hint--bottom-left', {
  'data-hint': "BERSERK! Half the time, bonus point"
}, m('span', {
  'data-icon': '`'
}));

function goBerserk(ctrl) {
  return m('button', {
    class: 'button berserk hint--bottom-left',
    'data-hint': "GO BERSERK! Half the time, bonus point",
    onclick: ctrl.berserk
  }, m('span', {
    'data-icon': '`'
  }));
}

function renderClock(ctrl, color, position) {
  var time = ctrl.clock.data[color];
  var running = ctrl.isClockRunning() && ctrl.data.game.player === color;
  return m('div', {
    class: 'clock clock_' + color + ' clock_' + position + ' ' + classSet({
      'outoftime': !time,
      'running': running,
      'emerg': time < ctrl.clock.data.emerg
    })
  }, [
    clockView.showBar(ctrl.clock, time),
    m('div.time', m.trust(clockView.formatClockTime(ctrl.clock, time * 1000, running))),
    game.berserkOf(ctrl.data, color) ? berserkIcon : (
      ctrl.data.player.color === color && game.berserkableBy(ctrl.data) ? goBerserk(ctrl) : null
    )
  ]);
}

function renderRelayClock(ctrl, color, position) {
  var time = ctrl.relayClock.data[color];
  var running = ctrl.isClockRunning() && ctrl.data.game.player === color;
  return m('div', {
    class: 'clock clock_' + color + ' clock_' + position + ' ' + classSet({
      'outoftime': !time,
      'running': running
    })
  }, m('div.time', m.trust(clockView.formatClockTime(ctrl.relayClock, time * 1000, running))));
}

function anyClock(ctrl, color, position) {
  if (ctrl.clock && !ctrl.data.blind) return renderClock(ctrl, color, position);
  else if (ctrl.relayClock) return renderRelayClock(ctrl, color, position);
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
