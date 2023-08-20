import { h, VNode } from 'snabbdom';
import SwissCtrl from '../ctrl';
import { PodiumPlayer } from '../interfaces';
import { userName } from './util';

const podiumStats = (p: PodiumPlayer, ctrl: SwissCtrl): VNode =>
  h('table.stats', [
    h('tr', [h('th', 'Points'), h('td', '' + p.points)]),
    h('tr', [h('th', 'Tie Break'), h('td', '' + p.tieBreak)]),
    p.performance && ctrl.opts.showRatings
      ? h('tr', [h('th', ctrl.trans.noarg('performance')), h('td', '' + p.performance)])
      : null,
  ]);

function podiumPosition(p: PodiumPlayer, pos: string, ctrl: SwissCtrl): VNode | undefined {
  return p
    ? h(
        'div.' + pos,
        {
          class: {
            lame: !!p.lame,
          },
        },
        [
          h('div.trophy'),
          h(
            'a.text.ulpt.user-link',
            {
              attrs: { href: '/@/' + p.user.name },
            },
            userName(p.user),
          ),
          podiumStats(p, ctrl),
        ],
      )
    : undefined;
}

export default function podium(ctrl: SwissCtrl) {
  const p = ctrl.data.podium || [];
  return h('div.podium', [
    podiumPosition(p[1], 'second', ctrl),
    podiumPosition(p[0], 'first', ctrl),
    podiumPosition(p[2], 'third', ctrl),
  ]);
}
