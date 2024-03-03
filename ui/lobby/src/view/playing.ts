import { Shogiground } from 'shogiground';
import { usiToSquareNames } from 'shogiops/compat';
import { forsythToRole } from 'shogiops/sfen';
import { handRoles } from 'shogiops/variant/util';
import { h } from 'snabbdom';
import LobbyController from '../ctrl';
import { engineNameFromCode } from 'common/engineName';

function timer(pov) {
  const date = Date.now() + pov.secondsLeft * 1000;
  return h(
    'time.timeago',
    {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).setAttribute('datetime', '' + date);
        },
      },
    },
    window.lishogi.timeago.format(date)
  );
}

export default function (ctrl: LobbyController) {
  if (ctrl.data.nowPlaying.some(pov => pov.variant.key === 'chushogi')) window.lishogi.loadChushogiPieceSprite();
  if (ctrl.data.nowPlaying.some(pov => pov.variant.key === 'kyotoshogi')) window.lishogi.loadKyotoshogiPieceSprite();
  return h(
    'div.now-playing',
    ctrl.data.nowPlaying.map(function (pov) {
      return h(
        'a' + (pov.isMyTurn ? '.my_turn' : ''),
        {
          key: pov.gameId,
          attrs: { href: '/' + pov.fullId },
        },
        [
          h(
            'div.mini-board.v-' + pov.variant.key,
            h('div.sg-wrap', {
              hook: {
                insert(vnode) {
                  const lm = pov.lastMove,
                    variant = pov.variant.key,
                    splitSfen = pov.sfen.split(' ');
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
                    { board: vnode.elm as HTMLElement }
                  );
                },
              },
            })
          ),
          h('span.meta', [
            pov.opponent.ai
              ? engineNameFromCode(pov.opponent.aiCode, pov.opponent.ai, ctrl.trans)
              : pov.opponent.username,
            h(
              'span.indicator',
              pov.isMyTurn
                ? pov.secondsLeft && pov.hasMoved
                  ? timer(pov)
                  : [ctrl.trans.noarg('yourTurn')]
                : h('span', '\xa0')
            ),
          ]),
        ]
      );
    })
  );
}
