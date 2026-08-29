import type { VNode, Hooks } from 'snabbdom';

import { finished, aborted, replayable, rematchable, moretimeable, type PlayerUser } from 'lib/game';
import type { ClockData } from 'lib/game/clock/clockCtrl';
import { game as gameRoute } from 'lib/game/router';
import { icons, type Icon } from 'lib/icons';
import { pubsub } from 'lib/pubsub';
import {
  spinnerVdom as spinner,
  type LooseVNodes,
  type LooseVNode,
  hl,
  bind,
  onInsert,
  snabIcon,
} from 'lib/view';

import type RoundController from '../ctrl';
import type { EventsWithoutPayload, RoundData } from '../interfaces';

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
    hl(
      'a.fbt',
      {
        attrs: { href: url },
        hook: bind(
          'click',
          e => {
            // force page load in case the URL is the same
            if (d.local) {
              d.local.analyse();
              return e.preventDefault();
            }
            if (location.pathname === url.split('#')[0]) location.reload();
          },
          undefined,
          false,
        ),
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
      hl(
        'button.rematch-decline',
        {
          attrs: { 'aria-label': i18n.site.decline, title: i18n.site.decline },
          hook: bind('click', () => ctrl.socket.send('rematch-no')),
        },
        ctrl.nvui ? i18n.site.decline : [snabIcon(icons.X)],
      ),
    hl(
      'button.fbt.rematch.white',
      {
        class: { me, glowing: them },
        attrs: {
          disabled,
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
      [me ? spinner() : hl('span', i18n.site.rematch)],
    ),
  ];
}

export function standard(
  ctrl: RoundController,
  condition: ((d: RoundData) => ButtonState) | undefined,
  icon: Icon,
  hint: string,
  socketMsg: EventsWithoutPayload,
  onclick?: () => void,
): VNode {
  // disabled if condition callback is provided and is falsy
  const enabled = () => !condition || condition(ctrl.data).enabled;
  const hintFn = () => condition?.(ctrl.data)?.overrideHint || hint;
  return hl(
    'button.fbt.' + socketMsg,
    {
      attrs: { disabled: !enabled(), ...(!ctrl.nvui ? { title: hintFn() } : {}) },
      hook: bind('click', () => {
        if (enabled()) onclick ? onclick() : ctrl.socket.sendLoading(socketMsg);
      }),
    },
    ctrl.nvui ? [hintFn()] : [hl('span', [snabIcon(icon)])],
  );
}

export function opponentGone(ctrl: RoundController): LooseVNode {
  const gone = ctrl.opponentGone();
  if (ctrl.data.game.rules?.includes('noClaimWin')) return null;
  return gone === true
    ? hl('div.suggestion', [
        hl('p', { hook: onSuggestionHook }, i18n.site.opponentLeftChoices),
        hl(
          'button.button.button-green',
          { hook: bind('click', () => ctrl.socket.sendLoading('resign-force')) },
          i18n.site.forceResignation,
        ),
        hl(
          'button.button',
          { hook: bind('click', () => ctrl.socket.sendLoading('draw-force')) },
          i18n.site.forceDraw,
        ),
      ])
    : gone !== false &&
        hl(
          'div.suggestion.opponent-left-counter',
          hl('p', i18n.site.opponentLeftCounter.asArray(gone, hl('strong', gone))),
        );
}

const fbtCancel = (f: (v: boolean) => void) =>
  hl(
    'button.fbt.no',
    {
      attrs: { 'aria-label': i18n.site.cancel, title: i18n.site.cancel },
      hook: bind('click', () => f(false)),
    },
    [snabIcon(icons.X)],
  );

export const resignConfirm = (ctrl: RoundController): VNode =>
  hl('div.act-confirm', [
    hl(
      'button.fbt.yes',
      {
        attrs: { 'aria-label': i18n.site.resign, title: i18n.site.resign },
        hook: bind('click', () => ctrl.resign(true)),
      },
      [snabIcon(icons.FlagOutline)],
    ),
    fbtCancel(ctrl.resign),
  ]);

export const drawConfirm = (ctrl: RoundController): VNode =>
  hl('div.act-confirm', [
    hl(
      'button.fbt.yes.draw-yes',
      {
        attrs: { 'aria-label': i18n.site.offerDraw, title: i18n.site.offerDraw },
        hook: bind('click', () => ctrl.offerDraw(true)),
      },
      [snabIcon(icons.OneHalf)],
    ),
    fbtCancel(ctrl.offerDraw),
  ]);

export const claimThreefold = (ctrl: RoundController, condition: (d: RoundData) => ButtonState): VNode =>
  hl(
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
    hl('span', '½'),
  );

export function threefoldSuggestion(ctrl: RoundController): LooseVNode {
  return (
    ctrl.data.game.threefold &&
    hl('div.suggestion', [hl('p', { hook: onSuggestionHook }, i18n.site.threefoldRepetition)])
  );
}

export function backToTournament(ctrl: RoundController): LooseVNode {
  const d = ctrl.data;
  return (
    d.tournament?.running &&
    hl('div.follow-up', [
      hl(
        'a.text.fbt.strong.glowing',
        {
          attrs: { href: '/tournament/' + d.tournament.id },
          hook: bind('click', ctrl.setRedirecting),
        },
        [snabIcon(icons.PlayTriangle, 'mirror-rtl'), i18n.site.backToTournament],
      ),
      hl('form', { attrs: { method: 'post', action: '/tournament/' + d.tournament.id + '/withdraw' } }, [
        hl('button.text.fbt.weak', [snabIcon(icons.Pause), i18n.site.pause]),
      ]),
      analysisButton(ctrl),
    ])
  );
}

export function backToSwiss(ctrl: RoundController): LooseVNode {
  const d = ctrl.data;
  return (
    d.swiss?.running &&
    hl('div.follow-up', [
      hl(
        'a.text.fbt.strong.glowing',
        {
          attrs: { href: '/swiss/' + d.swiss.id },
          hook: bind('click', ctrl.setRedirecting),
        },
        [snabIcon(icons.PlayTriangle, 'mirror-rtl'), i18n.site.backToTournament],
      ),
      analysisButton(ctrl),
    ])
  );
}

export function moretime(ctrl: RoundController): LooseVNode {
  return (
    moretimeable(ctrl.data) &&
    hl(
      'a.moretime',
      {
        attrs: {
          'aria-label': ctrl.data.clock
            ? i18n.site.giveNbSeconds(ctrl.data.clock.moretime)
            : i18n.preferences.giveMoreTime,
          title: ctrl.data.clock
            ? i18n.site.giveNbSeconds(ctrl.data.clock.moretime)
            : i18n.preferences.giveMoreTime,
        },
        hook: bind('click', ctrl.socket.moreTime),
      },
      [snabIcon(icons.PlusButton)],
    )
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
  return hl('div.follow-up', [
    rematchZone,
    d.tournament &&
      hl('a.fbt', { attrs: { href: '/tournament/' + d.tournament.id } }, i18n.site.viewTournament),
    d.swiss && hl('a.fbt', { attrs: { href: '/swiss/' + d.swiss.id } }, i18n.site.viewTournament),
    newable &&
      hl(
        'button.fbt.new-opponent',
        {
          hook: bind('click', () => {
            if (d.game.source === 'local') d.local?.newOpponent();
            else if (d.game.source === 'pool') location.href = poolUrl(d.clock!, d.opponent.user);
            else location.href = '/?hook_like=' + d.game.id;
          }),
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
        hl(
          'a.fbt.text',
          { attrs: { href: `/${d.game.rematch}/${d.opponent.color}` } },
          i18n.site.viewRematch,
        ),
      d.tournament &&
        hl('a.fbt', { attrs: { href: '/tournament/' + d.tournament.id } }, i18n.site.viewTournament),

      d.swiss && hl('a.fbt', { attrs: { href: '/swiss/' + d.swiss.id } }, i18n.site.viewTournament),
      analysisButton(ctrl),
    ];
  return content.find(x => !!x) && hl('div.follow-up', content);
}

const onSuggestionHook: Hooks = onInsert(el => pubsub.emit('round.suggestion', el.textContent));
