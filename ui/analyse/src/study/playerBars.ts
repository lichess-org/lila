import { h, VNode } from 'snabbdom';
import renderClocks from '../clocks';
import AnalyseCtrl from '../ctrl';
import { renderMaterialDiffs } from '../view';
import { TagArray } from './interfaces';
import { findTag, isFinished, resultOf } from './studyChapters';

interface PlayerNames {
  white: string;
  black: string;
}

export default function (ctrl: AnalyseCtrl): VNode[] | undefined {
  const study = ctrl.study;
  if (!study || ctrl.embed) return;
  const tags = study.data.chapter.tags,
    playerNames = {
      white: findTag(tags, 'white')!,
      black: findTag(tags, 'black')!,
    };

  const clocks = renderClocks(ctrl),
    ticking = !isFinished(study.data.chapter) && ctrl.turnColor(),
    materialDiffs = renderMaterialDiffs(ctrl);

  return (['white', 'black'] as Color[]).map(color =>
    renderPlayer(tags, clocks, materialDiffs, playerNames, color, ticking === color, ctrl.bottomColor() !== color)
  );
}

function renderPlayer(
  tags: TagArray[],
  clocks: [VNode, VNode] | undefined,
  materialDiffs: [VNode, VNode],
  playerNames: PlayerNames,
  color: Color,
  ticking: boolean,
  top: boolean
): VNode {
  const title = findTag(tags, `${color}title`),
    elo = findTag(tags, `${color}elo`),
    result = resultOf(tags, color === 'white');
  return h(
    `div.study__player.study__player-${top ? 'top' : 'bot'}`,
    {
      class: { ticking },
    },
    [
      h('div.left', [
        result && h('span.result', result),
        h('span.info', [
          title && h('span.utitle', title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, title + ' '),
          h('span.name', playerNames[color]),
          elo && h('span.elo', elo),
        ]),
      ]),
      materialDiffs[top ? 0 : 1],
      clocks?.[color === 'white' ? 0 : 1],
    ]
  );
}
