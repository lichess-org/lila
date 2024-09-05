import { VNode } from 'snabbdom';
import { looseH as h } from 'common/snabbdom';
import renderClocks from '../view/clocks';
import AnalyseCtrl from '../ctrl';
import { renderMaterialDiffs } from '../view/components';
import { StudyPlayers, Federation, TagArray } from './interfaces';
import { findTag, isFinished, looksLikeLichessGame, resultOf } from './studyChapters';
import { userTitle } from 'common/userLink';
import RelayPlayers, { fidePageLinkAttrs } from './relay/relayPlayers';

export default function(ctrl: AnalyseCtrl): VNode[] | undefined {
  const study = ctrl.study;
  if (!study) return;
  const relayPlayers = study.relay?.players;

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
      relayPlayers,
    ),
  );
}

function renderPlayer(
  ctrl: AnalyseCtrl,
  tags: TagArray[],
  clocks: [VNode, VNode] | undefined,
  materialDiffs: [VNode, VNode],
  players: StudyPlayers | undefined,
  color: Color,
  ticking: boolean,
  showRatings: boolean,
  relayPlayers?: RelayPlayers,
): VNode {
  const player = players?.[color],
    fideId = parseInt(findTag(tags, `${color}fideid`) || ''),
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
          (relayPlayers
            ? h(`a.name.relay-player-${color}`, relayPlayers.playerLinkConfig(player), player.name)
            : h(
              fideId ? 'a.name' : 'span.name',
              { attrs: fidePageLinkAttrs(player, ctrl.isEmbed) },
              player.name,
            )),
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
