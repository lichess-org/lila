import { looseH as h } from 'common/snabbdom';
import { StudyCtrl } from '../studyDeps';
import RelayCtrl from './relayCtrl';
import { userTitle } from 'common/userLink';
import { scrollToInnerSelector } from 'common';
import { renderClock, verticalEvalGauge } from '../multiBoard';
import { ChapterPreview } from '../interfaces';
import { gameLinkAttrs, gameLinksListener } from '../studyChapters';

export const gamesList = (study: StudyCtrl, relay: RelayCtrl) => {
  const chapters = study.chapters.list.all();
  const cloudEval = study.multiCloudEval.thisIfShowEval();
  const basePath = relay.roundPath();
  return h(
    `div.relay-games${cloudEval ? '.relay-games__eval' : ''}`,
    {
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
      : chapters.map(c => {
          const players = c.players || {
            white: { name: 'Unknown player' },
            black: { name: 'Unknown player' },
          };
          const status =
            !c.status || c.status == '*' ? renderClocks(c) : [c.status.slice(0, 1), c.status.slice(2, 3)];
          return h(
            `a.relay-game.relay-game--${c.id}`,
            {
              attrs: {
                ...gameLinkAttrs(basePath, c),
                'data-id': c.id,
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
                    h('player', [userTitle(p), p.name]),
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
