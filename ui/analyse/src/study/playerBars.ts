import type { VNode } from 'snabbdom';
import { hl } from 'lib/view';
import renderClocks from '../view/clocks';
import type AnalyseCtrl from '../ctrl';
import { renderMaterialDiffs } from '../view/components';
import type { StudyPlayers, Federation, TagArray, StudyPlayer, StatusStr } from './interfaces';
import { findTag, looksLikeLichessGame, resultOf } from './studyChapters';
import { userTitle } from 'lib/view/userLink';
import RelayPlayers, { fidePageLinkAttrs, playerId, playerPhotoOrFallback } from './relay/relayPlayers';
import { StudyCtrl } from './studyDeps';
import { intersection } from 'lib/tree/path';
import { defined } from 'lib';
import { resultTag } from './studyView';
import type { RelayRound } from './relay/interfaces';
import { playerColoredResult } from './relay/customScoreStatus';

export default function (ctrl: AnalyseCtrl): VNode[] | undefined {
  const study = ctrl.study;
  if (!study) return;
  const relayPlayers = study.relay?.players;

  const players = study.currentChapter().players,
    tags = study.data.chapter.tags,
    clocks = renderClocks(ctrl, selectClockPath(ctrl, study)),
    tickingColor = study.isClockTicking(ctrl.path) && ctrl.turnColor(),
    materialDiffs = renderMaterialDiffs(ctrl);

  return (['white', 'black'] as Color[]).map(color =>
    renderPlayer(
      ctrl,
      tags,
      clocks,
      materialDiffs,
      players,
      color,
      tickingColor === color,
      study.data.showRatings || !looksLikeLichessGame(tags),
      study.relay?.round,
      relayPlayers,
    ),
  );
}

// The tree node whose clocks are displayed.
// Finished game: last mainline node of the current variation.
// Ongoing game: the last mainline node, no matter what
function selectClockPath(ctrl: AnalyseCtrl, study: StudyCtrl): Tree.Path {
  const gamePath = ctrl.gamePath || study.data.chapter.relayPath;
  return ctrl.node.clock ? ctrl.path : gamePath ? intersection(ctrl.path, gamePath) : ctrl.path;
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
  round?: RelayRound,
  relayPlayers?: RelayPlayers,
): VNode {
  const showResult: boolean =
      !defined(ctrl.study?.relay) ||
      ctrl.study?.multiBoard.showResults() ||
      ctrl.node.ply === ctrl.tree.lastPly(),
    team = findTag(tags, `${color}team`),
    rawStatus = showResult ? findTag(tags, 'result')?.replace(/1\/2/g, '½') : undefined,
    status = rawStatus && rawStatus !== '*' ? (rawStatus as StatusStr) : undefined,
    result = showResult ? resultOf(tags, color === 'white') : undefined,
    top = ctrl.bottomColor() !== color,
    eloTag = findTag(tags, `${color}elo`),
    fideIdTag = findTag(tags, `${color}fideid`),
    fideId = fideIdTag ? parseInt(fideIdTag) : undefined,
    player: StudyPlayer = {
      ...players?.[color],
      name: findTag(tags, color),
      title: findTag(tags, `${color}title`),
      rating: showRatings && eloTag ? parseInt(eloTag) : undefined,
      fideId,
    },
    photo = fideId ? relayPlayers?.fidePhoto(fideId) : undefined;
  const coloredResult = status && status !== '*' && playerColoredResult(status, color, round);
  const resultNode = coloredResult
    ? hl(`${coloredResult.tag}.result`, coloredResult.points)
    : result && hl(`${resultTag(result)}.result`, result);
  return relayPlayers
    ? hl(`div.relay-board-player.relay-board-player-${top ? 'top' : 'bot'}`, { class: { ticking } }, [
        hl('div.left', [
          playerPhotoOrFallback(player, photo, 'small', 'relay-board-player__photo'),
          hl('div.info-split', [
            hl('div', [
              !!player.title && userTitle(player),
              playerId(player) &&
                hl(`a.name.relay-player-${color}`, relayPlayers.playerLinkConfig(player), player.name),
            ]),
            hl('div.info-secondary', [
              team ? hl('span.team', team) : undefined,
              playerFedFlag(player?.fed),
              player.rating && hl('span.elo', `${player.rating}`),
            ]),
          ]),
          resultNode,
        ]),
        materialDiffs[top ? 0 : 1],
        clocks?.[color === 'white' ? 0 : 1],
      ])
    : hl(`div.study__player.study__player-${top ? 'top' : 'bot'}`, { class: { ticking } }, [
        hl('div.left', [
          resultNode,
          hl('span.info', [
            team ? hl('span.team', team) : undefined,
            playerFedFlag(player?.fed),
            !!player.title && userTitle(player),
            playerId(player) &&
              hl(
                player.fideId ? 'a.name' : 'span.name',
                { attrs: fidePageLinkAttrs(player, ctrl.isEmbed) },
                player.name,
              ),
            player.rating && hl('span.elo', `${player.rating}`),
          ]),
        ]),
        materialDiffs[top ? 0 : 1],
        clocks?.[color === 'white' ? 0 : 1],
      ]);
}

export const playerFedFlag = (fed?: Federation): VNode | undefined =>
  fed &&
  hl('img.mini-game__flag', {
    attrs: {
      src: site.asset.fideFedSrc(fed.id),
      title: `Federation: ${fed.name}`,
    },
  });
