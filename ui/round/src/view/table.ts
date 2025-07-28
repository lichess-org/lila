import * as licon from 'lib/licon';
import { abortable, playable, drawableSwiss, resignable, takebackable, TopOrBottom } from 'lib/game/game';
import { render as renderReplay, analysisButton } from './replay';
import renderExpiration from './expiration';
import { userHtml } from './user';
import * as button from './button';
import type RoundController from '../ctrl';
import { type LooseVNodes, hl, bind } from 'lib/snabbdom';
import { toggleButton as boardMenuToggleButton } from 'lib/view/boardMenu';
import { anyClockView } from './clock';

function renderPlayer(ctrl: RoundController, position: TopOrBottom) {
  const player = ctrl.playerAt(position);
  return ctrl.nvui
    ? undefined
    : player.ai
      ? hl('div.user-link.online.ruser.ruser-' + position, [
          hl('i.line'),
          hl('name', i18n.site.aiNameLevelAiLevel('Stockfish', player.ai)),
        ])
      : userHtml(ctrl, player, position);
}

const isLoading = (ctrl: RoundController): boolean => ctrl.loading || ctrl.redirecting;

const loader = () => hl('i.ddloader');

const renderTableWith = (ctrl: RoundController, buttons: LooseVNodes[]) => [
  renderReplay(ctrl),
  buttons.find(x => !!x) && hl('div.rcontrols', buttons),
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
      ? hl('button', { hook: bind('click', action) }, text)
      : hl(`a.${tpe}`, { attrs: { 'data-icon': icon }, hook: bind('click', action) });

  const noBtn = o.no && btn('no', o.no.icon || licon.X, o.no.text || i18n.site.decline, o.no.action);
  const yesBtn =
    o.yes && btn('yes', o.yes.icon || licon.Checkmark, o.yes.text || i18n.site.accept, o.yes.action);

  return {
    promptVNode: hl('div.question', { key: o.prompt }, [noBtn, hl('p', o.prompt), yesBtn]),
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
            !!ctrl.drawConfirm
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
            !!ctrl.resignConfirm
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
    hl('div.rcontrols', [
      hl(
        'div.ricons',
        { class: { confirm: !!(ctrl.drawConfirm || ctrl.resignConfirm), empty: !icons.length } },
        icons,
      ),
      buttons,
    ]),
  ];
};

export const renderTable = (ctrl: RoundController): LooseVNodes => [
  hl('div.round__app__table'),
  renderExpiration(ctrl),
  renderPlayer(ctrl, 'top'),
  ctrl.data.player.spectator
    ? renderTableWatch(ctrl)
    : playable(ctrl.data)
      ? renderTablePlay(ctrl)
      : renderTableEnd(ctrl),
  renderPlayer(ctrl, 'bottom'),
  /* render clocks after players so they display on top of them in col1,
   * since they occupy the same grid cell. This is required to avoid
   * having two columns with min-content, which causes the horizontal moves
   * to overflow: it couldn't be contained in the parent anymore */
  anyClockView(ctrl, 'top'),
  anyClockView(ctrl, 'bottom'),
];
