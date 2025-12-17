import type { StudyCtrl } from '../studyDeps';
import type RelayCtrl from './relayCtrl';
import { userTitle } from 'lib/view/userLink';
import { defined, scrollToInnerSelector } from 'lib';
import { renderClock, verticalEvalGauge } from '../multiBoard';
import type { ChapterPreview } from '../interfaces';
import { gameLinkAttrs } from '../studyChapters';
import { playerFedFlag } from '../playerBars';
import { hl } from 'lib/view';
import { playerColoredResult } from './customScoreStatus';
import { COLORS } from 'chessops';

export const gamesList = (study: StudyCtrl, relay: RelayCtrl) => {
  const chapters = study.chapters.list.all();
  const cloudEval = study.multiCloudEval?.thisIfShowEval();
  const roundPath = relay.roundPath();
  const showResults = study.multiBoard.showResults();
  const round = study.relay?.round;
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
    chapters.length === 1 && chapters[0].name === 'Chapter 1'
      ? []
      : chapters.map((c, i) => {
          const clocks = renderClocks(c);
          const players = [c.players?.black, c.players?.white];
          if (c.orientation === 'black') {
            players.reverse();
            clocks.reverse();
          }
          return hl(
            `a.relay-game.relay-game--${c.id}`,
            {
              attrs: {
                ...gameLinkAttrs(roundPath, c),
                'data-n': i + 1,
              },
              class: { 'relay-game--current': c.id === study.data.chapter.id },
            },
            [
              showResults && cloudEval && verticalEvalGauge(c, cloudEval),
              hl(
                'span.relay-game__players',
                players.map((p, i) => {
                  const playerColor: Color = (c.orientation === 'black' ? COLORS : COLORS.slice().reverse())[
                    i
                  ];
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
                          ]),
                          coloredResult
                            ? hl(`${coloredResult.tag}`, [coloredResult.points])
                            : showResults && hl('span', clocks[i]),
                        ]
                      : [hl('span.mini-game__user', hl('span.name', 'Unknown player'))],
                  );
                }),
              ),
            ],
          );
        }),
  );
};

const renderClocks = (chapter: ChapterPreview) =>
  ['black', 'white'].map((color: Color) => renderClock(chapter, color));
