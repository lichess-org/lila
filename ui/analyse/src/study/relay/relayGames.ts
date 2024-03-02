import { looseH as h } from 'common/snabbdom';
import { StudyChapterMetaExt } from '../interfaces';
import { StudyCtrl } from '../studyDeps';
import RelayCtrl from './relayCtrl';
import { gameLinkProps } from './relayTourView';
import { userTitle } from 'common/userLink';

export const gamesList = (study: StudyCtrl, relay: RelayCtrl) => {
  const chapters = study.chapters.list() as StudyChapterMetaExt[];
  return h(
    'div.relay-games',
    chapters.map(c => {
      const status =
        c.res === '*' ? ['clock', 'clock'] : c.res ? [c.res.slice(0, 1), c.res.slice(2, 3)] : ['-', '-'];
      return h(
        'a.relay-game',
        {
          ...gameLinkProps(relay.roundPath, study.setChapter, c),
          class: { 'relay-game--current': c.id === study.data.chapter.id },
        },
        [
          h('span.relay-game__gauge', [
            h('span.relay-game__gauge__black', {
              hook: {
                postpatch() {
                  // do the magic here, like in multiboard.ts
                },
              },
            }),
            h('tick'),
          ]),
          h(
            'span.relay-game__players',
            c.players.map((p, i) => {
              const s = status[i];
              return h('span.relay-game__player', [
                h('player', [p[0] && userTitle({ title: p[0] }), p[1]]),
                h(s == '1' ? 'good' : s == '0' ? 'bad' : 'status', s),
              ]);
            }),
          ),
        ],
      );
    }),
  );
};
