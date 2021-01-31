import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import SwissCtrl from '../ctrl';
import { PodiumPlayer } from '../interfaces';
import { userName } from './util';

function podiumStats(p: PodiumPlayer, trans: Trans): VNode {
  const noarg = trans.noarg;
  return h('table.stats', [
    h('tr', [h('th', 'Points'), h('td', '' + p.points)]),
    h('tr', [h('th', 'Tie Break'), h('td', '' + p.tieBreak)]),
    p.performance ? h('tr', [h('th', noarg('performance')), h('td', '' + p.performance)]) : null
  ]);
}

function podiumPosition(p: PodiumPlayer, pos: string, trans: Trans): VNode | undefined {
  return p ? h('div.' + pos, {
    class: {
      engine: !!p.engine
    }
  }, [
    h('div.trophy'),
    h('a.text.ulpt.user-link', {
      attrs: { href: '/@/' + p.user.name }
    }, userName(p.user)),
    podiumStats(p, trans)
  ]) : undefined;
}

export default function podium(ctrl: SwissCtrl) {
  const p = ctrl.data.podium || [];
  return h('div.podium', [
    podiumPosition(p[1], 'second', ctrl.trans),
    podiumPosition(p[0], 'first', ctrl.trans),
    podiumPosition(p[2], 'third', ctrl.trans)
  ]);
}
