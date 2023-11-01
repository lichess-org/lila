import { prop, Prop } from 'common';
import * as licon from 'common/licon';
import { bind, dataIcon } from 'common/snabbdom';
import { text as xhrText, url as xhrUrl } from 'common/xhr';
import { h, VNode } from 'snabbdom';
import { renderIndexAndMove } from '../view/moveView';
import { baseUrl } from '../view/util';
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
  cloneable(): boolean;
  shareable(): boolean;
  redraw: () => void;
  trans: Trans;
  gamebook: boolean;
}

function fromPly(ctrl: StudyShareCtrl): VNode {
  const renderedMove = renderIndexAndMove(
    {
      withDots: true,
      showEval: false,
    },
    ctrl.currentNode(),
  );
  return h(
    'div.ply-wrap',
    ctrl.onMainline()
      ? h('label.ply', [
          h('input', {
            attrs: { type: 'checkbox', checked: ctrl.withPly() },
            hook: bind('change', e => ctrl.withPly((e.target as HTMLInputElement).checked), ctrl.redraw),
          }),
          ...(renderedMove
            ? ctrl.trans.vdom('startAtX', h('strong', renderedMove))
            : [ctrl.trans.noarg('startAtInitialPosition')]),
        ])
      : null,
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
  trans: Trans,
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
    cloneable: () => data.features.cloneable,
    shareable: () => data.features.shareable,
    redraw,
    trans,
    gamebook: data.chapter.gamebook,
  };
}

async function writePgnClipboard(url: string): Promise<void> {
  // Firefox does not support `ClipboardItem`
  if (typeof ClipboardItem === 'undefined') {
    const pgn = await xhrText(url);
    return navigator.clipboard.writeText(pgn);
  } else {
    const clipboardItem = new ClipboardItem({
      'text/plain': xhrText(url).then(pgn => new Blob([pgn], { type: 'text/plain' })),
    });
    return navigator.clipboard.write([clipboardItem]);
  }
}

const copyButton = (rel: string) =>
  h('button.button.copy', {
    attrs: {
      'data-rel': rel,
      ...dataIcon(licon.Clipboard),
    },
  });

export function view(ctrl: StudyShareCtrl): VNode {
  const studyId = ctrl.studyId,
    chapter = ctrl.chapter();
  const isPrivate = ctrl.isPrivate();
  const addPly = (path: string) =>
    ctrl.onMainline() ? (ctrl.withPly() ? `${path}#${ctrl.currentNode().ply}` : path) : `${path}#last`;
  const youCanPasteThis = () =>
    h(
      'p.form-help.text',
      { attrs: dataIcon(licon.InfoCircle) },
      ctrl.trans.noarg('youCanPasteThisInTheForumToEmbed'),
    );
  return h(
    'div.study__share',
    ctrl.shareable()
      ? [
          h('div.downloads', [
            ctrl.cloneable()
              ? h(
                  'a.button.text',
                  {
                    attrs: {
                      ...dataIcon(licon.StudyBoard),
                      href: `/study/${studyId}/clone`,
                    },
                  },
                  ctrl.trans.noarg('cloneStudy'),
                )
              : null,
            ctrl.relay &&
              h(
                'a.button.text',
                {
                  attrs: {
                    ...dataIcon(licon.Download),
                    href: `/api/broadcast/${ctrl.relay.data.tour.id}.pgn`,
                    download: true,
                  },
                },
                ctrl.trans.noarg('downloadAllRounds'),
              ),
            h(
              'a.button.text',
              {
                attrs: {
                  ...dataIcon(licon.Download),
                  href: ctrl.relay ? `${ctrl.relay.roundPath()}.pgn` : `/study/${studyId}.pgn`,
                  download: true,
                },
              },
              ctrl.trans.noarg(ctrl.relay ? 'downloadAllGames' : 'studyPgn'),
            ),
            h(
              'a.button.text',
              {
                attrs: {
                  ...dataIcon(licon.Download),
                  href: `/study/${studyId}/${chapter.id}.pgn`,
                  download: true,
                },
              },
              ctrl.trans.noarg(ctrl.relay ? 'downloadGame' : 'chapterPgn'),
            ),
            h(
              'a.button.text',
              {
                attrs: {
                  ...dataIcon(licon.Clipboard),
                  title: ctrl.trans.noarg('copyChapterPgnDescription'),
                  tabindex: '0',
                },
                hook: bind('click', async event => {
                  const iconFeedback = (success: boolean) => {
                    (event.target as HTMLElement).setAttribute(
                      'data-icon',
                      success ? licon.Checkmark : licon.X,
                    );
                    setTimeout(
                      () => (event.target as HTMLElement).setAttribute('data-icon', licon.Clipboard),
                      1000,
                    );
                  };
                  writePgnClipboard(`/study/${studyId}/${ctrl.chapter().id}.pgn`).then(
                    () => iconFeedback(true),
                    err => {
                      console.log(err);
                      iconFeedback(false);
                    },
                  );
                }),
              },
              ctrl.trans.noarg('copyChapterPgn'),
            ),
            h(
              'a.button.text',
              {
                attrs: {
                  ...dataIcon(licon.Download),
                  href: xhrUrl(document.body.getAttribute('data-asset-url') + '/export/fen.gif', {
                    fen: ctrl.currentNode().fen,
                    color: ctrl.bottomColor(),
                    lastMove: ctrl.currentNode().uci,
                    variant: ctrl.variantKey,
                    theme: document.body.dataset.boardTheme,
                    piece: document.body.dataset.pieceSet,
                  }),
                  download: true,
                },
              },
              'Board',
            ),
            h(
              'a.button.text',
              {
                attrs: {
                  ...dataIcon(licon.Download),
                  href: xhrUrl(`/study/${studyId}/${chapter.id}.gif`, {
                    theme: document.body.dataset.boardTheme,
                    piece: document.body.dataset.pieceSet,
                  }),
                  download: true,
                },
              },
              'GIF',
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
                h('div.form-control-with-clipboard', [
                  h(`input#study-share-${i18n}.form-control.copyable.autoselect`, {
                    attrs: {
                      readonly: true,
                      value: `${baseUrl()}${path}`,
                    },
                  }),
                  copyButton(`study-share-${i18n}`),
                ]),
                ...(pastable ? [fromPly(ctrl), !isPrivate ? youCanPasteThis() : null] : []),
              ]),
            ),
            h(
              'div.form-group',
              [
                h('label.form-label', ctrl.trans.noarg('embedInYourWebsite')),
                h('div.form-control-with-clipboard', [
                  h('input#study-share-embed.form-control.copyable.autoselect', {
                    attrs: {
                      readonly: true,
                      disabled: isPrivate,
                      value: !isPrivate
                        ? `<iframe ${
                            ctrl.gamebook ? 'width="320" height="320"' : 'width="600" height="371"'
                          } src="${baseUrl()}${addPly(
                            `/study/embed/${studyId}/${chapter.id}`,
                          )}" frameborder=0></iframe>`
                        : ctrl.trans.noarg('onlyPublicStudiesCanBeEmbedded'),
                    },
                  }),
                  copyButton(`study-share-embed`),
                ]),
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
                            ...dataIcon(licon.InfoCircle),
                          },
                        },
                        ctrl.trans.noarg('readMoreAboutEmbedding'),
                      ),
                    ]
                  : [],
              ),
            ),
            h('div.form-group', [
              h('label.form-label', 'FEN'),
              h('div.form-control-with-clipboard', [
                h('input#study-share-fen.form-control.copyable.autoselect', {
                  attrs: {
                    readonly: true,
                    value: ctrl.currentNode().fen,
                  },
                }),
                copyButton(`study-share-fen`),
              ]),
            ]),
          ]),
        ]
      : h('div', 'Sharing and exporting were disabled by the study owner.'),
  );
}
