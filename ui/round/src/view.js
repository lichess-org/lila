var map = require('lodash-node/modern/collections/map');
var chessground = require('chessground');
var round = require('./round');
var opposite = chessground.util.opposite;
var classSet = chessground.util.classSet;
var partial = chessground.util.partial;
var clockView = require('./clock/view');
var m = require('mithril');

function renderOpponent(ctrl) {
  var op = ctrl.data.opponent;
  return op.ai ? m('div.username.connected.statused',
    ctrl.trans('aiNameLevelAiLevel', 'Stockfish', op.ai)
  ) : m('div', {
      class: 'username ' + op.color + ' ' + classSet({
        'statused': op.statused,
        'connected': op.connected,
        'offline': !op.connected
      })
    },
    op.user ? [
      m('a', {
        class: 'user_link ulpt',
        href: ctrl.router.User.show(op.user.username).url,
        target: round.playable(ctrl.data) ? '_blank' : null,
        'data-icon': 'r',
      }, [
        (op.user.title ? op.user.title + ' ' : '') + op.user.username,
        op.engine ? m('span[data-icon=j]', {
          title: ctrl.trans('thisPlayerUsesChessComputerAssistance')
        }) : null
      ]),
      m('span.status')
    ] : m('span.user_link', [
      'Anonymous',
      m('span.status')
    ])
  );
}

function renderTableEnd(ctrl) {}

function renderButton(ctrl, condition, icon, hint, socketMsg) {
  return condition(ctrl.data) ? m('button', {
    class: 'button hint--bottom',
    'data-hint': ctrl.trans(hint),
    onclick: partial(ctrl.socket.send, socketMsg, null)
  }, m('span[data-icon=' + icon + ']')) : null;
}

function renderTablePlay(ctrl) {
  var d = ctrl.data;
  return [
    m('div.lichess_current_player',
      m('div.lichess_player', [
        m('div.cg-piece.king.' + d.game.player),
        m('p', ctrl.trans(d.game.player == d.player.color ? 'yourTurn' : 'waiting'))
      ])
    ),
    m('div.lichess_control.icons', [
      renderButton(ctrl, round.abortable, 'L', 'abortGame', 'abort'),
      renderButton(ctrl, round.takebackable, 'i', 'proposeATakeback', 'takeback-yes'),
      renderButton(ctrl, round.drawable, '2', 'offerDraw', 'draw-yes'),
      renderButton(ctrl, round.resignable, 'b', 'resign', 'resign')
    ]),
    d.player.isOfferingDraw ? m('div.negociation', [
      ctrl.trans('drawOfferSent') + ' ',
      m('a', {
        onclick: partial(ctrl.socket.send, 'draw-no', null)
      }, ctrl.trans('cancel'))
    ]) : null,
    d.opponent.isOfferingDraw ? m('div.negociation', [
      ctrl.trans('yourOpponentOffersADraw'),
      m('br'),
      m('a.button[data-icon=E]', {
        onclick: partial(ctrl.socket.send, 'draw-yes', null)
      }, ctrl.trans('accept')),
      m.trust('&nbsp;'),
      m('a.button[data-icon=L]', {
        onclick: partial(ctrl.socket.send, 'draw-no', null)
      }, ctrl.trans('decline')),
    ]) : null,
    d.player.isProposingTakeback ? m('div.negociation', [
      ctrl.trans('takebackPropositionSent') + ' ',
      m('a', {
        onclick: partial(ctrl.socket.send, 'takeback-no', null)
      }, ctrl.trans('cancel'))
    ]) : null,
    d.opponent.isProposingTakeback ? m('div.negociation', [
      ctrl.trans('yourOpponentProposesATakeback'),
      m('br'),
      m('a.button[data-icon=E]', {
        onclick: partial(ctrl.socket.send, 'takeback-yes', null)
      }, ctrl.trans('accept')),
      m.trust('&nbsp;'),
      m('a.button[data-icon=L]', {
        onclick: partial(ctrl.socket.send, 'takeback-no', null)
      }, ctrl.trans('decline')),
    ]) : null,
  ];
}

module.exports = function(ctrl) {
  var clockRunningColor = ctrl.isClockRunning() ? ctrl.data.game.player : null;
  return m('div', {
    config: function(el, isUpdate, context) {
      if (isUpdate) return;
      $('body').trigger('lichess.content_loaded');
    },
    class: 'lichess_game clearfix not_spectator pov_' + ctrl.data.player.color
  }, [
    ctrl.data.blindMode ? m('div#lichess_board_blind') : null,
    m('div.lichess_board_wrap', ctrl.data.blindMode ? null : [
      m('div.lichess_board.' + ctrl.data.game.variant, chessground.view(ctrl.chessground)),
      m('div#premove_alert', ctrl.trans('premoveEnabledClickAnywhereToCancel'))
    ]),
    m('div.lichess_ground',
      m('div.lichess_table_wrap', [
        (ctrl.clock && !ctrl.data.blindMode) ? clockView(ctrl.clock, opposite(ctrl.data.player.color), "top", clockRunningColor) : null,
        m('div', {
          class: 'lichess_table onbg ' + classSet({
            'table_with_clock': ctrl.clock,
            'finished': ctrl.data.game.finished
          })
        }, [
          m('div.lichess_opponent', renderOpponent(ctrl)),
          m('div.lichess_separator'),
          m('div.table_inner',
            round.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
          )
        ]), (ctrl.clock && !ctrl.data.blindMode) ? clockView(ctrl.clock, ctrl.data.player.color, "bottom", clockRunningColor) : null,
      ])
    )
  ]);
};
