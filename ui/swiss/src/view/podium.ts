import { h, type VNode } from 'snabbdom';
import type SwissCtrl from '../ctrl';
import type { PodiumPlayer } from '../interfaces';
import { userLink } from 'lib/view/userLink';
import { defined } from 'lib';

const podiumStats = (p: PodiumPlayer, ctrl: SwissCtrl): VNode =>
  h('table.stats', [
    h('tr', [h('th', i18n.site.points), h('td', '' + p.points)]),
    h('tr', [h('th', i18n.swiss.tieBreak), h('td', '' + p.tieBreak)]),
    p.performance && ctrl.opts.showRatings
      ? h('tr', [h('th', i18n.site.performance), h('td', '' + p.performance)])
      : null,
  ]);

function podiumPosition(p: PodiumPlayer, pos: string, ctrl: SwissCtrl): VNode | undefined {
  if (!p) return undefined;
  const patron = defined(p.user.patronColor);
  return h('div.' + pos, { class: { lame: !!p.lame } }, [
    h('div.trophy'),
    userLink({ ...p.user, line: patron, online: patron }),
    podiumStats(p, ctrl),
  ]);
}

export default function podium(ctrl: SwissCtrl) {
  const p = ctrl.data.podium || [];
  return h('div.podium', [
    podiumPosition(p[1], 'second', ctrl),
    podiumPosition(p[0], 'first', ctrl),
    podiumPosition(p[2], 'third', ctrl),
  ]);
}
