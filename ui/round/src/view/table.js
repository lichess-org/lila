var m = require('mithril');
var chessground = require('chessground');
var round = require('../round');
var status = require('../status');
var opposite = chessground.util.opposite;
var classSet = chessground.util.classSet;
var partial = chessground.util.partial;
var renderClock = require('../clock/view');
var renderStatus = require('./status');

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

function renderResult(ctrl) {
  var winner = round.getPlayer(ctrl.data, ctrl.data.game.winner);
  return winner ? m('div.lichess_player.' + winner.color, [
      m('div.cg-piece.king.' + winner.color),
      m('p', [
        renderStatus(ctrl),
        m('br'),
        ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious')
      ])
    ]) :
    m('div.lichess_player', m('p', renderStatus(ctrl)));
}

function renderRematchButton(ctrl) {
  return m('a.lichess_rematch.offer.button.hint--bottom', {
    'data-hint': ctrl.trans('playWithTheSameOpponentAgain'),
    onclick: partial(ctrl.socket.send, 'rematch-yes', null)
  }, ctrl.trans('rematch'));
}

function renderTableEnd(ctrl) {
  var d = ctrl.data;
  return [
    m('div.lichess_current_player', renderResult(ctrl)),
    m('div.lichess_control.buttons', [
      d.game.pool ? [
        m('a.button[data-icon=,]', {
          href: router.Pool.show(d.game.pool.id),
        }, 'Return to pool'),
        m('form[method=post]', {
          action: router.Pool.leave(d.game.pool.id)
        }, m('button.button[type=submit]', 'Leave the pool'))
      ] : (d.game.tournament ? m('a.button' + (d.game.tournament.running ? '.strong' : '') + '[data-icon=G]', {
        href: ctrl.router.Tournament.show(d.game.tournament.id)
      }, ctrl.trans('backToTournament')) : (d.opponent.ai ? renderRematchButton(ctrl) : [
        m('div.lichess_separator'),
        d.opponent.isOfferingRematch ? m('div.lichess_play_again_join.rematch_alert', [
          ctrl.trans('yourOpponentWantsToPlayANewGameWithYou'),
          m('a.glowing.button.lichess_play_again.lichess_rematch.hint--bottom', {
            'data-hint': ctrl.trans('playWithTheSameOpponentAgain'),
            onclick: partial(ctrl.socket.send, 'rematch-yes', null),
          }, ctrl.trans('joinTheGame')),
          m('a', {
            onclick: partial(ctrl.socket.send, 'rematch-no', null),
          }, ctrl.trans('declineInvitation'))
        ]) : (d.player.isOfferingRematch ? m('div.lichess_play_again_join.rematch_wait', [
          ctrl.trans('rematchOfferSent'),
          m('br'),
          ctrl.trans('waitingForOpponent'),
          m('br'), m('br'),
          m('a.lichess_rematch_cancel', {
            onclick: partial(ctrl.socket.send, 'rematch-no', null),
          }, ctrl.trans('cancelRematchOffer'))
        ]) : renderRematchButton(ctrl))
      ]))
    ])
  ];
}

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
    d.game.threefold ? m('div#claim_draw_zone', [
      ctrl.trans('threefoldRepetition'),
      m.trust('&nbsp;'),
      m('a.lichess_claim_draw.button', {
        onclick: partial(ctrl.socket.send, 'draw-claim', null)
      }, ctrl.trans('claimADraw'))
    ]) : (
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
      ]) : null
    ),
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
    ]) : null, (round.mandatory(d) && round.nbMoves(d, d.player.color) === 0) ? m('div[data-icon=j]',
      ctrl.trans('youHaveNbSecondsToMakeYourFirstMove')
    ) : null
  ];
}

module.exports = function(ctrl) {
  var clockRunningColor = ctrl.isClockRunning() ? ctrl.data.game.player : null;
  return m('div.lichess_table_wrap', [
    (ctrl.clock && !ctrl.data.blindMode) ? renderClock(ctrl.clock, opposite(ctrl.data.player.color), "top", clockRunningColor) : null,
    m('div', {
      class: 'lichess_table onbg ' + classSet({
        'table_with_clock': ctrl.clock,
        'finished': status.finished(ctrl.data)
      })
    }, [
      m('div.lichess_opponent', renderOpponent(ctrl)),
      m('div.lichess_separator'),
      m('div.table_inner',
        round.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
      )
    ]), (ctrl.clock && !ctrl.data.blindMode) ? renderClock(ctrl.clock, ctrl.data.player.color, "bottom", clockRunningColor) : null,
  ])
}
