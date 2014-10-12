var m = require('mithril');
var chessground = require('chessground');
var round = require('../round');
var status = require('../status');
var opposite = chessground.util.opposite;
var classSet = chessground.util.classSet;
var partial = chessground.util.partial;
var renderClock = require('../clock/view');
var renderStatus = require('./status');

function renderPlayer(ctrl, player) {
  return player.ai ? m('div.username.connected.statused', [
    ctrl.trans('aiNameLevelAiLevel', 'Stockfish', player.ai),
    m('span.status')
  ]) : m('div', {
      class: 'username ' + player.color + ' ' + classSet({
        'statused': player.statused,
        'connected': player.connected,
        'offline': !player.connected
      })
    },
    player.user ? [
      m('a', {
        class: 'user_link ulpt',
        href: ctrl.router.User.show(player.user.username).url,
        target: round.playable(ctrl.data) ? '_blank' : null,
        'data-icon': 'r',
      }, [
        (player.user.title ? player.user.title + ' ' : '') + player.user.username,
        player.engine ? m('span[data-icon=j]', {
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
  return winner ? m('div.player.' + winner.color, [
      m('div.no-square', m('div.cg-piece.king.' + winner.color)),
      m('p', [
        renderStatus(ctrl),
        m('br'),
        ctrl.trans(winner.color == 'white' ? 'whiteIsVictorious' : 'blackIsVictorious')
      ])
    ]) :
    m('div.player', m('p', renderStatus(ctrl)));
}

function renderRematchButton(ctrl) {
  return m('a.rematch.offer.button.hint--bottom', {
    'data-hint': ctrl.trans('playWithTheSameOpponentAgain'),
    onclick: partial(ctrl.socket.send, 'rematch-yes', null)
  }, ctrl.trans('rematch'));
}

function renderTableEnd(ctrl) {
  var d = ctrl.data;
  return [
    m('div.current_player', renderResult(ctrl)),
    m('div.control.buttons', [
      d.game.pool ? [
        m('a.button[data-icon=,]', {
          href: router.Pool.show(d.game.pool.id).url,
        }, 'Return to pool'),
        m('form[method=post]', {
          action: router.Pool.leave(d.game.pool.id).url
        }, m('button.button[type=submit]', 'Leave the pool'))
      ] : (d.tournament ? m('a.button' + (d.tournament.running ? '.strong' : '') + '[data-icon=G]', {
        href: ctrl.router.Tournament.show(d.tournament.id).url
      }, ctrl.trans('backToTournament')) : (d.opponent.ai ? renderRematchButton(ctrl) : [
        m('div.separator'),
        d.opponent.isOfferingRematch ? m('div.lichess_play_again_join.rematch_alert', [
          ctrl.trans('yourOpponentWantsToPlayANewGameWithYou'),
          m('a.glowing.button.lichess_play_again.rematch.hint--bottom', {
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
          m('a.rematch_cancel', {
            onclick: partial(ctrl.socket.send, 'rematch-no', null),
          }, ctrl.trans('cancelRematchOffer'))
        ]) : renderRematchButton(ctrl))
      ])), !d.opponent.isOfferingRematch ? m('a.lichess_new_game.button.hint--bottom', {
        'data-hint': ctrl.trans('playWithAnotherOpponent'),
        href: ctrl.router.Lobby.home().url
      }, ctrl.trans('newOpponent')) : null
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

function renderTableWatch(ctrl) {
  var d = ctrl.data;
  return [
    m('div.current_player', (status.finished(d) || status.aborted(d)) ? renderResult(ctrl) : (
      m('div.player', [
        m('div.no-square', m('div.cg-piece.king.' + d.game.player)),
        m('p', ctrl.trans(d.game.player == 'white' ? 'whitePlays' : 'blackPlays'))
      ]))),
    m('div.separator'),
    renderPlayer(ctrl, d.player),
    m('div.control.buttons', [
      d.game.rematch ? m('a.button[data-icon=v]', {
        href: ctrl.router.Round.watcher(d.game.rematch, d.opponent.color).url
      }, ctrl.trans('viewRematch')) : null,
      d.tournament ? m('a.button', {
        href: ctrl.Tournament.show(d.tournament.id)
      }, ctrl.trans('viewTournament')) : null
    ])
  ];
}

function renderTablePlay(ctrl) {
  var d = ctrl.data;
  return [
    m('div.current_player',
      m('div.player', [
        m('div.no-square', m('div.cg-piece.king.' + d.game.player)),
        m('p', ctrl.trans(d.game.player == d.player.color ? 'yourTurn' : 'waiting'))
      ])
    ),
    m('div.control.icons', [
      renderButton(ctrl, round.abortable, 'L', 'abortGame', 'abort'),
      renderButton(ctrl, round.takebackable, 'i', 'proposeATakeback', 'takeback-yes'),
      renderButton(ctrl, round.drawable, '2', 'offerDraw', 'draw-yes'),
      renderButton(ctrl, round.resignable, 'b', 'resign', 'resign')
    ]),
    d.game.threefold ? m('div#claim_draw_zone', [
      ctrl.trans('threefoldRepetition'),
      m.trust('&nbsp;'),
      m('a.button', {
        onclick: partial(ctrl.socket.send, 'draw-claim', null)
      }, ctrl.trans('claimADraw'))
    ]) : (
      d.player.isOfferingDraw ? m('div.negotiation', [
        ctrl.trans('drawOfferSent') + ' ',
        m('a', {
          onclick: partial(ctrl.socket.send, 'draw-no', null)
        }, ctrl.trans('cancel'))
      ]) : null,
      d.opponent.isOfferingDraw ? m('div.negotiation', [
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
    d.player.isProposingTakeback ? m('div.negotiation', [
      ctrl.trans('takebackPropositionSent') + ' ',
      m('a', {
        onclick: partial(ctrl.socket.send, 'takeback-no', null)
      }, ctrl.trans('cancel'))
    ]) : null,
    d.opponent.isProposingTakeback ? m('div.negotiation', [
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
      round.moretimeable(ctrl.data) ? m('a.moretime.hint--bottom-left', {
        'data-hint': ctrl.trans('giveNbSeconds', ctrl.data.clock.moretime),
        onclick: partial(ctrl.socket.send, 'moretime', null)
      }, m('span[data-icon=O]')) : null
    ] : null
  ])
}
