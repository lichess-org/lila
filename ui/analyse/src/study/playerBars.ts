import { VNode } from 'snabbdom';
import { looseH as h } from 'common/snabbdom';
import renderClocks from '../view/clocks';
import AnalyseCtrl from '../ctrl';
import { renderMaterialDiffs } from '../view/components';
import { ChapterPreviewPlayers, Federation, TagArray } from './interfaces';
import { findTag, isFinished, looksLikeLichessGame, resultOf } from './studyChapters';
import { userTitle } from 'common/userLink';

export default function (ctrl: AnalyseCtrl): VNode[] | undefined {
  const study = ctrl.study;
  if (!study) return;

  const players = study.currentChapter().players,
    tags = study.data.chapter.tags,
    clocks = renderClocks(ctrl),
    ticking = !isFinished(study.data.chapter) && ctrl.turnColor(),
    materialDiffs = renderMaterialDiffs(ctrl);

  return (['white', 'black'] as Color[]).map(color =>
    renderPlayer(
      ctrl,
      tags,
      clocks,
      materialDiffs,
      players,
      color,
      ticking === color,
      study.data.showRatings || !looksLikeLichessGame(tags),
    ),
  );
}

function renderPlayer(
  ctrl: AnalyseCtrl,
  tags: TagArray[],
  clocks: [VNode, VNode] | undefined,
  materialDiffs: [VNode, VNode],
  players: ChapterPreviewPlayers | undefined,
  color: Color,
  ticking: boolean,
  showRatings: boolean,
): VNode {
  const player = players?.[color],
    fideId = findTag(tags, `${color}fideid`),
    team = findTag(tags, `${color}team`),
    rating = showRatings && player?.rating,
    result = resultOf(tags, color === 'white'),
    top = ctrl.bottomColor() !== color;
  return h(`div.study__player.study__player-${top ? 'top' : 'bot'}`, { class: { ticking } }, [
    h('div.left', [
      result && h('span.result', result),
      h('span.info', [
        team ? h('span.team', team) : undefined,
        playerFed(player?.fed),
        player && userTitle(player),
        player &&
          (fideId
            ? h(
                'a.name',
                { attrs: { href: `/fide/${fideId}/redirect`, target: ctrl.isEmbed ? '_blank' : '' } },
                player.name,
              )
            : h('span.name', player.name)),
        rating && h('span.elo', `${rating}`),
      ]),
    ]),
    materialDiffs[top ? 0 : 1],
    clocks?.[color === 'white' ? 0 : 1],
  ]);
}

export const playerFed = (fed?: Federation): VNode | undefined =>
  fed &&
  h('img.mini-game__flag', {
    attrs: { src: site.asset.url(`images/fide-fed/${fed.id}.svg`), title: `Federation: ${fed.name}` },
  });
