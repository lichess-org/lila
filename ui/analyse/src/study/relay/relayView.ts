import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import { TagArray } from '../interfaces';
import { renderClocks } from '../../clocks';
import AnalyseCtrl from '../../ctrl';
import { isFinished } from '../studyChapters';

export function renderPlayers(ctrl: AnalyseCtrl): VNode[] | undefined {
  const study = ctrl.study;
  if (!study || !study.data.chapter.relay) return;
  const relay = study.relay;
  if (!relay) return;
  const clocks = renderClocks(ctrl),
  tags = study.data.chapter.tags,
  ticking = !isFinished(study.data.chapter) && ctrl.turnColor();
  return (['white', 'black'] as Color[]).map(color => renderPlayer(tags, clocks, color, ticking === color));
}

function renderPlayer(tags: TagArray[], clocks: [VNode, VNode] | undefined, color: Color, ticking: boolean): VNode {
  return h(`div.relay_player.${color}`, {
    class: { ticking }
  }, [
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
