import { VNode, Hooks } from 'snabbdom';
import * as licon from 'common/licon';
import { spinnerVdom as spinner } from 'common/spinner';
import * as util from '../util';
import * as game from 'game';
import * as status from 'game/status';
import { game as gameRoute } from 'game/router';
import { RoundData } from '../interfaces';
import { ClockData } from '../clock/clockCtrl';
import RoundController from '../ctrl';
import { LooseVNodes, looseH as h } from 'common/snabbdom';

export interface ButtonState {
  enabled: boolean;
  overrideHint?: string;
}

function analysisBoardOrientation(data: RoundData) {
  return data.game.variant.key === 'racingKings' ? 'white' : data.player.color;
}

function poolUrl(clock: ClockData, blocking?: game.PlayerUser) {
  return '/#pool/' + clock.initial / 60 + '+' + clock.increment + (blocking ? '/' + blocking.id : '');
}

function analysisButton(ctrl: RoundController): VNode | false {
  const d = ctrl.data,
    url = gameRoute(d, analysisBoardOrientation(d)) + '#' + ctrl.ply;
  return (
    game.replayable(d) &&
    h(
      'a.fbt',
      {
        attrs: { href: url },
        hook: util.bind('click', _ => {
          // force page load in case the URL is the same
          if (location.pathname === url.split('#')[0]) location.reload();
        }),
      },
      ctrl.noarg('analysis'),
    )
  );
}

function rematchButtons(ctrl: RoundController): LooseVNodes {
  const d = ctrl.data,
    me = !!d.player.offeringRematch,
    disabled = !me && !d.opponent.onGame && (!!d.clock || !d.player.user || !d.opponent.user),
    them = !!d.opponent.offeringRematch && !disabled,
    noarg = ctrl.noarg;
  if (!game.rematchable(d)) return [];
  return [
    them &&
      h(
        'button.rematch-decline',
        {
          attrs: { 'data-icon': licon.X, title: noarg('decline') },
          hook: util.bind('click', () => ctrl.socket.send('rematch-no')),
        },
        ctrl.nvui ? noarg('decline') : '',
      ),
    h(
      'button.fbt.rematch.white',
      {
        class: { me, glowing: them, disabled },
        attrs: {
          title: them ? noarg('yourOpponentWantsToPlayANewGameWithYou') : me ? noarg('rematchOfferSent') : '',
        },
        hook: util.bind(
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
      [me ? spinner() : h('span', noarg('rematch'))],
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
      attrs: { disabled: !enabled(), title: ctrl.noarg(hintFn()) },
      hook: util.bind('click', () => {
        if (enabled()) onclick ? onclick() : ctrl.socket.sendLoading(socketMsg);
      }),
    },
    [h('span', ctrl.nvui ? [ctrl.noarg(hintFn())] : util.justIcon(icon))],
  );
}

export function opponentGone(ctrl: RoundController) {
  const gone = ctrl.opponentGone();
  if (ctrl.data.game.rules?.includes('noClaimWin')) return null;
  return gone === true
    ? h('div.suggestion', [
        h('p', { hook: onSuggestionHook }, ctrl.noarg('opponentLeftChoices')),
        h(
          'button.button',
          { hook: util.bind('click', () => ctrl.socket.sendLoading('resign-force')) },
          ctrl.noarg('forceResignation'),
        ),
        h(
          'button.button',
          { hook: util.bind('click', () => ctrl.socket.sendLoading('draw-force')) },
          ctrl.noarg('forceDraw'),
        ),
      ])
    : gone &&
        h(
          'div.suggestion',
          h('p', ctrl.trans.vdomPlural('opponentLeftCounter', gone, h('strong', '' + gone))),
        );
}

const fbtCancel = (ctrl: RoundController, f: (v: boolean) => void) =>
  h('button.fbt.no', {
    attrs: { title: ctrl.noarg('cancel'), 'data-icon': licon.X },
    hook: util.bind('click', () => f(false)),
  });

export const resignConfirm = (ctrl: RoundController): VNode =>
  h('div.act-confirm', [
    h('button.fbt.yes', {
      attrs: { title: ctrl.noarg('resign'), 'data-icon': licon.FlagOutline },
      hook: util.bind('click', () => ctrl.resign(true)),
    }),
    fbtCancel(ctrl, ctrl.resign),
  ]);

export const drawConfirm = (ctrl: RoundController): VNode =>
  h('div.act-confirm', [
    h('button.fbt.yes.draw-yes', {
      attrs: { title: ctrl.noarg('offerDraw'), 'data-icon': licon.OneHalf },
      hook: util.bind('click', () => ctrl.offerDraw(true)),
    }),
    fbtCancel(ctrl, ctrl.offerDraw),
  ]);

export const claimThreefold = (ctrl: RoundController, condition: (d: RoundData) => ButtonState): VNode =>
  h(
    'button.button.draw-yes',
    {
      hook: util.bind('click', () =>
        condition(ctrl.data).enabled ? ctrl.socket.sendLoading('draw-claim') : undefined,
      ),
      attrs: {
        title: ctrl.noarg(condition(ctrl.data)?.overrideHint || 'claimADraw'),
        disabled: !condition(ctrl.data).enabled,
      },
      class: { disabled: !condition(ctrl.data).enabled },
    },
    h('span', '½'),
  );

export function threefoldSuggestion(ctrl: RoundController) {
  return (
    ctrl.data.game.threefold &&
    h('div.suggestion', [h('p', { hook: onSuggestionHook }, ctrl.noarg('threefoldRepetition'))])
  );
}

export function backToTournament(ctrl: RoundController) {
  const d = ctrl.data;
  return (
    d.tournament?.running &&
    h('div.follow-up', [
      h(
        'a.text.fbt.strong.glowing',
        {
          attrs: { 'data-icon': licon.PlayTriangle, href: '/tournament/' + d.tournament.id },
          hook: util.bind('click', ctrl.setRedirecting),
        },
        ctrl.noarg('backToTournament'),
      ),
      h('form', { attrs: { method: 'post', action: '/tournament/' + d.tournament.id + '/withdraw' } }, [
        h('button.text.fbt.weak', util.justIcon(licon.Pause), ctrl.noarg('pause')),
      ]),
      analysisButton(ctrl),
    ])
  );
}

export function backToSwiss(ctrl: RoundController) {
  const d = ctrl.data;
  return (
    d.swiss?.running &&
    h('div.follow-up', [
      h(
        'a.text.fbt.strong.glowing',
        {
          attrs: { 'data-icon': licon.PlayTriangle, href: '/swiss/' + d.swiss.id },
          hook: util.bind('click', ctrl.setRedirecting),
        },
        ctrl.noarg('backToTournament'),
      ),
      analysisButton(ctrl),
    ])
  );
}

export function moretime(ctrl: RoundController) {
  return (
    game.moretimeable(ctrl.data) &&
    h('a.moretime', {
      attrs: {
        title: ctrl.data.clock
          ? ctrl.trans('giveNbSeconds', ctrl.data.clock.moretime)
          : ctrl.noarg('giveMoreTime'),
        'data-icon': licon.PlusButton,
      },
      hook: util.bind('click', ctrl.socket.moreTime),
    })
  );
}

export function followUp(ctrl: RoundController): VNode {
  const d = ctrl.data,
    rematchable =
      !d.game.rematch &&
      (status.finished(d) ||
        (status.aborted(d) && (!d.game.rated || !['lobby', 'pool'].includes(d.game.source)))) &&
      !d.tournament &&
      !d.simul &&
      !d.swiss &&
      !d.game.boosted,
    newable =
      (status.finished(d) || status.aborted(d)) && (d.game.source === 'lobby' || d.game.source === 'pool'),
    rematchZone = rematchable || d.game.rematch ? rematchButtons(ctrl) : [];
  return h('div.follow-up', [
    ...rematchZone,
    d.tournament &&
      h('a.fbt', { attrs: { href: '/tournament/' + d.tournament.id } }, ctrl.noarg('viewTournament')),
    d.swiss && h('a.fbt', { attrs: { href: '/swiss/' + d.swiss.id } }, ctrl.noarg('viewTournament')),
    newable &&
      h(
        'a.fbt',
        {
          attrs: {
            href: d.game.source === 'pool' ? poolUrl(d.clock!, d.opponent.user) : '/?hook_like=' + d.game.id,
          },
        },
        ctrl.noarg('newOpponent'),
      ),
    analysisButton(ctrl),
  ]);
}

export function watcherFollowUp(ctrl: RoundController) {
  const d = ctrl.data,
    content = [
      d.game.rematch &&
        h(
          'a.fbt.text',
          { attrs: { href: `/${d.game.rematch}/${d.opponent.color}` } },
          ctrl.noarg('viewRematch'),
        ),
      d.tournament &&
        h('a.fbt', { attrs: { href: '/tournament/' + d.tournament.id } }, ctrl.noarg('viewTournament')),

      d.swiss && h('a.fbt', { attrs: { href: '/swiss/' + d.swiss.id } }, ctrl.noarg('viewTournament')),
      analysisButton(ctrl),
    ];
  return content.find(x => !!x) && h('div.follow-up', content);
}

const onSuggestionHook: Hooks = util.onInsert(el => site.pubsub.emit('round.suggestion', el.textContent));
