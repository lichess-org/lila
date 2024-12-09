import type { VNode, Hooks } from 'snabbdom';
import * as licon from 'common/licon';
import { spinnerVdom as spinner } from 'common/spinner';
import { justIcon } from '../util';
import { replayable, rematchable, moretimeable, type PlayerUser } from 'game';
import { finished, aborted } from 'game/status';
import { game as gameRoute } from 'game/router';
import type { RoundData } from '../interfaces';
import type { ClockData } from '../clock/clockCtrl';
import type RoundController from '../ctrl';
import { type LooseVNodes, type LooseVNode, looseH as h, bind, onInsert } from 'common/snabbdom';
import { pubsub } from 'common/pubsub';

export interface ButtonState {
  enabled: boolean;
  overrideHint?: string;
}

function analysisBoardOrientation(data: RoundData) {
  return data.game.variant.key === 'racingKings' ? 'white' : data.player.color;
}

function poolUrl(clock: ClockData, blocking?: PlayerUser) {
  return '/#pool/' + clock.initial / 60 + '+' + clock.increment + (blocking ? '/' + blocking.id : '');
}

function analysisButton(ctrl: RoundController): VNode | false {
  const d = ctrl.data,
    url = gameRoute(d, analysisBoardOrientation(d)) + '#' + ctrl.ply;
  return (
    replayable(d) &&
    h(
      'a.fbt',
      {
        attrs: { href: url },
        hook: bind('click', _ => {
          // force page load in case the URL is the same
          if (location.pathname === url.split('#')[0]) location.reload();
        }),
      },
      i18n.site.analysis,
    )
  );
}

function rematchButtons(ctrl: RoundController): LooseVNodes {
  const d = ctrl.data,
    me = !!d.player.offeringRematch,
    disabled = !me && !d.opponent.onGame && (!!d.clock || !d.player.user || !d.opponent.user),
    them = !!d.opponent.offeringRematch && !disabled;
  if (!rematchable(d)) return [];
  return [
    them &&
      h(
        'button.rematch-decline',
        {
          attrs: { 'data-icon': licon.X, title: i18n.site.decline },
          hook: bind('click', () => ctrl.socket.send('rematch-no')),
        },
        ctrl.nvui ? i18n.site.decline : '',
      ),
    h(
      'button.fbt.rematch.white',
      {
        class: { me, glowing: them, disabled },
        attrs: {
          title: them
            ? i18n.site.yourOpponentWantsToPlayANewGameWithYou
            : me
              ? i18n.site.rematchOfferSent
              : '',
        },
        hook: bind(
          'click',
          () => {
            const d = ctrl.data;
            if (d.game.rematch) location.href = gameRoute(d.game.rematch, d.opponent.color);
            else if (d.player.offeringRematch) {
              d.player.offeringRematch = false;
              ctrl.socket.send('rematch-no');
            } else if (d.opponent.onGame || !d.clock) {
              d.player.offeringRematch = true;
              if (d.opponent.onGame) ctrl.socket.send('rematch-yes');
              else if (!disabled && !d.opponent.onGame) ctrl.challengeRematch();
            }
          },
          ctrl.redraw,
        ),
      },
      [me ? spinner() : h('span', i18n.site.rematch)],
    ),
  ];
}

export function standard(
  ctrl: RoundController,
  condition: ((d: RoundData) => ButtonState) | undefined,
  icon: string,
  hint: string,
  socketMsg: string,
  onclick?: () => void,
): VNode {
  // disabled if condition callback is provided and is falsy
  const enabled = () => !condition || condition(ctrl.data).enabled;
  const hintFn = () => condition?.(ctrl.data)?.overrideHint || hint;
  return h(
    'button.fbt.' + socketMsg,
    {
      attrs: { disabled: !enabled(), title: hintFn() },
      hook: bind('click', () => {
        if (enabled()) onclick ? onclick() : ctrl.socket.sendLoading(socketMsg);
      }),
    },
    [h('span', ctrl.nvui ? [hintFn()] : justIcon(icon))],
  );
}

export function opponentGone(ctrl: RoundController): LooseVNode {
  const gone = ctrl.opponentGone();
  if (ctrl.data.game.rules?.includes('noClaimWin')) return null;
  return gone === true
    ? h('div.suggestion', [
        h('p', { hook: onSuggestionHook }, i18n.site.opponentLeftChoices),
        h(
          'button.button',
          { hook: bind('click', () => ctrl.socket.sendLoading('resign-force')) },
          i18n.site.forceResignation,
        ),
        h(
          'button.button',
          { hook: bind('click', () => ctrl.socket.sendLoading('draw-force')) },
          i18n.site.forceDraw,
        ),
      ])
    : gone !== false &&
        h('div.suggestion', h('p', i18n.site.opponentLeftCounter.asArray(gone, h('strong', '' + gone))));
}

const fbtCancel = (f: (v: boolean) => void) =>
  h('button.fbt.no', {
    attrs: { title: i18n.site.cancel, 'data-icon': licon.X },
    hook: bind('click', () => f(false)),
  });

export const resignConfirm = (ctrl: RoundController): VNode =>
  h('div.act-confirm', [
    h('button.fbt.yes', {
      attrs: { title: i18n.site.resign, 'data-icon': licon.FlagOutline },
      hook: bind('click', () => ctrl.resign(true)),
    }),
    fbtCancel(ctrl.resign),
  ]);

export const drawConfirm = (ctrl: RoundController): VNode =>
  h('div.act-confirm', [
    h('button.fbt.yes.draw-yes', {
      attrs: { title: i18n.site.offerDraw, 'data-icon': licon.OneHalf },
      hook: bind('click', () => ctrl.offerDraw(true)),
    }),
    fbtCancel(ctrl.offerDraw),
  ]);

export const claimThreefold = (ctrl: RoundController, condition: (d: RoundData) => ButtonState): VNode =>
  h(
    'button.button.draw-yes',
    {
      hook: bind('click', () =>
        condition(ctrl.data).enabled ? ctrl.socket.sendLoading('draw-claim') : undefined,
      ),
      attrs: {
        title: condition(ctrl.data)?.overrideHint || i18n.site.claimADraw,
        disabled: !condition(ctrl.data).enabled,
      },
      class: { disabled: !condition(ctrl.data).enabled },
    },
    h('span', '½'),
  );

export function threefoldSuggestion(ctrl: RoundController): LooseVNode {
  return (
    ctrl.data.game.threefold &&
    h('div.suggestion', [h('p', { hook: onSuggestionHook }, i18n.site.threefoldRepetition)])
  );
}

export function backToTournament(ctrl: RoundController): LooseVNode {
  const d = ctrl.data;
  return (
    d.tournament?.running &&
    h('div.follow-up', [
      h(
        'a.text.fbt.strong.glowing',
        {
          attrs: { 'data-icon': licon.PlayTriangle, href: '/tournament/' + d.tournament.id },
          hook: bind('click', ctrl.setRedirecting),
        },
        i18n.site.backToTournament,
      ),
      h('form', { attrs: { method: 'post', action: '/tournament/' + d.tournament.id + '/withdraw' } }, [
        h('button.text.fbt.weak', justIcon(licon.Pause), i18n.site.pause),
      ]),
      analysisButton(ctrl),
    ])
  );
}

export function backToSwiss(ctrl: RoundController): LooseVNode {
  const d = ctrl.data;
  return (
    d.swiss?.running &&
    h('div.follow-up', [
      h(
        'a.text.fbt.strong.glowing',
        {
          attrs: { 'data-icon': licon.PlayTriangle, href: '/swiss/' + d.swiss.id },
          hook: bind('click', ctrl.setRedirecting),
        },
        i18n.site.backToTournament,
      ),
      analysisButton(ctrl),
    ])
  );
}

export function moretime(ctrl: RoundController): LooseVNode {
  return (
    moretimeable(ctrl.data) &&
    h('a.moretime', {
      attrs: {
        title: ctrl.data.clock
          ? i18n.site.giveNbSeconds(ctrl.data.clock.moretime)
          : i18n.preferences.giveMoreTime,
        'data-icon': licon.PlusButton,
      },
      hook: bind('click', ctrl.socket.moreTime),
    })
  );
}

export function followUp(ctrl: RoundController): VNode {
  const d = ctrl.data,
    rematchable =
      !d.game.rematch &&
      (finished(d) || (aborted(d) && (!d.game.rated || !['lobby', 'pool'].includes(d.game.source)))) &&
      !d.tournament &&
      !d.simul &&
      !d.swiss &&
      !d.game.boosted,
    newable = (finished(d) || aborted(d)) && ['lobby', 'pool', 'local'].includes(d.game.source),
    rematchZone = rematchable || d.game.rematch ? rematchButtons(ctrl) : [];
  return h('div.follow-up', [
    ...rematchZone,
    d.tournament &&
      h('a.fbt', { attrs: { href: '/tournament/' + d.tournament.id } }, i18n.site.viewTournament),
    d.swiss && h('a.fbt', { attrs: { href: '/swiss/' + d.swiss.id } }, i18n.site.viewTournament),
    newable &&
      h(
        'a.fbt',
        {
          attrs: {
            href: d.game.source === 'pool' ? poolUrl(d.clock!, d.opponent.user) : '/?hook_like=' + d.game.id,
          },
        },
        i18n.site.newOpponent,
      ),
    analysisButton(ctrl),
  ]);
}

export function watcherFollowUp(ctrl: RoundController): LooseVNode {
  const d = ctrl.data,
    content = [
      d.game.rematch &&
        h('a.fbt.text', { attrs: { href: `/${d.game.rematch}/${d.opponent.color}` } }, i18n.site.viewRematch),
      d.tournament &&
        h('a.fbt', { attrs: { href: '/tournament/' + d.tournament.id } }, i18n.site.viewTournament),

      d.swiss && h('a.fbt', { attrs: { href: '/swiss/' + d.swiss.id } }, i18n.site.viewTournament),
      analysisButton(ctrl),
    ];
  return content.find(x => !!x) && h('div.follow-up', content);
}

const onSuggestionHook: Hooks = onInsert(el => pubsub.emit('round.suggestion', el.textContent));
