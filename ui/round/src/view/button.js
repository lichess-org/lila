var chessground = require('chessground');
var classSet = chessground.util.classSet;
var util = require('../util');
var game = require('game').game;
var status = require('game').status;
var partial = chessground.util.partial;
var router = require('game').router;
var m = require('mithril');
var vn = require('mithril/render/vnode');

function analysisBoardOrientation(data) {
  if (data.game.variant.key === 'racingKings') {
    return 'white';
  } else {
    return data.player.color;
  }
}

module.exports = {
  standard: function(ctrl, condition, icon, hint, socketMsg, onclick) {
    // disabled if condition callback is provided and is falsy
    var enabled = function() {
      return !condition || condition(ctrl.data);
    };
    return vn('button', socketMsg || 'click', {
      class: 'fbt hint--bottom ' + socketMsg,
      disabled: !enabled(),
      'data-hint': ctrl.trans.noarg(hint),
      onclick: function() {
        if (enabled()) onclick ? onclick() : ctrl.socket.sendLoading(socketMsg, null);
      }
    }, [vn('span', undefined, {
      'data-icon': icon
    })]);
  },
  forceResign: function(ctrl) {
    if (ctrl.forceResignable())
      return m('div.suggestion', [
        m('p', ctrl.trans.noarg('theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim')),
        m('a.button', {
          onclick: partial(ctrl.socket.sendLoading, 'resign-force', null),
        }, ctrl.trans.noarg('forceResignation')),
        m('a.button', {
          onclick: partial(ctrl.socket.sendLoading, 'draw-force', null),
        }, ctrl.trans.noarg('forceDraw'))
      ]);
  },
  resignConfirm: function(ctrl) {
    return m('div.resign_confirm', [
      m('button.fbt.no.hint--bottom', {
        'data-hint': ctrl.trans.noarg('cancel'),
        onclick: partial(ctrl.resign, false)
      }, m('span', {
        'data-icon': 'L'
      })),
      m('button.fbt.yes.active.hint--bottom', {
        'data-hint': ctrl.trans.noarg('resign'),
        onclick: partial(ctrl.resign, true)
      }, m('span', {
        'data-icon': 'b'
      }))
    ]);
  },
  threefoldClaimDraw: function(ctrl) {
    if (ctrl.data.game.threefold) return m('div.suggestion', [
      m('p', ctrl.trans.noarg('threefoldRepetition')),
      m('a.button', {
        onclick: partial(ctrl.socket.sendLoading, 'draw-claim', null)
      }, ctrl.trans.noarg('claimADraw'))
    ]);
  },
  cancelDrawOffer: function(ctrl) {
    if (ctrl.data.player.offeringDraw) return m('div.pending', [
      m('p', ctrl.trans.noarg('drawOfferSent'))
    ]);
  },
  answerOpponentDrawOffer: function(ctrl) {
    if (ctrl.data.opponent.offeringDraw) return m('div.negotiation', [
      m('p', ctrl.trans.noarg('yourOpponentOffersADraw')),
      m('a.accept[data-icon=E]', {
        onclick: partial(ctrl.socket.sendLoading, 'draw-yes', null),
        title: ctrl.trans.noarg('accept')
      }),
      m('a.decline[data-icon=L]', {
        onclick: partial(ctrl.socket.sendLoading, 'draw-no', null),
        title: ctrl.trans.noarg('decline')
      }),
    ]);
  },
  cancelTakebackProposition: function(ctrl) {
    if (ctrl.data.player.proposingTakeback) return m('div.pending', [
      m('p', ctrl.trans.noarg('takebackPropositionSent')),
      m('a.button', {
        onclick: partial(ctrl.socket.sendLoading, 'takeback-no', null)
      }, ctrl.trans.noarg('cancel'))
    ]);
  },
  answerOpponentTakebackProposition: function(ctrl) {
    if (ctrl.data.opponent.proposingTakeback) return m('div.negotiation', [
      m('p', ctrl.trans.noarg('yourOpponentProposesATakeback')),
      m('a.accept[data-icon=E]', {
        onclick: partial(ctrl.takebackYes),
        title: ctrl.trans.noarg('accept')
      }),
      m('a.decline[data-icon=L]', {
        onclick: partial(ctrl.socket.sendLoading, 'takeback-no', null),
        title: ctrl.trans.noarg('decline')
      })
    ]);
  },
  submitMove: function(ctrl) {
    if (ctrl.vm.moveToSubmit || ctrl.vm.dropToSubmit) return m('div.negotiation', [
      m('p', ctrl.trans.noarg('moveConfirmation')),
      m('a.accept[data-icon=E]', {
        onclick: partial(ctrl.submitMove, true),
        title: 'Submit move'
      }),
      m('a.decline[data-icon=L]', {
        onclick: partial(ctrl.submitMove, false),
        title: ctrl.trans.noarg('cancel')
      })
    ]);
  },
  answerOpponentRematch: function(ctrl) {
    if (ctrl.data.opponent.offeringRematch) return m('div.negotiation', [
      m('p', ctrl.trans.noarg('yourOpponentWantsToPlayANewGameWithYou')),
      m('a.accept[data-icon=E]', {
        title: ctrl.trans.noarg('joinTheGame'),
        onclick: partial(ctrl.socket.sendLoading, 'rematch-yes', null)
      }),
      m('a.decline[data-icon=L]', {
        title: ctrl.trans.noarg('decline'),
        onclick: partial(ctrl.socket.sendLoading, 'rematch-no', null)
      })
    ]);
  },
  cancelRematch: function(ctrl) {
    if (ctrl.data.player.offeringRematch) return m('div.pending', [
      m('p', ctrl.trans.noarg('rematchOfferSent')),
      m('a.button', {
        onclick: partial(ctrl.socket.sendLoading, 'rematch-no', null)
      }, ctrl.trans.noarg('cancel'))
    ]);
  },
  backToTournament: function(ctrl) {
    var d = ctrl.data;
    if (d.tournament && d.tournament.running) return m('div.follow_up', [
      m('a', {
        'data-icon': 'G',
        class: 'text fbt strong glowed',
        href: '/tournament/' + d.tournament.id,
        onclick: ctrl.setRedirecting
      }, ctrl.trans.noarg('backToTournament')),
      m('form', {
        method: 'post',
        action: '/tournament/' + d.tournament.id + '/withdraw'
      }, m('button.text.button[data-icon=b]', ctrl.trans.noarg('withdraw'))),
      game.replayable(d) ? m('a.button', {
        href: router.game(d, analysisBoardOrientation(d)) + (ctrl.replaying() ? '#' + ctrl.vm.ply : '')
      }, ctrl.trans.noarg('analysis')) : null
    ]);
  },
  moretime: function(ctrl) {
    if (game.moretimeable(ctrl.data)) return vn('a', 'moretime', {
      class: 'moretime hint--bottom-left',
      'data-hint': ctrl.trans('giveNbSeconds', ctrl.data.clock.moretime),
      onclick: ctrl.socket.moreTime
    }, [vn('span', undefined, {
      'data-icon': 'O'
    })]);
  },
  followUp: function(ctrl) {
    var d = ctrl.data;
    var rematchable = !d.game.rematch && (status.finished(d) || status.aborted(d)) && !d.tournament && !d.simul && !d.game.boosted && (d.opponent.onGame || (!d.clock && d.player.user && d.opponent.user));
    var newable = (status.finished(d) || status.aborted(d)) && d.game.source == 'lobby';
    return m('div.follow_up', [
      ctrl.vm.challengeRematched ? m('div.suggestion.text[data-icon=j]',
        ctrl.trans.noarg('rematchOfferSent')
      ) : (rematchable ? m('a.button', {
        onclick: function() {
          if (d.opponent.onGame) ctrl.socket.sendLoading('rematch-yes', null);
          else ctrl.challengeRematch();
        }
      }, ctrl.trans.noarg('rematch')) : null),
      ctrl.data.game.rematch ? m('a.button.hint--top', {
        'data-hint': ctrl.trans.noarg('joinTheGame'),
        href: router.game(ctrl.data.game.rematch, ctrl.data.opponent.color)
      }, ctrl.trans.noarg('rematchOfferAccepted')) : null,
      d.tournament ? m('a.button', {
        href: '/tournament/' + d.tournament.id
      }, ctrl.trans.noarg('viewTournament')) : null,
      newable ? m('a.button', {
        href: '/?hook_like=' + d.game.id,
      }, ctrl.trans.noarg('newOpponent')) : null,
      game.replayable(d) ? m('a.button', {
        href: router.game(d, analysisBoardOrientation(d)) + (ctrl.replaying() ? '#' + ctrl.vm.ply : '')
      }, ctrl.trans.noarg('analysis')) : null
    ]);
  },
  watcherFollowUp: function(ctrl) {
    var d = ctrl.data;
    return m('div.follow_up', [
      d.game.rematch ? m('a.button.text[data-icon=v]', {
        href: router.game(d.game.rematch, d.opponent.color)
      }, ctrl.trans.noarg('viewRematch')) : null,
      d.tournament ? m('a.button', {
        href: '/tournament/' + d.tournament.id
      }, ctrl.trans.noarg('viewTournament')) : null,
      game.replayable(d) ? m('a.button', {
        href: router.game(d, analysisBoardOrientation(d)) + (ctrl.replaying() ? '#' + ctrl.vm.ply : '')
      }, ctrl.trans.noarg('analysis')) : null
    ]);
  }
};
