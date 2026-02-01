import { prop } from 'lib';
import * as licon from 'lib/licon';
import { type VNode, bind, dataIcon, hl, copyMeInput } from 'lib/view';
import { writeTextClipboard, url as xhrUrl } from 'lib/xhr';
import { renderIndexAndMove } from '../view/components';
import { baseUrl } from '../view/util';
import type { ChapterPreview, StudyData } from './interfaces';
import type RelayCtrl from './relay/relayCtrl';
import type { TreeNode } from 'lib/tree/types';

function fromPly(ctrl: StudyShare): VNode {
  const renderedMove = renderIndexAndMove(ctrl.currentNode(), false, false);
  return hl(
    'div.ply-wrap',
    ctrl.onMainline() &&
      hl('label.ply', [
        hl('div.switch', { attrs: { role: 'button' } }, [
          hl('input#study-share-start-position.cmn-toggle.cmn-toggle--subtle', {
            attrs: { type: 'checkbox', checked: ctrl.withPly() },
            hook: bind('change', e => ctrl.withPly((e.target as HTMLInputElement).checked), ctrl.redraw),
          }),
          hl('label', { attrs: { for: 'study-share-start-position' } }),
        ]),
        renderedMove
          ? i18n.study.startAtX.asArray(hl('strong', renderedMove))
          : [i18n.study.startAtInitialPosition],
      ]),
  );
}

export class StudyShare {
  withPly = prop(false);

  constructor(
    readonly data: StudyData,
    readonly currentChapter: () => ChapterPreview,
    readonly currentNode: () => TreeNode,
    readonly onMainline: () => boolean,
    readonly bottomColor: () => Color,
    readonly relay: RelayCtrl | undefined,
    readonly redraw: () => void,
  ) {}

  studyId = this.data.id;

  variantKey = this.data.chapter.setup.variant.key;

  chapter = this.currentChapter;
  isPrivate = () => this.data.visibility === 'private';
  cloneable = () => this.data.features.cloneable;
  shareable = () => this.data.features.shareable;
  gamebook = this.data.chapter.gamebook;
}

export function view(ctrl: StudyShare): VNode {
  const studyId = ctrl.studyId,
    chapter = ctrl.chapter();
  const isPrivate = ctrl.isPrivate();
  const addPly = (path: string) =>
    ctrl.onMainline() ? (ctrl.withPly() ? `${path}#${ctrl.currentNode().ply}` : path) : `${path}#last`;
  const youCanPasteThis = () =>
    hl(
      'p.form-help.text',
      { attrs: dataIcon(licon.InfoCircle) },
      i18n.study.youCanPasteThisInTheForumToEmbed,
    );
  const copyChapterPgn = (url: string, text: string) =>
    hl(
      'a.button.text',
      {
        attrs: {
          ...dataIcon(licon.Clipboard),
          tabindex: '0',
          'data-url': url,
        },
        hook: bind('click', async event => {
          const target = event.target as HTMLElement;
          const url = target.dataset['url']!;
          const iconFeedback = (success: boolean) => {
            target.setAttribute('data-icon', success ? licon.Checkmark : licon.X);
            setTimeout(() => target.setAttribute('data-icon', licon.Clipboard), 1000);
          };
          writeTextClipboard(url).then(
            () => iconFeedback(true),
            err => {
              console.log(err);
              iconFeedback(false);
            },
          );
        }),
      },
      text,
    );
  return hl(
    'div.study__share',
    ctrl.shareable()
      ? [
          hl('div.downloads', [
            ctrl.cloneable() &&
              hl(
                'a.button.text',
                { attrs: { ...dataIcon(licon.StudyBoard), href: `/study/${studyId}/clone` } },
                i18n.study.cloneStudy,
              ),
            ctrl.relay &&
              hl(
                'a.button.text',
                {
                  attrs: {
                    ...dataIcon(licon.Download),
                    href: `/api/broadcast/${ctrl.relay.data.tour.id}.pgn`,
                    download: true,
                  },
                },
                i18n.broadcast.downloadAllRounds,
              ),
            hl(
              'a.button.text',
              {
                attrs: {
                  ...dataIcon(licon.Download),
                  href: ctrl.relay ? `${ctrl.relay.roundPath()}.pgn` : `/study/${studyId}.pgn`,
                  download: true,
                },
              },
              ctrl.relay ? i18n.site.downloadAllGames : i18n.study.studyPgn,
            ),
            hl(
              'a.button.text',
              {
                attrs: {
                  ...dataIcon(licon.Download),
                  href: `/study/${studyId}/${chapter.id}.pgn`,
                  download: true,
                },
              },
              ctrl.relay ? i18n.study.downloadGame : i18n.study.chapterPgn,
            ),
            copyChapterPgn(`/study/${studyId}/${ctrl.chapter().id}.pgn`, i18n.study.copyChapterPgn),
            copyChapterPgn(
              `/study/${studyId}/${ctrl.chapter().id}.pgn?clocks=false&comments=false&variations=false`,
              i18n.study.copyRawChapterPgn,
            ),
            hl(
              'a.button.text',
              {
                attrs: {
                  ...dataIcon(licon.Download),
                  href: xhrUrl(site.asset.baseUrl() + '/export/fen.gif', {
                    fen: ctrl.currentNode().fen,
                    color: ctrl.bottomColor(),
                    lastMove: ctrl.currentNode().uci,
                    variant: ctrl.variantKey,
                    theme: document.body.dataset.board,
                    piece: document.body.dataset.pieceSet,
                  }),
                  download: true,
                },
              },
              'Board',
            ),
            hl(
              'a.button.text',
              {
                attrs: {
                  ...dataIcon(licon.Download),
                  href: xhrUrl(`/study/${studyId}/${chapter.id}.gif`, {
                    theme: document.body.dataset.board,
                    piece: document.body.dataset.pieceSet,
                    showGlyphs: true,
                  }),
                  download: true,
                },
              },
              'GIF',
            ),
          ]),
          hl('form.form3', [
            (ctrl.relay
              ? [
                  [ctrl.relay.data.tour.name, ctrl.relay.tourPath()],
                  [ctrl.data.name, ctrl.relay.roundPath()],
                  [i18n.broadcast.currentGameUrl, addPly(`${ctrl.relay.roundPath()}/${chapter.id}`), true],
                ]
              : [
                  [i18n.study.studyUrl, `/study/${studyId}`],
                  [i18n.study.currentChapterUrl, addPly(`/study/${studyId}/${chapter.id}`), true],
                ]
            ).map(([text, path, pastable]: [string, string, boolean]) =>
              hl('div.form-group', [
                hl('label.form-label', text),
                copyMeInput(`${baseUrl()}${path}`),
                pastable && fromPly(ctrl),
                pastable && isPrivate && youCanPasteThis(),
              ]),
            ),
            isPrivate ||
              hl('div.form-group', [
                hl('label.form-label', i18n.study.embedInYourWebsite),
                copyMeInput(
                  !isPrivate
                    ? `<iframe ${
                        ctrl.gamebook ? 'width="320" height="320"' : 'width="600" height="371"'
                      } src="${baseUrl()}${addPly(
                        `/study/embed/${studyId}/${chapter.id}`,
                      )}" frameborder=0></iframe>`
                    : i18n.study.onlyPublicStudiesCanBeEmbedded,
                  { disabled: isPrivate },
                ),
                fromPly(ctrl),
                hl(
                  'a.form-help.text',
                  {
                    attrs: {
                      href: '/developers#embed-study',
                      target: '_blank',
                      ...dataIcon(licon.InfoCircle),
                    },
                  },
                  i18n.study.readMoreAboutEmbedding,
                ),
              ]),
          ]),
          hl('div.form-group', [hl('label.form-label', 'FEN'), copyMeInput(ctrl.currentNode().fen)]),
        ]
      : hl('div', 'Sharing and exporting were disabled by the study owner.'),
  );
}
