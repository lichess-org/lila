var chessground = require('chessground');
var classSet = chessground.util.classSet;
var game = require('game').game;
var status = require('game').status;
var partial = chessground.util.partial;
var router = require('game').router;
var m = require('mithril');

function analysisBoardOrientation(data) {
  if (data.game.variant.key === 'racingKings') {
    return 'white';
  } else {
    return data.player.color;
  }
}

module.exports = {
  standard: function(ctrl, condition, icon, hint, socketMsg, onclick) {
    // disabled if condition callback is provied and is falsy
    var enabled = !condition || condition(ctrl.data);
    return m('button', {
      class: 'button hint--bottom ' + socketMsg + classSet({
        ' disabled': !enabled
      }),
      'data-hint': ctrl.trans(hint),
      onclick: enabled ? onclick || partial(ctrl.socket.send, socketMsg, null) : null
    }, m('span', {
      'data-icon': icon
    }));
  },
  forceResign: function(ctrl) {
    if (ctrl.forceResignable())
      return m('div.suggestion', [
        m('p', ctrl.trans('theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim')),
        m('a.button', {
          onclick: partial(ctrl.socket.send, 'resign-force', null),
        }, ctrl.trans('forceResignation')),
        m('a.button', {
          onclick: partial(ctrl.socket.send, 'draw-force', null),
        }, ctrl.trans('forceDraw'))
      ]);
  },
  resignConfirm: function(ctrl) {
    return m('div.resign_confirm', [
      m('button.button.yes.active.hint--bottom', {
        'data-hint': ctrl.trans('resign'),
        onclick: partial(ctrl.resign, true)
      }, m('span', {
        'data-icon': 'b'
      })), m('button.button.no.hint--bottom', {
        'data-hint': ctrl.trans('cancel'),
        onclick: partial(ctrl.resign, false)
      }, m('span', {
        'data-icon': 'L'
      }))
    ]);
  },
  threefoldClaimDraw: function(ctrl) {
    if (ctrl.data.game.threefold) return m('div.suggestion', [
      m('p', ctrl.trans('threefoldRepetition')),
      m('a.button', {
        onclick: partial(ctrl.socket.send, 'draw-claim', null)
      }, ctrl.trans('claimADraw'))
    ]);
  },
  cancelDrawOffer: function(ctrl) {
    if (ctrl.data.player.offeringDraw) return m('div.pending', [
      m('p', ctrl.trans('drawOfferSent')),
      m('a.button', {
        onclick: partial(ctrl.socket.send, 'draw-no', null)
      }, ctrl.trans('cancel'))
    ]);
  },
  answerOpponentDrawOffer: function(ctrl) {
    if (ctrl.data.opponent.offeringDraw) return m('div.negotiation', [
      m('p', ctrl.trans('yourOpponentOffersADraw')),
      m('a.accept[data-icon=E]', {
        onclick: partial(ctrl.socket.send, 'draw-yes', null),
        title: ctrl.trans('accept')
      }),
      m('a.decline[data-icon=L]', {
        onclick: partial(ctrl.socket.send, 'draw-no', null),
        title: ctrl.trans('decline')
      }),
    ]);
  },
  cancelTakebackProposition: function(ctrl) {
    if (ctrl.data.player.proposingTakeback) return m('div.pending', [
      m('p', ctrl.trans('takebackPropositionSent')),
      m('a.button', {
        onclick: partial(ctrl.socket.send, 'takeback-no', null)
      }, ctrl.trans('cancel'))
    ]);
  },
  answerOpponentTakebackProposition: function(ctrl) {
    if (ctrl.data.opponent.proposingTakeback) return m('div.negotiation', [
      m('p', ctrl.trans('yourOpponentProposesATakeback')),
      m('a.accept[data-icon=E]', {
        onclick: partial(ctrl.takebackYes),
        title: ctrl.trans('accept')
      }),
      m('a.decline[data-icon=L]', {
        onclick: partial(ctrl.socket.send, 'takeback-no', null),
        title: ctrl.trans('decline')
      })
    ]);
  },
  submitMove: function(ctrl) {
    if (ctrl.vm.moveToSubmit || ctrl.vm.dropToSubmit) return m('div.negotiation', [
      m('p', ctrl.trans('moveConfirmation')),
      m('a.accept[data-icon=E]', {
        onclick: partial(ctrl.submitMove, true),
        title: 'Submit move'
      }),
      m('a.decline[data-icon=L]', {
        onclick: partial(ctrl.submitMove, false),
        title: ctrl.trans('cancel')
      })
    ]);
  },
  feedback: function(ctrl) {
    if (ctrl.vm.buttonFeedback) return m.trust(lichess.spinnerHtml);
  },
  challengeRematched: function(ctrl) {
    if (ctrl.vm.challengeRematched) return m('div.suggestion',
      ctrl.trans('rematchOfferSent')
    );
  },
  answerOpponentRematch: function(ctrl) {
    if (ctrl.data.opponent.offeringRematch) return m('div.negotiation', [
      m('p', ctrl.trans('yourOpponentWantsToPlayANewGameWithYou')),
      m('a.accept[data-icon=E]', {
        title: ctrl.trans('joinTheGame'),
        onclick: partial(ctrl.socket.send, 'rematch-yes', null)
      }),
      m('a.decline[data-icon=L]', {
        title: ctrl.trans('decline'),
        onclick: partial(ctrl.socket.send, 'rematch-no', null)
      })
    ]);
  },
  cancelRematch: function(ctrl) {
    if (ctrl.data.player.offeringRematch) return m('div.pending', [
      m('p', ctrl.trans('rematchOfferSent')),
      m('a.button', {
        onclick: partial(ctrl.socket.send, 'rematch-no', null),
      }, ctrl.trans('cancel'))
    ]);
  },
  joinRematch: function(ctrl) {
    if (ctrl.data.game.rematch) return m('div.suggestion', [
      m('p', ctrl.trans('rematchOfferAccepted')),
      m('a.button.glowed.hint--bottom', {
        href: router.game(ctrl.data.game.rematch, ctrl.data.opponent.color)
      }, ctrl.trans('joinTheGame'))
    ]);
  },
  backToTournament: function(ctrl) {
    if (ctrl.data.tournament) return m('div.follow_up', [
      m('a', {
        'data-icon': 'G',
        class: 'text button strong' + (ctrl.data.tournament.running ? ' glowed' : ''),
        href: '/tournament/' + ctrl.data.tournament.id,
        onclick: ctrl.setRedirecting
      }, ctrl.trans('backToTournament')),
      ctrl.data.tournament.running ? m('form', {
        method: 'post',
        action: '/tournament/' + ctrl.data.tournament.id + '/withdraw'
      }, m('button.text.button[data-icon=b]', ctrl.trans('withdraw'))) : null
    ]);
  },
  moretime: function(ctrl) {
    if (game.moretimeable(ctrl.data)) return m('a.moretime.hint--bottom-left', {
      'data-hint': ctrl.trans('giveNbSeconds', ctrl.data.clock.moretime),
      onclick: ctrl.socket.moreTime
    }, m('span[data-icon=O]'));
  },
  followUp: function(ctrl) {
    var hash = ctrl.replaying() ? '#' + ctrl.vm.ply : '';
    var rematchable = (status.finished(ctrl.data) || status.aborted(ctrl.data)) && !ctrl.data.tournament && !ctrl.data.simul && !ctrl.data.game.boosted && (ctrl.data.opponent.onGame || (!ctrl.data.game.clock && ctrl.data.player.user && ctrl.data.opponent.user));
    var newable = (status.finished(ctrl.data) || status.aborted(ctrl.data)) && ctrl.data.game.source == 'lobby';
    return m('div.follow_up', [
      rematchable ? m('a.button', {
        onclick: function() {
          if (ctrl.data.opponent.onGame) ctrl.socket.send('rematch-yes', null);
          else ctrl.challengeRematch();
        }
      }, ctrl.trans('rematch')) : null,
      newable ? m('a.button', {
        href: '/?hook_like=' + ctrl.data.game.id,
      }, ctrl.trans('newOpponent')) : null,
      game.replayable(ctrl.data) ? m('a.button', {
        onclick: partial(ctrl.socket.send, 'rematch-no', null),
        href: router.game(ctrl.data, analysisBoardOrientation(ctrl.data)) + hash
      }, ctrl.trans('analysis')) : null
    ]);
  },
  watcherFollowUp: function(ctrl) {
    var hash = ctrl.replaying() ? '#' + ctrl.vm.ply : '';
    return m('div.follow_up', [
      ctrl.data.game.rematch ? m('a.button.text[data-icon=v]', {
        href: router.game(ctrl.data.game.rematch, ctrl.data.opponent.color)
      }, ctrl.trans('viewRematch')) : null,
      ctrl.data.tournament ? m('a.button', {
        href: '/tournament/' + ctrl.data.tournament.id
      }, ctrl.trans('viewTournament')) : null,
      game.replayable(ctrl.data) ? m('a.button', {
        href: router.game(ctrl.data, analysisBoardOrientation(ctrl.data)) + hash
      }, ctrl.trans('analysis')) : null
    ]);
  }
};
