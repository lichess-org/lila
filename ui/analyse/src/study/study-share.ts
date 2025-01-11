import { type Prop, prop } from 'common/common';
import { bind } from 'common/snabbdom';
import { i18n, i18nVdom } from 'i18n';
import { type VNode, h } from 'snabbdom';
import { renderIndexAndMove } from '../move-view';
import { baseUrl } from '../util';
import type { StudyChapterMeta, StudyData } from './interfaces';

interface StudyShareCtrl {
  studyId: string;
  chapter: () => StudyChapterMeta;
  isPrivate(): boolean;
  currentNode: () => Tree.Node;
  onMainline: () => boolean;
  withPly: Prop<boolean>;
  cloneable: boolean;
  offset: number;
  gameId?: string;
  redraw: () => void;
}

function fromPly(ctrl: StudyShareCtrl): VNode {
  const renderedMove = renderIndexAndMove(
    {
      variant: ctrl.chapter().variant,
      withDots: true,
      showEval: false,
      offset: ctrl.offset,
    },
    ctrl.currentNode(),
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
              ctrl.redraw,
            ),
          }),
          ...(renderedMove
            ? i18nVdom('study:startAtX', h('strong', renderedMove))
            : [i18n('study:startAtInitialPosition')]),
        ])
      : null,
  );
}

export function ctrl(
  data: StudyData,
  currentChapter: () => StudyChapterMeta,
  currentNode: () => Tree.Node,
  onMainline: () => boolean,
  redraw: () => void,
  offset: number,
): StudyShareCtrl {
  const withPly = prop(false);
  return {
    studyId: data.id,
    chapter: currentChapter,
    isPrivate() {
      return data.visibility === 'private';
    },
    currentNode,
    onMainline,
    withPly,
    cloneable: data.features.cloneable,
    redraw,
    offset,
    gameId: data.chapter.gameLength ? data.chapter.setup.gameId : undefined,
  };
}

export function view(ctrl: StudyShareCtrl): VNode {
  const studyId = ctrl.studyId;
  const chapter = ctrl.chapter();
  let fullUrl = `${baseUrl()}/study/${studyId}/${chapter.id}`;
  let embedUrl = `${baseUrl()}/study/embed/${studyId}/${chapter.id}`;
  const isPrivate = ctrl.isPrivate();
  if (ctrl.withPly() && ctrl.onMainline()) {
    const p = ctrl.currentNode().ply;
    fullUrl += `#${p}`;
    embedUrl += `#${p}`;
  }
  return h('div.study__share', [
    h('form.form3', [
      h('div.form-group', [
        h('label.form-label', i18n('study:studyUrl')),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            value: `${baseUrl()}/study/${studyId}`,
          },
        }),
      ]),
      h('div.form-group', [
        h('label.form-label', i18n('study:currentChapterUrl')),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            value: fullUrl,
          },
        }),
        fromPly(ctrl),
        !isPrivate
          ? h(
              'p.form-help.text',
              {
                attrs: { 'data-icon': '' },
              },
              i18n('study:youCanPasteThisInTheForumToEmbed'),
            )
          : null,
      ]),
      ctrl.gameId
        ? h('div.form-group', [
            h('label.form-label', i18n('study:currentGameUrl')),
            h('input.form-control.autoselect', {
              attrs: {
                readonly: true,
                value: `${baseUrl()}/${ctrl.gameId}`,
              },
            }),
          ])
        : null,
      h(
        'div.form-group',
        [
          h('label.form-label', i18n('study:embedInYourWebsite')),
          h('input.form-control.autoselect', {
            attrs: {
              readonly: true,
              disabled: isPrivate,
              value: !isPrivate
                ? `<iframe width=600 height=371 src="${embedUrl}" frameborder=0></iframe>`
                : i18n('study:onlyPublicStudiesCanBeEmbedded'),
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
                      'data-icon': '',
                    },
                  },
                  i18n('study:readMoreAboutEmbedding'),
                ),
              ]
            : [],
        ),
      ),
      h('div.form-group', [
        h('label.form-label', 'SFEN'),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            value: ctrl.currentNode().sfen,
          },
        }),
      ]),
    ]),
    h('div.downloads', [
      h(
        'a.button.text',
        {
          attrs: {
            'data-icon': 'x',
            href: `/study/${studyId}/${chapter.id}.kif`,
            download: true,
          },
        },
        i18n('study:chapterKif'),
      ),
      'standard' === chapter.variant
        ? h(
            'a.button.text',
            {
              attrs: {
                'data-icon': 'x',
                href: `/study/${studyId}/${chapter.id}.csa`,
                download: true,
              },
            },
            i18n('study:chapterCsa'),
          )
        : null,
      ctrl.cloneable
        ? h(
            'a.button.text',
            {
              attrs: {
                'data-icon': '4',
                href: `/study/${studyId}/clone`,
              },
            },
            i18n('study:cloneStudy'),
          )
        : null,
      'standard' === chapter.variant
        ? h(
            'a.button.text',
            {
              attrs: {
                'data-icon': 'x',
                href: `/study/${studyId}/${chapter.id}.gif`,
                download: true,
              },
            },
            'GIF',
          )
        : null,
    ]),
  ]);
}
