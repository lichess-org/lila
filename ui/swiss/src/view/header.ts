import { h } from 'snabbdom';
import { VNode } from 'snabbdom';
import SwissCtrl from '../ctrl';
import { dataIcon } from './util';

function startClock(time: number) {
  return {
    insert: (vnode: VNode) => $(vnode.elm as HTMLElement).clock({ time }),
  };
}

const oneDayInSeconds = 60 * 60 * 24;

function clock(ctrl: SwissCtrl): VNode | undefined {
  const next = ctrl.data.nextRound;
  if (!next) return;
  if (next.in > oneDayInSeconds)
    return h('div.clock', [
      h('time.timeago.shy', {
        attrs: {
          datetime: Date.now() + next.in * 1000,
        },
        hook: {
          insert(vnode) {
            (vnode.elm as HTMLElement).setAttribute('datetime', '' + (Date.now() + next.in * 1000));
          },
        },
      }),
    ]);
  return h(`div.clock.clock-created.time-cache-${next.at}`, [
    h('span.shy', ctrl.data.status == 'created' ? 'Starting in' : 'Next round'),
    h('span.time.text', {
      hook: startClock(next.in + 1),
    }),
  ]);
}

function ongoing(ctrl: SwissCtrl): VNode | undefined {
  const nb = ctrl.data.nbOngoing;
  return nb ? h('div.ongoing', [h('span.nb', [nb]), h('span.shy', 'Ongoing games')]) : undefined;
}

export default function (ctrl: SwissCtrl): VNode {
  const greatPlayer = ctrl.data.greatPlayer;
  return h('div.swiss__main__header', [
    h('i.img', dataIcon('g')),
    h(
      'h1',
      greatPlayer
        ? [
            h(
              'a',
              {
                attrs: {
                  href: greatPlayer.url,
                  target: '_blank',
                  rel: 'noopener',
                },
              },
              greatPlayer.name
            ),
            ' Tournament',
          ]
        : [ctrl.data.name]
    ),
    ctrl.data.status == 'finished' ? undefined : clock(ctrl) || ongoing(ctrl),
  ]);
}
