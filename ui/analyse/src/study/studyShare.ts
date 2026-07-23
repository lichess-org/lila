import { prop } from 'lib';
import { licon } from 'lib/licon';
import type { TreeNode } from 'lib/tree/types';
import { type VNode, bind, dataIcon, hl, copyMeInput, type MaybeVNode } from 'lib/view';
import { cmnToggleProp } from 'lib/view/cmn-toggle';
import { writeTextClipboard, url as xhrUrl } from 'lib/xhr';

import { renderIndexAndMove } from '@/view/components';
import { baseUrl } from '@/view/util';

import type { ChapterPreview, StudyDataFromServer } from './interfaces';
import type RelayCtrl from './relay/relayCtrl';
import { relayIframe } from './relay/relayTourView';

export class StudyShare {
  withPly = prop(false);

  constructor(
    readonly data: StudyDataFromServer,
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

function fromPly(ctrl: StudyShare): MaybeVNode {
  if (!ctrl.onMainline()) return undefined;
  const renderedMove = renderIndexAndMove(ctrl.currentNode(), false, false);
  return hl('label.url-start-at-ply', [
    cmnToggleProp({ id: 'study-share-start-position', prop: ctrl.withPly, redraw: ctrl.redraw }),
    ...(renderedMove.length
      ? i18n.study.startAtX.asArray(hl('strong', renderedMove))
      : [i18n.study.startAtInitialPosition]),
  ]);
}

function youCanPasteThis() {
  return hl(
    'p.form-help.text',
    { attrs: dataIcon(licon.InfoCircle) },
    i18n.study.youCanPasteThisInTheForumToEmbed,
  );
}

function copyChapterPgn(url: string, text: string) {
  return hl(
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
}

export function view(ctrl: StudyShare): VNode {
  const { studyId, relay } = ctrl;
  const chapter = ctrl.chapter();
  const isPrivate = ctrl.isPrivate();
  const currentNode = ctrl.currentNode();

  const addPly = (path: string) =>
    ctrl.onMainline() ? (ctrl.withPly() ? `${path}#${currentNode.ply}` : path) : `${path}#last`;

  return hl('div.study__share', [
    hl('div.downloads', [
      ctrl.cloneable() &&
        hl(
          'a.button.text',
          { attrs: { ...dataIcon(licon.StudyBoard), href: `/study/${studyId}/clone` } },
          i18n.study.cloneStudy,
        ),
      relay &&
        hl(
          'a.button.text',
          {
            attrs: {
              ...dataIcon(licon.Download),
              href: `/api/broadcast/${relay.data.tour.id}.pgn`,
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
            href: relay ? `${relay.roundPath()}.pgn` : `/study/${studyId}.pgn`,
            download: true,
          },
        },
        relay ? i18n.site.downloadAllGames : i18n.study.studyPgn,
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
        relay ? i18n.study.downloadGame : i18n.study.chapterPgn,
      ),
      copyChapterPgn(`/study/${studyId}/${chapter.id}.pgn`, i18n.study.copyChapterPgn),
      copyChapterPgn(
        `/study/${studyId}/${chapter.id}.pgn?clocks=false&comments=false&variations=false`,
        i18n.study.copyRawChapterPgn,
      ),
      hl(
        'a.button.text',
        {
          attrs: {
            ...dataIcon(licon.Download),
            href: xhrUrl(site.asset.baseUrl() + '/export/fen.gif', {
              fen: currentNode.fen,
              color: ctrl.bottomColor(),
              lastMove: currentNode.uci,
              variant: ctrl.variantKey,
              theme: document.body.dataset.board,
              piece: document.body.dataset.pieceSet,
            }),
            download: true,
          },
        },
        i18n.site.board,
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
      hl('button.button.text', {
        attrs: dataIcon(licon.StarOutline),
        on: { click: () => shortcutsDialog(ctrl) },
      }),
    ]),
    hl('form.form3', [
      (relay
        ? [
            [relay.data.tour.name, relay.tourPath()],
            [ctrl.data.name, relay.roundPath()],
            [i18n.broadcast.currentGameUrl, addPly(`${relay.roundPath()}/${chapter.id}`), true],
          ]
        : [
            [i18n.study.studyUrl, `/study/${studyId}`],
            [i18n.study.currentChapterUrl, addPly(`/study/${studyId}/${chapter.id}`), true],
          ]
      ).map(([text, path, pastable]: [string, string, boolean]) =>
        hl('div.form-group', [
          hl('label.form-label', text),
          copyMeInput(`${baseUrl()}${path}`, { inputAttrs: { readonly: true } }),
          pastable && fromPly(ctrl),
          pastable && isPrivate && youCanPasteThis(),
        ]),
      ),
      relay
        ? hl('div.form-group', [
            hl('label.form-label', 'Embed this particular game'),
            copyMeInput(relayIframe(`${relay.roundPath()}/${chapter.id}`), {
              inputAttrs: { readonly: true },
            }),
            hl(
              'a.form-help.text',
              { attrs: { ...dataIcon(licon.InfoCircle), href: `${relay.roundPath()}#overview` } },
              'More options for embedding a broadcast',
            ),
          ])
        : isPrivate || // study embed
          hl('div.form-group', [
            hl('div.form-label', [
              hl('label', i18n.study.embedInYourWebsite),
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
            copyMeInput(
              !isPrivate
                ? `<iframe ${
                    ctrl.gamebook ? 'width="320" height="320"' : 'width="600" height="371"'
                  } src="${baseUrl()}${addPly(
                    `/study/embed/${studyId}/${chapter.id}`,
                  )}" frameborder=0></iframe>`
                : i18n.study.onlyPublicStudiesCanBeEmbedded,
              { inputAttrs: { readonly: true, disabled: isPrivate } },
            ),
            fromPly(ctrl),
          ]),
    ]),
    hl('div.form-group', [
      hl('label.form-label', 'FEN'),
      copyMeInput(currentNode.fen, { inputAttrs: { readonly: true } }),
    ]),
  ]);
}

function shortcutsDialog(ctrl: StudyShare) {
  const shortcut = {
    name: `${ctrl.data.name} • ${ctrl.chapter().name}`,
    iconKey: 'StudyBoard',
    url: `/study/${ctrl.studyId}/${ctrl.chapter().id}`,
  };
  site.asset.loadEsm('lobby.shortcutsDialog', { init: { contextual: [shortcut] } });
}
