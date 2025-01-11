import { i18n } from 'i18n';
import { engineNameFromCode } from 'shogi/engine-name';
import { Shogiground } from 'shogiground';
import { usiToSquareNames } from 'shogiops/compat';
import { forsythToRole } from 'shogiops/sfen';
import { handRoles } from 'shogiops/variant/util';
import { type VNode, h } from 'snabbdom';
import type LobbyController from '../ctrl';

function timer(pov) {
  const date = Date.now() + pov.secondsLeft * 1000;
  return h(
    'time.timeago',
    {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).setAttribute('datetime', `${date}`);
        },
      },
    },
    window.lishogi.timeago.format(date),
  );
}

export default function (ctrl: LobbyController): VNode {
  return h(
    'div.now-playing',
    ctrl.data.nowPlaying.map(pov =>
      h(
        `a${pov.isMyTurn ? '.my_turn' : ''}`,
        {
          key: pov.gameId,
          attrs: { href: `/${pov.fullId}` },
        },
        [
          h(
            `div.mini-board.v-${pov.variant.key}`,
            h('div.sg-wrap', {
              hook: {
                insert(vnode) {
                  const lm = pov.lastMove;
                  const variant = pov.variant.key;
                  const splitSfen = pov.sfen.split(' ');
                  Shogiground(
                    {
                      coordinates: { enabled: false },
                      drawable: { enabled: false, visible: false },
                      viewOnly: true,
                      orientation: pov.color,
                      disableContextMenu: false,
                      sfen: {
                        board: splitSfen[0],
                        hands: splitSfen[2],
                      },
                      hands: {
                        inlined: variant !== 'chushogi',
                        roles: handRoles(variant),
                      },
                      lastDests: lm ? usiToSquareNames(lm) : undefined,
                      forsyth: {
                        fromForsyth: forsythToRole(variant),
                      },
                    },
                    { board: vnode.elm as HTMLElement },
                  );
                },
              },
            }),
          ),
          h('span.meta', [
            pov.opponent.ai
              ? engineNameFromCode(pov.opponent.aiCode, pov.opponent.ai)
              : pov.opponent.username,
            h(
              'span.indicator',
              pov.isMyTurn
                ? pov.secondsLeft && pov.hasMoved
                  ? timer(pov)
                  : [i18n('yourTurn')]
                : h('span', '\xa0'),
            ),
          ]),
        ],
      ),
    ),
  );
}
