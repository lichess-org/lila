import { prop } from 'common';
import * as licon from 'common/licon';
import { bind, dataIcon, looseH as h } from 'common/snabbdom';
import { text as xhrText, url as xhrUrl } from 'common/xhr';
import { VNode } from 'snabbdom';
import { renderIndexAndMove } from '../view/moveView';
import { baseUrl } from '../view/util';
import { StudyChapterMeta, StudyData } from './interfaces';
import RelayCtrl from './relay/relayCtrl';
import AnalyseCtrl from '../ctrl';

function fromPly(ctrl: StudyShare): VNode {
  const renderedMove = renderIndexAndMove({ withDots: true, showEval: false }, ctrl.currentNode());
  return h(
    'div.ply-wrap',
    ctrl.onMainline() &&
      h('label.ply', [
        h('input', {
          attrs: { type: 'checkbox', checked: ctrl.withPly() },
          hook: bind('change', e => ctrl.withPly((e.target as HTMLInputElement).checked), ctrl.redraw),
        }),
        ...(renderedMove
          ? ctrl.trans.vdom('startAtX', h('strong', renderedMove))
          : [ctrl.trans.noarg('startAtInitialPosition')]),
      ]),
  );
}

export class StudyShare {
  withPly = prop(false);

  constructor(
    readonly data: StudyData,
    readonly currentChapter: () => StudyChapterMeta,
    readonly currentNode: () => Tree.Node,
    readonly onMainline: () => boolean,
    readonly bottomColor: () => Color,
    readonly relay: RelayCtrl | undefined,
    readonly redraw: () => void,
    readonly trans: Trans,
    readonly analyse: AnalyseCtrl,
  ) {}

  studyId = this.data.id;

  variantKey = this.data.chapter.setup.variant.key as VariantKey;
  chapter = this.currentChapter;
  isPrivate = () => this.data.visibility === 'private';
  cloneable = () => this.data.features.cloneable;
  shareable = () => this.data.features.shareable;
  gamebook = this.data.chapter.gamebook;
}

//This will be the helper function for the writePgnClipboard function
async function removeEngineComments(url: string): Promise<void> {
  const regex = /{[^}]*}/g;
  const pgn = await xhrText(url);
  const newPgn = pgn.replace(regex, '');
  return navigator.clipboard.writeText(newPgn);
}

async function writePgnClipboard(url: string, ctrl: StudyShare): Promise<void> {
  // Firefox does not support `ClipboardItem`
  console.log('Hello from writePgnClipboard');
  if (ctrl.analyse.excludeCommentsAndEngineAnalysis()) {
    return removeEngineComments(url);
  }
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
  h('button.button.copy', { attrs: { 'data-rel': rel, ...dataIcon(licon.Clipboard) } });

export function view(ctrl: StudyShare): VNode {
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
            ctrl.cloneable() &&
              h(
                'a.button.text',
                { attrs: { ...dataIcon(licon.StudyBoard), href: `/study/${studyId}/clone` } },
                ctrl.trans.noarg('cloneStudy'),
              ),
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
                  writePgnClipboard(`/study/${studyId}/${ctrl.chapter().id}.pgn`, ctrl).then(
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
                  href: xhrUrl(lichess.asset.baseUrl() + '/export/fen.gif', {
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
                    attrs: { readonly: true, value: `${baseUrl()}${path}` },
                  }),
                  copyButton(`study-share-${i18n}`),
                ]),
                pastable && fromPly(ctrl),
                pastable && isPrivate && youCanPasteThis(),
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
                  attrs: { readonly: true, value: ctrl.currentNode().fen },
                }),
                copyButton(`study-share-fen`),
              ]),
            ]),
          ]),
        ]
      : h('div', 'Sharing and exporting were disabled by the study owner.'),
  );
}
