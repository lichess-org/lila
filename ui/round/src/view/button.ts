import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Hooks } from 'snabbdom/hooks'
import * as util from '../util';
import { PlayerUser, game, status, router } from 'game';
import { RoundData, MaybeVNodes } from '../interfaces';
import { ClockData } from '../clock/clockCtrl';
import RoundController from '../ctrl';

function analysisBoardOrientation(data: RoundData) {
  return data.game.variant.key === 'racingKings' ? 'white' : data.player.color;
}

function poolUrl(clock: ClockData, blocking?: PlayerUser) {
  return '/#pool/' + (clock.initial / 60) + '+' + clock.increment + (blocking ? '/' + blocking.id : '');
}

function analysisButton(ctrl: RoundController): VNode | null {
  const d = ctrl.data,
    url = router.game(d, analysisBoardOrientation(d)) + '#' + ctrl.ply;
  return game.replayable(d) ? h('a.button', {
    attrs: { href: url },
    hook: util.bind('click', _ => {
      // force page load in case the URL is the same
      if (location.pathname === url.split('#')[0]) location.reload();
    })
  }, ctrl.trans.noarg('analysis')) : null;
}

function rematchButtons(ctrl: RoundController): MaybeVNodes {
  const d = ctrl.data,
    me = !!d.player.offeringRematch, them = !!d.opponent.offeringRematch,
    noarg = ctrl.trans.noarg;
  return [
    them ? h('button.rematch-decline', {
      attrs: {
        'data-icon': 'L',
        title: noarg('decline')
      },
      hook: util.bind('click', () => {
        ctrl.socket.send('rematch-no');
      })
    }, ctrl.blind ? noarg('decline') : '') : null,
    h('button.button.rematch.white', {
      class: {
        me,
        them,
        disabled: !me && !(d.opponent.onGame || (!d.clock && d.player.user && d.opponent.user))
      },
      attrs: {
        title: them ? noarg('yourOpponentWantsToPlayANewGameWithYou') : (
          me ? noarg('rematchOfferSent') : '')
      },
      hook: util.bind('click', e => {
        const d = ctrl.data;
        if (d.game.rematch) location.href = router.game(d.game.rematch, d.opponent.color);
        else if (d.player.offeringRematch) {
          d.player.offeringRematch = false;
          ctrl.socket.send('rematch-no');
        }
        else if (d.opponent.onGame) {
          d.player.offeringRematch = true;
          ctrl.socket.send('rematch-yes');
        }
        else if (!(e.target as HTMLElement).classList.contains('disabled')) ctrl.challengeRematch();
      }, ctrl.redraw)
    }, [
      me ? util.spinner() : h('span', noarg('rematch'))
    ])
  ];
}

export function standard(
  ctrl: RoundController,
  condition: ((d: RoundData) => boolean) | undefined,
  icon: string,
  hint: string,
  socketMsg: string,
  onclick?: () => void
): VNode {
  // disabled if condition callback is provided and is falsy
  const enabled = function() {
    return !condition || condition(ctrl.data);
  };
  return h('button.fbt.hint--bottom.' + socketMsg, {
    attrs: {
      disabled: !enabled(),
      'data-hint': ctrl.trans.noarg(hint)
    },
    hook: util.bind('click', _ => {
      if (enabled()) onclick ? onclick() : ctrl.socket.sendLoading(socketMsg);
    })
  }, [
    h('span', ctrl.blind ? [ctrl.trans.noarg(hint)] : util.justIcon(icon))
  ]);
}

export function forceResign(ctrl: RoundController) {
  return ctrl.forceResignable() ? h('div.suggestion', [
    h('p', { hook: onSuggestionHook }, ctrl.trans.noarg('opponentLeftChoices')),
    h('button.button', {
      hook: util.bind('click', () => ctrl.socket.sendLoading('resign-force'))
    }, ctrl.trans.noarg('forceResignation')),
    h('button.button', {
      hook: util.bind('click', () => ctrl.socket.sendLoading('draw-force'))
    }, ctrl.trans.noarg('forceDraw'))
  ]) : null;
}

function actConfirm(ctrl: RoundController, f: (v: boolean) => void, transKey: string, icon: string, klass?: string): VNode {
  return h('div.act_confirm.' + transKey, [
    h('button.fbt.yes.active.hint--bottom.' + (klass || ''), {
      attrs: {'data-hint': ctrl.trans.noarg(transKey) },
      hook: util.bind('click', () => f(true))
    }, [h('span', util.justIcon(icon))]),
    h('button.fbt.no.hint--bottom', {
      attrs: { 'data-hint': ctrl.trans.noarg('cancel') },
      hook: util.bind('click', () => f(false))
    }, [h('span', util.justIcon('L'))])
  ]);
}

export function resignConfirm(ctrl: RoundController): VNode {
  return actConfirm(ctrl, ctrl.resign, 'resign', 'b');
}

export function drawConfirm(ctrl: RoundController): VNode {
  return actConfirm(ctrl, ctrl.offerDraw, 'offerDraw', '2', 'draw-yes');
}

export function threefoldClaimDraw(ctrl: RoundController) {
  return ctrl.data.game.threefold ? h('div.suggestion', [
    h('p', {
      hook: onSuggestionHook
    }, ctrl.trans.noarg('threefoldRepetition')),
    h('button.button', {
      hook: util.bind('click', () => ctrl.socket.sendLoading('draw-claim'))
    }, ctrl.trans.noarg('claimADraw'))
  ]) : null;
}

export function cancelDrawOffer(ctrl: RoundController) {
  return ctrl.data.player.offeringDraw ? h('div.pending', [
    h('p', ctrl.trans.noarg('drawOfferSent'))
  ]) : null;
}

export function answerOpponentDrawOffer(ctrl: RoundController) {
  return ctrl.data.opponent.offeringDraw ? h('div.negotiation.draw', [
    h('p', ctrl.trans.noarg('yourOpponentOffersADraw')),
    acceptButton(ctrl, () => ctrl.socket.sendLoading('draw-yes')),
    declineButton(ctrl, () => ctrl.socket.sendLoading('draw-no'))
  ]) : null;
}

export function cancelTakebackProposition(ctrl: RoundController) {
  return ctrl.data.player.proposingTakeback ? h('div.pending', [
    h('p', ctrl.trans.noarg('takebackPropositionSent')),
    h('button.button', {
      hook: util.bind('click', () => ctrl.socket.sendLoading('takeback-no'))
    }, ctrl.trans.noarg('cancel'))
  ]) : null;
}

function acceptButton(ctrl: RoundController, action: () => void, i18nKey: string = 'accept') {
  const text = ctrl.trans.noarg(i18nKey);
  return ctrl.blind ? h('button', {
    hook: util.bind('click', action)
  }, text) : h('a.accept', {
    attrs: {
      'data-icon': 'E',
      title: text
    },
    hook: util.bind('click', action)
  });
}
function declineButton(ctrl: RoundController, action: () => void, i18nKey: string = 'decline') {
  const text = ctrl.trans.noarg(i18nKey);
  return ctrl.blind ? h('button', {
    hook: util.bind('click', action)
  }, text) : h('a.decline', {
    attrs: {
      'data-icon': 'L',
      title: text
    },
    hook: util.bind('click', action)
  });
}

export function answerOpponentTakebackProposition(ctrl: RoundController) {
  return ctrl.data.opponent.proposingTakeback ? h('div.negotiation.takeback', [
    h('p', ctrl.trans.noarg('yourOpponentProposesATakeback')),
    acceptButton(ctrl, ctrl.takebackYes),
    declineButton(ctrl, () => ctrl.socket.sendLoading('takeback-no'))
  ]) : null;
}

export function submitMove(ctrl: RoundController): VNode | undefined {
  return (ctrl.moveToSubmit || ctrl.dropToSubmit) ? h('div.negotiation.move-confirm', [
    h('p', ctrl.trans.noarg('moveConfirmation')),
    acceptButton(ctrl, () => ctrl.submitMove(true)),
    declineButton(ctrl, () => ctrl.submitMove(false), 'cancel')
  ]) : undefined;
}

export function backToTournament(ctrl: RoundController): VNode | undefined {
  const d = ctrl.data;
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
      h('button.text.button.weak', util.justIcon('Z'), 'Pause')
    ]),
    analysisButton(ctrl)
  ]) : undefined;
}

export function moretime(ctrl: RoundController) {
  return game.moretimeable(ctrl.data) ? h('a.moretime.hint--bottom-left', {
    attrs: {
      'data-hint': ctrl.data.clock ? ctrl.trans('giveNbSeconds', ctrl.data.clock.moretime) :
      ctrl.trans.noarg('giveMoreTime')
    },
    hook: util.bind('click', ctrl.socket.moreTime)
  }, [
    h('span', util.justIcon('O'))
  ]) : null;
}

export function followUp(ctrl: RoundController): VNode {
  const d = ctrl.data,
    noarg = ctrl.trans.noarg,
    rematchable = !d.game.rematch && (status.finished(d) || status.aborted(d)) && !d.tournament && !d.simul && !d.game.boosted,
    newable = (status.finished(d) || status.aborted(d)) && (
      d.game.source === 'lobby' ||
      d.game.source === 'pool'),
    rematchZone = ctrl.challengeRematched ? [
      h('div.suggestion.text', {
        hook: onSuggestionHook,
        attrs: { 'data-icon': 'j' }
      }, noarg('rematchOfferSent'))
    ] : (rematchable || d.game.rematch ? rematchButtons(ctrl) : [
      h('button.button.rematch.white',
        { class: { disabled: true } },
        [h('span', noarg('rematch'))]
      )
    ]);
  return h('div.follow_up', [
    ...rematchZone,
    d.tournament ? h('a.button', {
      attrs: {href: '/tournament/' + d.tournament.id}
    }, noarg('viewTournament')) : null,
    newable ? h('a.button', {
      attrs: { href: d.game.source === 'pool' ? poolUrl(d.clock!, d.opponent.user) : '/?hook_like=' + d.game.id },
    }, noarg('newOpponent')) : null,
    analysisButton(ctrl)
  ]);
}

export function watcherFollowUp(ctrl: RoundController): VNode {
  const d = ctrl.data;
  return h('div.follow_up', [
    d.game.rematch ? h('a.button.text', {
      attrs: {
        'data-icon': 'v',
        href: `/${d.game.rematch}/${d.opponent.color}`
      }
    }, ctrl.trans.noarg('viewRematch')) : null,
    d.tournament ? h('a.button', {
      attrs: {href: '/tournament/' + d.tournament.id}
    }, ctrl.trans.noarg('viewTournament')) : null,
    analysisButton(ctrl)
  ]);
}

const onSuggestionHook: Hooks = {
  insert(vnode) {
    window.lichess.pubsub.emit('round.suggestion')((vnode.elm as HTMLElement).textContent);
  }
};
