import * as util from '../util';
import { game, status, router } from 'game';

import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

function analysisBoardOrientation(data) {
  return data.game.variant.key === 'racingKings' ? 'white' : data.player.color;
}

function poolUrl(clock) {
  return '/#pool/' + (clock.initial / 60) + '+' + clock.increment;
}

function analysisButton(ctrl) {
  var d = ctrl.data;
  var url = router.game(d, analysisBoardOrientation(d)) + '#' + ctrl.vm.ply;
  return game.replayable(d) ? h('a.button', {
    attrs: { href: url },
    hook: util.bind('click', () => {
      // force page load in case the URL is the same
      if (location.pathname === url.split('#')[0]) location.reload();
    })
  }, ctrl.trans.noarg('analysis')) : null;
}

function rematchButton(ctrl) {
  var d = ctrl.data;
  var me = d.player.offeringRematch, them = d.opponent.offeringRematch;
  return h('a.button.rematch.white', {
    class: { me, them },
    attrs: {
      title: them ? ctrl.trans.noarg('yourOpponentWantsToPlayANewGameWithYou') : (
        me ? ctrl.trans.noarg('rematchOfferSent') : '')
    },
    hook: util.bind('click', () => {
      var d = ctrl.data;
      if (d.game.rematch) location.href = router.game(d.game.rematch, d.opponent.color);
      else if (d.player.offeringRematch) {
        d.player.offeringRematch = false;
        ctrl.socket.send('rematch-no');
      }
      else if (d.opponent.onGame) {
        d.player.offeringRematch = true;
        ctrl.socket.send('rematch-yes');
      }
      else ctrl.challengeRematch();
      ctrl.redraw();
    })
  }, [
    me ? util.spinner() : ctrl.trans.noarg('rematch')
  ]);
}

export function standard(ctrl, condition, icon, hint, socketMsg, onclick): VNode {
  // disabled if condition callback is provied and is falsy
  var enabled = function() {
    return !condition || condition(ctrl.data);
  };
  return h('button.fbt.hint--bottom.' + socketMsg, {
    attrs: {
      disabled: !enabled(),
      'data-hint': ctrl.trans.noarg(hint)
    },
    hook: {
      insert: vnode => {
        (vnode.elm as HTMLElement).addEventListener('click', () => {
          if (enabled()) onclick ? onclick() : ctrl.socket.sendLoading(socketMsg, null);
        });
      }
    }
  }, [
    h('span', { attrs: {'data-icon': icon} })
  ]);
};
export function forceResign(ctrl) {
  return ctrl.forceResignable() ?  h('div.suggestion', [
    h('p', ctrl.trans.noarg('theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim')),
    h('a.button', {
      hook: util.bind('click', () => ctrl.socket.sendLoading('resign-force'))
    }, ctrl.trans.noarg('forceResignation')),
    h('a.button', {
      hook: util.bind('click', () => ctrl.socket.sendLoading('draw-force'))
    }, ctrl.trans.noarg('forceDraw'))
  ]) : null;
};
export function resignConfirm(ctrl): VNode {
  return h('div.resign_confirm', [
    h('button.fbt.no.hint--bottom', {
      attrs: { 'data-hint': ctrl.trans.noarg('cancel') },
      hook: util.bind('click', () => ctrl.resign(false))
    }, [h('span', { attrs: {'data-icon': 'L'} })]),
    h('button.fbt.yes.active.hint--bottom', {
      attrs: {'data-hint': ctrl.trans.noarg('resign') },
      hook: util.bind('click', () => ctrl.resign(true))
    }, [h('span', { attrs: { 'data-icon': 'b'} })])
  ]);
};
export function threefoldClaimDraw(ctrl) {
  return ctrl.data.game.threefold ? h('div.suggestion', [
    h('p', ctrl.trans('threefoldRepetition')),
    h('a.button', {
      hook: util.bind('click', () => ctrl.socket.sendLoading('draw-claim'))
    }, ctrl.trans.noarg('claimADraw'))
  ]) : null;
};
export function cancelDrawOffer(ctrl) {
  return ctrl.data.player.offeringDraw ? h('div.pending', [
    h('p', ctrl.trans.noarg('drawOfferSent'))
  ]) : null;
};
export function answerOpponentDrawOffer(ctrl) {
  return ctrl.data.opponent.offeringDraw ? h('div.negotiation', [
    h('p', ctrl.trans.noarg('yourOpponentOffersADraw')),
    h('a.accept', {
      hook: util.bind('click', () => ctrl.socket.sendLoading('draw-yes')),
      attrs: {
        'data-icon': 'E',
        title: ctrl.trans.noarg('accept')
      }
    }),
    h('a.decline', {
      attrs: {
        'data-icon': 'L',
        title: ctrl.trans.noarg('decline')
      },
      hook: util.bind('click', () => ctrl.socket.sendLoading('draw-no'))
    })
  ]) : null;
};
export function cancelTakebackProposition(ctrl) {
  return ctrl.data.player.proposingTakeback ? h('div.pending', [
    h('p', ctrl.trans.noarg('takebackPropositionSent')),
    h('a.button', {
      hook: util.bind('click', () => ctrl.socket.sendLoading('takeback-no'))
    }, ctrl.trans.noarg('cancel'))
  ]) : null;
};
export function answerOpponentTakebackProposition(ctrl) {
  return ctrl.data.opponent.proposingTakeback ? h('div.negotiation', [
    h('p', ctrl.trans.noarg('yourOpponentProposesATakeback')),
    h('a.accept', {
      attrs: {
        'data-icon': 'E',
        title: ctrl.trans.noarg('accept')
      },
      hook: util.bind('click', ctrl.takebackYes)
    }),
    h('a.decline', {
      attrs: {
        'data-icon': 'L',
        title: ctrl.trans.noarg('decline')
      },
      hook: util.bind('click', () => ctrl.socket.sendLoading('takeback-no'))
    })
  ]) : null;
};
export function submitMove(ctrl): VNode | undefined {
  return (ctrl.vm.moveToSubmit || ctrl.vm.dropToSubmit) ? h('div.negotiation', [
    h('p', ctrl.trans.noarg('moveConfirmation')),
    h('a.accept', {
      attrs: {
        'data-icon': 'E',
        title: 'Submit move'
      },
      hook: util.bind('click', () => ctrl.submitMove(true))
    }),
    h('a.decline', {
      attrs: {
        'data-icon': 'L',
        title: ctrl.trans.noarg('cancel')
      },
      hook: util.bind('click', () => ctrl.submitMove(false))
    })
  ]) : undefined;
};
export function backToTournament(ctrl): VNode | undefined {
  var d = ctrl.data;
  return (d.tournament && d.tournament.running) ? h('div.follow_up', [
    h('a.text.fbt.strong.glowed', {
      attrs: {
        'data-icon': 'G',
        href: '/tournament/' + d.tournament.id
      },
      hook: util.bind('click', ctrl.setRedirecting)
    }, ctrl.trans.noarg('backToTournament')),
    h('form', {
      attrs: {
        method: 'post',
        action: '/tournament/' + d.tournament.id + '/withdraw'
      }
    }, [
      h('button.text.button.weak', { attrs: {'data-icon': 'Z'} }, 'Pause')
    ]),
    analysisButton(ctrl)
  ]) : undefined;
};
export function moretime(ctrl) {
  return game.moretimeable(ctrl.data) ? h('a.moretime.hint--bottom-left', {
    attrs: { 'data-hint': ctrl.trans('giveNbSeconds', ctrl.data.clock.moretime) },
    hook: util.bind('click', ctrl.socket.moreTime)
  }, [
    h('span', { attrs: {'data-icon': 'O'}})
  ]) : null;
};
export function followUp(ctrl): VNode {
  var d = ctrl.data;
  var rematchable = !d.game.rematch && (status.finished(d) || status.aborted(d)) && !d.tournament && !d.simul && !d.game.boosted && (d.opponent.onGame || (!d.clock && d.player.user && d.opponent.user));
  var newable = (status.finished(d) || status.aborted(d)) && (
    d.game.source === 'lobby' ||
      d.game.source === 'pool');
  return h('div.follow_up', [
    ctrl.vm.challengeRematched ? h('div.suggestion.text', {
      attrs: {'data-icon': 'j' }
    }, ctrl.trans.noarg('rematchOfferSent')
    ) : (rematchable || d.game.rematch ? rematchButton(ctrl) : null),
    d.tournament ? h('a.button', {
      attrs: {href: '/tournament/' + d.tournament.id}
    }, ctrl.trans.noarg('viewTournament')) : null,
    newable ? h('a.button', {
      attrs: {href: d.game.source === 'pool' ? poolUrl(d.clock) : '/?hook_like=' + d.game.id },
    }, ctrl.trans.noarg('newOpponent')) : null,
    analysisButton(ctrl)
  ]);
};
export function watcherFollowUp(ctrl): VNode {
  var d = ctrl.data;
  return h('div.follow_up', [
    d.game.rematch ? h('a.button.text', {
      attrs: {
        'data-icon': 'v',
        href: router.game(d.game.rematch, d.opponent.color)
      }
    }, ctrl.trans.noarg('viewRematch')) : null,
    d.tournament ? h('a.button', {
      attrs: {href: '/tournament/' + d.tournament.id}
    }, ctrl.trans.noarg('viewTournament')) : null,
    analysisButton(ctrl)
  ]);
};
