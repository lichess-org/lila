import { h, VNode } from 'snabbdom';
import { bind, baseUrl } from '../util';
import { prop, Prop } from 'common';
import { renderIndexAndMove } from '../moveView';
import { StudyData, StudyChapterMeta } from './interfaces';
import RelayCtrl from './relay/relayCtrl';

export interface StudyShareCtrl {
  studyId: string;
  chapter: () => StudyChapterMeta;
  isPrivate(): boolean;
  currentNode: () => Tree.Node;
  withPly: Prop<boolean>;
  relay: RelayCtrl | undefined;
  cloneable: boolean;
  redraw: () => void;
  trans: Trans;
}

function fromPly(ctrl: StudyShareCtrl): VNode {
  const renderedMove = renderIndexAndMove(
    {
      withDots: true,
      showEval: false,
    },
    ctrl.currentNode()
  );
  return h(
    'div.ply-wrap',
    h('label.ply', [
      h('input', {
        attrs: { type: 'checkbox' },
        hook: bind(
          'change',
          e => {
            ctrl.withPly((e.target as HTMLInputElement).checked);
          },
          ctrl.redraw
        ),
      }),
      ...(renderedMove
        ? ctrl.trans.vdom('startAtX', h('strong', renderedMove))
        : [ctrl.trans.noarg('startAtInitialPosition')]),
    ])
  );
}

export function ctrl(
  data: StudyData,
  currentChapter: () => StudyChapterMeta,
  currentNode: () => Tree.Node,
  relay: RelayCtrl | undefined,
  redraw: () => void,
  trans: Trans
): StudyShareCtrl {
  const withPly = prop(false);
  return {
    studyId: data.id,
    chapter: currentChapter,
    isPrivate() {
      return data.visibility === 'private';
    },
    currentNode,
    withPly,
    relay,
    cloneable: data.features.cloneable,
    redraw,
    trans,
  };
}

export function view(ctrl: StudyShareCtrl): VNode {
  const studyId = ctrl.studyId,
    chapter = ctrl.chapter();
  const isPrivate = ctrl.isPrivate();
  const addPly = (path: string) => (ctrl.withPly() ? `${path}#${ctrl.currentNode().ply}` : path);
  return h('div.study__share', [
    h('div.downloads', [
      ctrl.cloneable
        ? h(
            'a.button.text',
            {
              attrs: {
                'data-icon': '4',
                href: `/study/${studyId}/clone`,
              },
            },
            ctrl.trans.noarg('cloneStudy')
          )
        : null,
      h(
        'a.button.text',
        {
          attrs: {
            'data-icon': 'x',
            href: `/study/${studyId}.pgn`,
            download: true,
          },
        },
        ctrl.trans.noarg(ctrl.relay ? 'downloadAllGames' : 'studyPgn')
      ),
      h(
        'a.button.text',
        {
          attrs: {
            'data-icon': 'x',
            href: `/study/${studyId}/${chapter.id}.pgn`,
            download: true,
          },
        },
        ctrl.trans.noarg(ctrl.relay ? 'downloadGame' : 'chapterPgn')
      ),
      h(
        'a.button.text',
        {
          attrs: {
            'data-icon': 'x',
            href: `/study/${studyId}/${chapter.id}.gif`,
            download: true,
          },
        },
        'GIF'
      ),
    ]),
    h('form.form3', [
      ...(ctrl.relay
        ? [
            ['broadcastUrl', `${ctrl.relay.tourPath()}`],
            ['currentRoundUrl', `${ctrl.relay.roundPath()}`],
            ['currentGameUrl', `${ctrl.relay.roundPath()}/${chapter.id}`],
          ]
        : [
            ['studyUrl', `/study/${studyId}`],
            ['currentChapter', addPly(`/study/${studyId}/${chapter.id}`), true],
          ]
      ).map(([i18n, path, isFull]: [string, string, boolean]) =>
        h('div.form-group', [
          h('label.form-label', ctrl.trans.noarg(i18n)),
          h('input.form-control.autoselect', {
            attrs: {
              readonly: true,
              value: `${baseUrl()}${path}`,
            },
          }),
          ...(isFull
            ? [
                fromPly(ctrl),
                !isPrivate
                  ? h(
                      'p.form-help.text',
                      {
                        attrs: { 'data-icon': '' },
                      },
                      ctrl.trans.noarg('youCanPasteThisInTheForumToEmbed')
                    )
                  : null,
              ]
            : []),
        ])
      ),
      h(
        'div.form-group',
        [
          h('label.form-label', ctrl.trans.noarg('embedInYourWebsite')),
          h('input.form-control.autoselect', {
            attrs: {
              readonly: true,
              disabled: isPrivate,
              value: !isPrivate
                ? `<iframe width=600 height=371 src="${baseUrl()}${addPly(
                    `/study/embed/${studyId}/${chapter.id}`
                  )}" frameborder=0></iframe>`
                : ctrl.trans.noarg('onlyPublicStudiesCanBeEmbedded'),
            },
          }),
        ].concat(
          !isPrivate
            ? [
                fromPly(ctrl),
                h(
                  'a.form-help.text',
                  {
                    attrs: {
                      href: '/developers#embed-study',
                      target: '_blank',
                      rel: 'noopener',
                      'data-icon': '',
                    },
                  },
                  ctrl.trans.noarg('readMoreAboutEmbedding')
                ),
              ]
            : []
        )
      ),
      h('div.form-group', [
        h('label.form-label', 'FEN'),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            value: ctrl.currentNode().fen,
          },
        }),
      ]),
    ]),
  ]);
}
