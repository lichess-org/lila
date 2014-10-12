var chessground = require('chessground');
var round = require('../round');
var partial = chessground.util.partial;

module.exports = {
  standard: function(ctrl, condition, icon, hint, socketMsg) {
    return condition(ctrl.data) ? m('button', {
      class: 'button hint--bottom',
      'data-hint': ctrl.trans(hint),
      onclick: partial(ctrl.socket.send, socketMsg, null)
    }, m('span[data-icon=' + icon + ']')) : null;
  },
  forceResign: function(ctrl) {
    if (!ctrl.data.opponent.ai && !ctrl.data.opponent.onGame && round.resignable(ctrl.data))
      return m('div.force_resign_zone', [
        ctrl.trans('theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim'),
        m('br'),
        m('a.button', {
          onclick: partial(ctrl.socket.send, 'resign-force'),
        }, ctrl.trans('forceResignation')),
        m('a.button', {
          onclick: partial(ctrl.socket.send, 'draw-force'),
        }, ctrl.trans('forceDraw'))
      ]);
  },
  threefoldClaimDraw: function(ctrl) {
    if (ctrl.data.game.threefold) return m('div#claim_draw_zone', [
      ctrl.trans('threefoldRepetition'),
      m.trust('&nbsp;'),
      m('a.button', {
        onclick: partial(ctrl.socket.send, 'draw-claim', null)
      }, ctrl.trans('claimADraw'))
    ]);
  },
  cancelDrawOffer: function(ctrl) {
    if (ctrl.data.player.offeringDraw) return m('div.negotiation', [
      ctrl.trans('drawOfferSent') + ' ',
      m('a', {
        onclick: partial(ctrl.socket.send, 'draw-no', null)
      }, ctrl.trans('cancel'))
    ]);
  },
  answerOpponentDrawOffer: function(ctrl) {
    if (ctrl.data.opponent.offeringDraw) return m('div.negotiation', [
      ctrl.trans('yourOpponentOffersADraw'),
      m('br'),
      m('a.button[data-icon=E]', {
        onclick: partial(ctrl.socket.send, 'draw-yes', null)
      }, ctrl.trans('accept')),
      m.trust('&nbsp;'),
      m('a.button[data-icon=L]', {
        onclick: partial(ctrl.socket.send, 'draw-no', null)
      }, ctrl.trans('decline')),
    ]);
  },
  cancelTakebackProposition: function(ctrl) {
    if (ctrl.data.player.proposingTakeback) return m('div.negotiation', [
      ctrl.trans('takebackPropositionSent') + ' ',
      m('a', {
        onclick: partial(ctrl.socket.send, 'takeback-no', null)
      }, ctrl.trans('cancel'))
    ]);
  },
  answerOpponentTakebackProposition: function(ctrl) {
    if (ctrl.data.opponent.proposingTakeback) return m('div.negotiation', [
      ctrl.trans('yourOpponentProposesATakeback'),
      m('br'),
      m('a.button[data-icon=E]', {
        onclick: partial(ctrl.socket.send, 'takeback-yes', null)
      }, ctrl.trans('accept')),
      m.trust('&nbsp;'),
      m('a.button[data-icon=L]', {
        onclick: partial(ctrl.socket.send, 'takeback-no', null)
      }, ctrl.trans('decline')),
    ]);
  },
  rematch: function(ctrl) {
    return m('a.rematch.offer.button.hint--bottom', {
      'data-hint': ctrl.trans('playWithTheSameOpponentAgain'),
      onclick: partial(ctrl.socket.send, 'rematch-yes', null)
    }, ctrl.trans('rematch'));
  },
  answerOpponentRematch: function(ctrl) {
    if (ctrl.data.opponent.offeringRematch) return m('div.lichess_play_again_join.rematch_alert', [
      ctrl.trans('yourOpponentWantsToPlayANewGameWithYou'),
      m('a.glowing.button.lichess_play_again.rematch.hint--bottom', {
        'data-hint': ctrl.trans('playWithTheSameOpponentAgain'),
        onclick: partial(ctrl.socket.send, 'rematch-yes', null),
      }, ctrl.trans('joinTheGame')),
      m('a', {
        onclick: partial(ctrl.socket.send, 'rematch-no', null),
      }, ctrl.trans('declineInvitation'))
    ]);
  },
  cancelRematch: function(ctrl) {
    if (ctrl.data.player.offeringRematch) return m('div.lichess_play_again_join.rematch_wait', [
      ctrl.trans('rematchOfferSent'),
      m('br'),
      ctrl.trans('waitingForOpponent'),
      m('br'), m('br'),
      m('a.rematch_cancel', {
        onclick: partial(ctrl.socket.send, 'rematch-no', null),
      }, ctrl.trans('cancelRematchOffer'))
    ]);
  },
  newGame: function(ctrl) {
    if (!ctrl.data.offeringRematch) return m('a.lichess_new_game.button.hint--bottom', {
      'data-hint': ctrl.trans('playWithAnotherOpponent'),
      href: ctrl.router.Lobby.home().url
    }, ctrl.trans('newOpponent'));
  },
  backToTournament: function(ctrl) {
    if (ctrl.data.tournament) return m('a[data-icon=G].button' + (ctrl.data.tournament.running ? '.strong' : ''), {
      href: ctrl.router.Tournament.show(ctrl.data.tournament.id).url
    }, ctrl.trans('backToTournament'));
  },
  moretime: function(ctrl) {
    if (round.moretimeable(ctrl.data)) return m('a.moretime.hint--bottom-left', {
      'data-hint': ctrl.trans('giveNbSeconds', ctrl.data.clock.moretime),
      onclick: partial(ctrl.socket.send, 'moretime', null)
    }, m('span[data-icon=O]'));
  }
};
