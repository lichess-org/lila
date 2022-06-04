import { prop, Prop } from 'common';
import { bind } from 'common/snabbdom';
import { url as xhrUrl } from 'common/xhr';
import { h, VNode } from 'snabbdom';
import { renderIndexAndMove } from '../moveView';
import { baseUrl } from '../util';
import { StudyChapterMeta, StudyData } from './interfaces';
import RelayCtrl from './relay/relayCtrl';

export interface StudyShareCtrl {
  studyId: string;
  variantKey: VariantKey;
  chapter: () => StudyChapterMeta;
  bottomColor: () => Color;
  isPrivate(): boolean;
  currentNode: () => Tree.Node;
  onMainline: () => boolean;
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
    ctrl.onMainline()
      ? h('label.ply', [
          h('input', {
            attrs: { type: 'checkbox', checked: ctrl.withPly() },
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
      : null
  );
}

export function ctrl(
  data: StudyData,
  currentChapter: () => StudyChapterMeta,
  currentNode: () => Tree.Node,
  onMainline: () => boolean,
  bottomColor: () => Color,
  relay: RelayCtrl | undefined,
  redraw: () => void,
  trans: Trans
): StudyShareCtrl {
  const withPly = prop(false);
  return {
    studyId: data.id,
    variantKey: data.chapter.setup.variant.key as VariantKey,
    chapter: currentChapter,
    bottomColor,
    isPrivate() {
      return data.visibility === 'private';
    },
    currentNode,
    onMainline,
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
  const addPly = (path: string) =>
    ctrl.onMainline() ? (ctrl.withPly() ? `${path}#${ctrl.currentNode().ply}` : path) : `${path}#last`;
  const youCanPasteThis = () =>
    h('p.form-help.text', { attrs: { 'data-icon': '' } }, ctrl.trans.noarg('youCanPasteThisInTheForumToEmbed'));
  return h('div.study__share', [
    h('div.downloads', [
      ctrl.cloneable
        ? h(
            'a.button.text',
            {
              attrs: {
                'data-icon': '',
                href: `/study/${studyId}/clone`,
              },
            },
            ctrl.trans.noarg('cloneStudy')
          )
        : null,
      ctrl.relay &&
        h(
          'a.button.text',
          {
            attrs: {
              'data-icon': '',
              href: `/api/broadcast/${ctrl.relay.data.tour.id}.pgn`,
              download: true,
            },
          },
          ctrl.trans.noarg('downloadAllRounds')
        ),
      h(
        'a.button.text',
        {
          attrs: {
            'data-icon': '',
            href: ctrl.relay ? `${ctrl.relay.roundPath()}.pgn` : `/study/${studyId}.pgn`,
            download: true,
          },
        },
        ctrl.trans.noarg(ctrl.relay ? 'downloadAllGames' : 'studyPgn')
      ),
      h(
        'a.button.text',
        {
          attrs: {
            'data-icon': '',
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
            'data-icon': '',
            href: xhrUrl(`/export/gif/${ctrl.currentNode().fen.replace(/ /g, '_')}`, {
              color: ctrl.bottomColor(),
              lastMove: ctrl.currentNode().uci,
              variant: ctrl.variantKey,
            }),
            download: true,
          },
        },
        'Board'
      ),
      h(
        'a.button.text',
        {
          attrs: {
            'data-icon': '',
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
            ['broadcastUrl', ctrl.relay.tourPath()],
            ['currentRoundUrl', ctrl.relay.roundPath()],
            ['currentGameUrl', addPly(`${ctrl.relay.roundPath()}/${chapter.id}`), true],
          ]
        : [
            ['studyUrl', `/study/${studyId}`],
            ['currentChapterUrl', addPly(`/study/${studyId}/${chapter.id}`), true],
          ]
      ).map(([i18n, path, pastable]: [string, string, boolean]) =>
        h('div.form-group', [
          h('label.form-label', ctrl.trans.noarg(i18n)),
          h('input.form-control.autoselect', {
            attrs: {
              readonly: true,
              value: `${baseUrl()}${path}`,
            },
          }),
          ...(pastable ? [fromPly(ctrl), !isPrivate ? youCanPasteThis() : null] : []),
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
