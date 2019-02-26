import { h } from 'snabbdom';
import { Draughtsground } from 'draughtsground';
import LobbyController from '../ctrl';

function timer(pov) {
  const date = Date.now() + pov.secondsLeft * 1000;
  return h('time.timeago', {
    hook: {
      insert(vnode) {
        (vnode.elm as HTMLElement).setAttribute('datetime', '' + date);
      }
    }
  }, window.lidraughts.timeago.format(date));
}

export default function(ctrl: LobbyController) {
  return h('div.now-playing',
    ctrl.data.nowPlaying.map(function(pov) {
      return h('a.' + pov.variant.key + (pov.isMyTurn ? '.my_turn' : ''), {
        key: pov.gameId,
        attrs: { href: '/' + pov.fullId }
      }, [
        h('div.mini-board.cg-board-wrap.is2d', {
          hook: {
            insert(vnode) {
              const lm = String(pov.lastMove);
              Draughtsground(vnode.elm as HTMLElement, {
                coordinates: 0,
                drawable: { enabled: false, visible: false },
                resizable: false,
                viewOnly: true,
                orientation: pov.color,
                fen: pov.fen,
                lastMove: lm ? [(lm[0] + lm[1]) as Key, (lm[2] + lm[3]) as Key] : undefined
              });
            }
          }
        }, [h('div.cg-board')]),
        h('span.meta', [
          pov.opponent.ai ? ctrl.trans('aiNameLevelAiLevel', 'Scan', pov.opponent.ai) : pov.opponent.username,
          h('span.indicator',
            pov.isMyTurn ?
            (pov.secondsLeft ? timer(pov) : [ctrl.trans.noarg('yourTurn')]) :
            h('span', {
              hook: {
                insert(vnode) {
                  (vnode.elm as HTMLElement).innerHTML = '&nbsp;';
                }
              }
            }))
        ])
      ]);
    }));
}
