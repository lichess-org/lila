import * as licon from 'common/licon';
import type { Position } from '../interfaces';
import { abortable, playable, drawableSwiss, resignable, takebackable } from 'game';
import { aborted, finished } from 'game/status';
import { renderClock } from '../clock/clockView';
import renderCorresClock from '../corresClock/corresClockView';
import { render as renderReplay, analysisButton } from './replay';
import renderExpiration from './expiration';
import { userHtml } from './user';
import * as button from './button';
import type RoundController from '../ctrl';
import { type LooseVNodes, looseH as h, bind } from 'common/snabbdom';
import { toggleButton as boardMenuToggleButton } from 'common/boardMenu';

function renderPlayer(ctrl: RoundController, position: Position) {
  const player = ctrl.playerAt(position);
  return ctrl.nvui
    ? undefined
    : player.ai
      ? h('div.user-link.online.ruser.ruser-' + position, [
          h('i.line'),
          h('name', i18n.site.aiNameLevelAiLevel('Stockfish', player.ai)),
        ])
      : (ctrl.opts.local?.userVNode(player, position) ?? userHtml(ctrl, player, position));
}

const isLoading = (ctrl: RoundController): boolean => ctrl.loading || ctrl.redirecting;

const loader = () => h('i.ddloader');

const renderTableWith = (ctrl: RoundController, buttons: LooseVNodes) => [
  renderReplay(ctrl),
  buttons.find(x => !!x) && h('div.rcontrols', buttons),
];

export const renderTableEnd = (ctrl: RoundController): LooseVNodes =>
  renderTableWith(ctrl, [
    isLoading(ctrl)
      ? loader()
      : button.backToTournament(ctrl) || button.backToSwiss(ctrl) || button.followUp(ctrl),
  ]);

export const renderTableWatch = (ctrl: RoundController): LooseVNodes =>
  renderTableWith(ctrl, [
    isLoading(ctrl) ? loader() : playable(ctrl.data) ? undefined : button.watcherFollowUp(ctrl),
  ]);

const prompt = (ctrl: RoundController) => {
  const o = ctrl.question();
  if (!o) return {};

  const btn = (tpe: 'yes' | 'no', icon: string, text: string, action: () => void) =>
    ctrl.nvui
      ? h('button', { hook: bind('click', action) }, text)
      : h(`a.${tpe}`, { attrs: { 'data-icon': icon }, hook: bind('click', action) });

  const noBtn = o.no && btn('no', o.no.icon || licon.X, o.no.text || i18n.site.decline, o.no.action);
  const yesBtn =
    o.yes && btn('yes', o.yes.icon || licon.Checkmark, o.yes.text || i18n.site.accept, o.yes.action);

  return {
    promptVNode: h('div.question', { key: o.prompt }, [noBtn, h('p', o.prompt), yesBtn]),
    isQuestion: o.no !== undefined || o.yes !== undefined,
  };
};

export const renderTablePlay = (ctrl: RoundController): LooseVNodes => {
  const d = ctrl.data,
    loading = isLoading(ctrl),
    { promptVNode, isQuestion } = prompt(ctrl),
    icons =
      loading || isQuestion
        ? []
        : [
            abortable(d)
              ? button.standard(ctrl, undefined, licon.X, i18n.site.abortGame, 'abort')
              : button.standard(
                  ctrl,
                  d => ({ enabled: takebackable(d) }),
                  licon.Back,
                  i18n.site.proposeATakeback,
                  'takeback-yes',
                  ctrl.takebackYes,
                ),
            ctrl.drawConfirm
              ? button.drawConfirm(ctrl)
              : ctrl.data.game.threefold
                ? button.claimThreefold(ctrl, d => {
                    const threefoldable = drawableSwiss(d);
                    return {
                      enabled: threefoldable,
                      overrideHint: threefoldable ? undefined : i18n.site.noDrawBeforeSwissLimit,
                    };
                  })
                : button.standard(
                    ctrl,
                    d => ({
                      enabled: ctrl.canOfferDraw(),
                      overrideHint: drawableSwiss(d) ? undefined : i18n.site.noDrawBeforeSwissLimit,
                    }),
                    licon.OneHalf,
                    i18n.site.offerDraw,
                    'draw-yes',
                    () => ctrl.offerDraw(true),
                  ),
            ctrl.resignConfirm
              ? button.resignConfirm(ctrl)
              : button.standard(
                  ctrl,
                  d => ({ enabled: resignable(d) }),
                  licon.FlagOutline,
                  i18n.site.resign,
                  'resign',
                  () => ctrl.resign(true),
                ),
            analysisButton(ctrl),
            boardMenuToggleButton(ctrl.menu, i18n.site.menu),
          ],
    buttons = loading
      ? [loader()]
      : [promptVNode, button.opponentGone(ctrl), button.threefoldSuggestion(ctrl)];
  return [
    renderReplay(ctrl),
    h('div.rcontrols', [
      h('div.ricons', { class: { confirm: !!(ctrl.drawConfirm || ctrl.resignConfirm) } }, icons),
      ...buttons,
    ]),
  ];
};

function whosTurn(ctrl: RoundController, color: Color, position: Position) {
  const d = ctrl.data;
  if (finished(d) || aborted(d)) return;
  return h(
    'div.rclock.rclock-turn.rclock-' + position,
    d.game.player === color &&
      h(
        'div.rclock-turn__text',
        d.player.spectator
          ? i18n.site[d.game.player === 'white' ? 'whitePlays' : 'blackPlays']
          : i18n.site[d.game.player === d.player.color ? 'yourTurn' : 'waitingForOpponent'],
      ),
  );
}

function anyClock(ctrl: RoundController, position: Position) {
  const player = ctrl.playerAt(position);
  if (ctrl.clock) return renderClock(ctrl, player, position);
  else if (ctrl.data.correspondence && ctrl.data.game.turns > 1)
    return renderCorresClock(ctrl.corresClock!, player.color, position, ctrl.data.game.player);
  else return whosTurn(ctrl, player.color, position);
}

export const renderTable = (ctrl: RoundController): LooseVNodes => [
  h('div.round__app__table'),
  renderExpiration(ctrl),
  renderPlayer(ctrl, 'top'),
  ...(ctrl.data.player.spectator
    ? renderTableWatch(ctrl)
    : playable(ctrl.data)
      ? renderTablePlay(ctrl)
      : renderTableEnd(ctrl)),
  renderPlayer(ctrl, 'bottom'),
  /* render clocks after players so they display on top of them in col1,
   * since they occupy the same grid cell. This is required to avoid
   * having two columns with min-content, which causes the horizontal moves
   * to overflow: it couldn't be contained in the parent anymore */
  anyClock(ctrl, 'top'),
  anyClock(ctrl, 'bottom'),
];
