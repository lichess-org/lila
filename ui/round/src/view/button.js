var chessground = require('chessground');
var game = require('game').game;
var status = require('game').status;
var partial = chessground.util.partial;
var throttle = require('lodash-node/modern/functions/throttle');
var m = require('mithril');

module.exports = {
  standard: function(ctrl, condition, icon, hint, socketMsg) {
    return condition(ctrl.data) ? m('button', {
      class: 'button hint--bottom ' + socketMsg,
      'data-hint': ctrl.trans(hint),
      onclick: partial(ctrl.socket.send, socketMsg, null)
    }, m('span[data-icon=' + icon + ']')) : null;
  },
  forceResign: function(ctrl) {
    if (!ctrl.data.opponent.ai && ctrl.data.clock && ctrl.data.opponent.isGone && game.resignable(ctrl.data))
      return m('div.force_resign_zone', [
        ctrl.trans('theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim'),
        m('br'),
        m('a.button', {
          onclick: partial(ctrl.socket.send, 'resign-force', null),
        }, ctrl.trans('forceResignation')),
        m('a.button', {
          onclick: partial(ctrl.socket.send, 'draw-force', null),
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
      m('a.button.text[data-icon=E]', {
        onclick: partial(ctrl.socket.send, 'draw-yes', null)
      }, ctrl.trans('accept')),
      m.trust('&nbsp;'),
      m('a.button.text[data-icon=L]', {
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
      m('a.button.text[data-icon=E]', {
        onclick: partial(ctrl.socket.send, 'takeback-yes', null)
      }, ctrl.trans('accept')),
      m.trust('&nbsp;'),
      m('a.button.text[data-icon=L]', {
        onclick: partial(ctrl.socket.send, 'takeback-no', null)
      }, ctrl.trans('decline')),
    ]);
  },
  rematch: function(ctrl) {
    if ((status.finished(ctrl.data) || status.aborted(ctrl.data)) && !ctrl.data.tournament) {
      if (ctrl.data.opponent.onGame || ctrl.data.game.perf === 'correspondence') {
        return m('a.button.hint--bottom', {
          'data-hint': ctrl.trans('playWithTheSameOpponentAgain'),
          onclick: partial(ctrl.socket.send, 'rematch-yes', null)
        }, ctrl.trans('rematch'));
      } else {
        return m('a.button.disabled', ctrl.trans('rematch'));
      }
    }

  },
  answerOpponentRematch: function(ctrl) {
    if (ctrl.data.opponent.offeringRematch) return [
      ctrl.trans('yourOpponentWantsToPlayANewGameWithYou'),
      m('a.glowing.button.fat.hint--bottom', {
        'data-hint': ctrl.trans('playWithTheSameOpponentAgain'),
        onclick: partial(ctrl.socket.send, 'rematch-yes', null),
      }, ctrl.trans('joinTheGame')),
      m('a.declineInvitation.button', {
        onclick: partial(ctrl.socket.send, 'rematch-no', null),
      }, ctrl.trans('declineInvitation'))
    ];
  },
  cancelRematch: function(ctrl) {
    if (ctrl.data.player.offeringRematch) return [
      ctrl.trans('rematchOfferSent'),
      m('br'),
      ctrl.trans('waitingForOpponent'),
      m('a.button', {
        onclick: partial(ctrl.socket.send, 'rematch-no', null),
      }, ctrl.trans('cancelRematchOffer'))
    ];
  },
  viewRematch: function(ctrl) {
    if (ctrl.data.game.rematch) return m('a.viewRematch.button.text[data-icon=v]', {
      href: ctrl.router.Round.watcher(ctrl.data.game.rematch, ctrl.data.opponent.color).url
    }, ctrl.trans('viewRematch'));
  },
  joinRematch: function(ctrl) {
    if (ctrl.data.game.rematch) return [
      ctrl.trans('rematchOfferAccepted'),
      m('a.button.fat.hint--bottom', {
        'data-hint': ctrl.trans('playWithTheSameOpponentAgain'),
        href: ctrl.router.Round.watcher(ctrl.data.game.rematch, ctrl.data.opponent.color).url
      }, ctrl.trans('joinTheGame'))
    ];
  },
  backToTournament: function(ctrl) {
    if (ctrl.data.tournament && ctrl.data.tournament.running) return m('a.text[data-icon=G].button.strong.glowing', {
      href: '/tournament/' + ctrl.data.tournament.id
    }, ctrl.trans('backToTournament'));
  },
  viewTournament: function(ctrl) {
    if (ctrl.data.tournament) return m('a.viewTournament.button', {
      href: '/tournament/' + ctrl.data.tournament.id
    }, ctrl.trans('viewTournament'));
  },
  moretime: function(ctrl) {
    if (game.moretimeable(ctrl.data)) return m('a.moretime.hint--bottom-left', {
      'data-hint': ctrl.trans('giveNbSeconds', ctrl.data.clock.moretime),
      onclick: throttle(partial(ctrl.socket.send, 'moretime', null), 600)
    }, m('span[data-icon=O]'));
  },
  analysis: function(ctrl) {
    if (game.replayable(ctrl.data)) return m('a.button.replay_and_analyse', {
      onclick: partial(ctrl.socket.send, 'rematch-no', null),
      href: ctrl.router.Round.watcher(ctrl.data.game.id, ctrl.data.player.color).url
    }, ctrl.trans('analysis'));
  }
};
