import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import { TagArray } from '../interfaces';
import { renderClocks } from '../../clocks';
import AnalyseCtrl from '../../ctrl';

export function renderPlayers(ctrl: AnalyseCtrl): [VNode, VNode] | undefined {
  const study = ctrl.study;
  if (!study || !study.data.chapter.relay) return;
  const clocks = renderClocks(ctrl);
  const tags = study.data.chapter.tags;
  return [
    renderPlayer(tags, clocks, 'white'),
    renderPlayer(tags, clocks, 'black')
  ];
}

function renderPlayer(tags: TagArray[], clocks: [VNode, VNode] | undefined, color: Color): VNode {
  return h('div.relay_player.' + color, [
    h('div.left', [
      h('span.color'),
      playerInfo(tags, color)
    ]),
    clocks && clocks[color === 'white' ? 0 : 1]
  ]);
}

function playerInfo(tags: TagArray[], color: Color): VNode {
  const title = findTag(tags, `${color}title`),
  elo = findTag(tags, `${color}elo`);
  return h('span.info', [
    title && h('span.title', title + ' '),
    h('span.name', findTag(tags, color) || color),
    elo && h('span.elo', elo)
  ]);
}

function findTag(tags: TagArray[], name: string): string | undefined {
  const t = tags.find(t => t[0].toLowerCase() === name);
  return t && t[1];
}
