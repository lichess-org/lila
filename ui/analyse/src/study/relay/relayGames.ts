import { StudyCtrl } from '../studyDeps';
import RelayCtrl from './relayCtrl';
import { userTitle } from 'common/userLink';
import { defined, scrollToInnerSelector } from 'common';
import { renderClock, verticalEvalGauge } from '../multiBoard';
import { ChapterPreview } from '../interfaces';
import { gameLinkAttrs, gameLinksListener } from '../studyChapters';
import { playerFed } from '../playerBars';
import { h } from 'snabbdom';

export const gamesList = (study: StudyCtrl, relay: RelayCtrl) => {
  const chapters = study.chapters.list.all();
  const cloudEval = study.multiCloudEval.thisIfShowEval();
  const basePath = relay.roundPath();
  return h(
    'div.relay-games',
    {
      class: { 'relay-games__eval': defined(cloudEval) },
      hook: {
        insert: gameLinksListener(study.setChapter),
        postpatch(old, vnode) {
          const currentId = study.data.chapter.id;
          if (old.data!.current !== currentId)
            scrollToInnerSelector(vnode.elm as HTMLElement, '.relay-game--current');
          vnode.data!.current = currentId;
        },
      },
    },
    chapters.length == 1 && chapters[0].name == 'Chapter 1'
      ? []
      : chapters.map((c, i) => {
          const players = c.players || {
            white: { name: 'Unknown player' },
            black: { name: 'Unknown player' },
          };
          const status =
            !c.status || c.status == '*' ? renderClocks(c) : [c.status.slice(2, 3), c.status.slice(0, 1)];
          return h(
            `a.relay-game.relay-game--${c.id}`,
            {
              attrs: {
                ...gameLinkAttrs(basePath, c),
                'data-n': i + 1,
              },
              class: { 'relay-game--current': c.id === study.data.chapter.id },
            },
            [
              cloudEval && verticalEvalGauge(c, cloudEval),
              h(
                'span.relay-game__players',
                [players.black, players.white].map((p, i) => {
                  const s = status[i];
                  return h('span.relay-game__player', [
                    h('span.mini-game__user', [
                      p.fed && playerFed(p.fed),
                      h('span.name', [userTitle(p), p.name]),
                    ]),
                    h(s == '1' ? 'good' : s == '0' ? 'bad' : 'status', [s]),
                  ]);
                }),
              ),
            ],
          );
        }),
  );
};

const renderClocks = (chapter: ChapterPreview) =>
  ['black', 'white'].map((color: Color) => renderClock(chapter, color));
