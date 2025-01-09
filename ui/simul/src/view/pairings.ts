import { h } from 'snabbdom';
import SimulCtrl from '../ctrl';
import { Pairing } from '../interfaces';
import * as status from 'game/status';
import { i18n, i18nFormat } from 'i18n';
import { initOneWithState } from 'common/mini-board';
import { i18nVariant } from 'i18n/variant';

export default function (ctrl: SimulCtrl) {
  return h('div.game-list.now-playing.box__pad', ctrl.data.pairings.map(miniPairing(ctrl)));
}

const miniPairing = (ctrl: SimulCtrl) => (pairing: Pairing) => {
  const game = pairing.game,
    player = pairing.player,
    result =
      pairing.game.status >= status.ids.mate
        ? pairing.game.winner
          ? pairing.game.winner === pairing.hostColor
            ? i18nFormat('xWon', ctrl.data.host.name)
            : i18nFormat('xLost', ctrl.data.host.name)
          : i18n('draw')
        : '*';

  return h(
    'a',
    {
      class: {
        host: ctrl.data.host.gameId === game.id && ctrl.data.isRunning,
      },
      attrs: {
        href: `/${game.id}/${game.orient}`,
      },
    },
    [
      h(
        `span.mini-board mini-board-${game.id} parse-sfen v-${game.variant}`,
        {
          props: {
            'data-color': game.orient,
            'data-sfen': game.sfen,
            'data-lastmove': game.lastMove,
            'data-variant': game.variant,
          },
          hook: {
            insert: vnode => {
              initOneWithState(vnode.elm as HTMLElement, {
                sfen: game.sfen,
                orientation: game.orient,
                lastMove: game.lastMove,
                variant: game.variant,
              });
            },
          },
        },
        [h('div', { class: { 'sg-wrap': true } })]
      ),
      h('span', { class: { vstext: true } }, [
        h('span', { class: { vstext__pl: true } }, [i18nVariant(pairing.variant), h('br'), result]),
        h('div', { class: { vstext__op: true } }, [
          player.name,
          h('br'),
          player.title ? `${player.title} ` : '',
          player.rating,
        ]),
      ]),
    ]
  );
};
