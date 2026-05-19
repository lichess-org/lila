import { COLORS } from 'chessops';

import { defined, scrollToInnerSelector } from 'lib';
import { hl } from 'lib/view';
import { userTitle } from 'lib/view/userLink';

import { playerFedFlag } from '@/view/util';

import type { ChapterPreview } from '../interfaces';
import { pinIcon, renderClock, verticalEvalGauge } from '../multiBoard';
import type { MultiCloudEval } from '../multiCloudEval';
import { gameLinkAttrs } from '../studyChapters';
import type { StudyCtrl } from '../studyDeps';
import { playerColoredResult } from './customScoreStatus';
import type RelayCtrl from './relayCtrl';

export const gamesLists = (study: StudyCtrl, relay: RelayCtrl) => {
  const cloudEval = study.multiCloudEval?.thisIfShowEval();
  const nonPinned = gamesList(study, relay, false, cloudEval);
  const pinned = relay.players.pins.anyPinned() ? gamesList(study, relay, true, cloudEval) : [];
  return hl(
    'div.relay-games',
    {
      class: { 'relay-games__eval': defined(cloudEval) },
      hook: {
        postpatch(old, vnode) {
          const currentId = study.data.chapter.id;
          if (old.data!.current !== currentId)
            scrollToInnerSelector(vnode.elm as HTMLElement, '.relay-game--current');
          vnode.data!.current = currentId;
        },
      },
    },
    pinned.length ? [pinned, hl('div.relay-games__separator'), nonPinned] : nonPinned,
  );
};

const gamesList = (study: StudyCtrl, relay: RelayCtrl, pinned: boolean, cloudEval?: MultiCloudEval) => {
  const chapters = study.chapters.list.all();
  const roundPath = relay.roundPath();
  const showResults = study.multiBoard.showResults();
  const round = study.relay?.round;
  return chapters.length === 1 && chapters[0].name === 'Chapter 1'
    ? []
    : chapters.map((c, i) => {
        if (relay.players.pins.isChapterPinned(c) !== pinned) return;
        const clocks = renderClocks(c);
        const players = [c.players?.black, c.players?.white];
        if (c.orientation === 'black') {
          players.reverse();
          clocks.reverse();
        }
        const current = c.id === study.data.chapter.id && !relay.tourShow();
        return hl(
          `a.relay-game.relay-game--${c.id}`,
          {
            attrs: {
              ...gameLinkAttrs(roundPath, c),
              'data-n': i + 1,
            },
            class: { 'relay-game--current': current },
          },
          [
            showResults && cloudEval && verticalEvalGauge(c, c.orientation, cloudEval),
            hl(
              'span.relay-game__players',
              players.map((p, i) => {
                const playerColor: Color = (c.orientation === 'black' ? COLORS : COLORS.slice().reverse())[i];
                const coloredResult =
                  showResults &&
                  c.status &&
                  c.status !== '*' &&
                  playerColoredResult(c.status, playerColor, round);
                return hl(
                  'span.relay-game__player',
                  p
                    ? [
                        hl('span.mini-game__user', [
                          playerFedFlag(p.fed),
                          hl('span.name', [userTitle(p), p.name]),
                          pinned && relay.players.pins.isPlayerPinned(p) ? pinIcon() : undefined,
                        ]),
                        coloredResult
                          ? hl(coloredResult.tag, [coloredResult.points])
                          : showResults && hl('span', clocks[i]),
                      ]
                    : [hl('span.mini-game__user', hl('span.name', 'Unknown player'))],
                );
              }),
            ),
          ],
        );
      });
};

const renderClocks = (chapter: ChapterPreview) =>
  ['black', 'white'].map((color: Color) => renderClock(chapter, color));
