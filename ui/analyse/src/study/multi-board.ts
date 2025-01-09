import { MaybeVNodes, bind } from 'common/snabbdom';
import spinner from 'common/spinner';
import { Shogiground } from 'shogiground';
import { opposite } from 'shogiground/util';
import { usiToSquareNames } from 'shogiops/compat';
import { forsythToRole, roleToForsyth } from 'shogiops/sfen';
import { handRoles } from 'shogiops/variant/util';
import { VNode, h } from 'snabbdom';
import { ChapterPreview, ChapterPreviewPlayer, Position, StudyCtrl } from './interfaces';
import { multiBoard as xhrLoad } from './study-xhr';
import { loadChushogiPieceSprite, loadKyotoshogiPieceSprite } from 'common/assets';
import { i18n } from 'i18n';

export class MultiBoardCtrl {
  loading: boolean = false;
  page: number = 1;
  pager?: Paginator<ChapterPreview>;
  playing: boolean = false;

  constructor(
    readonly studyId: string,
    readonly redraw: () => void
  ) {}

  addNode(pos: Position, node: Tree.Node): void {
    const cp = this.pager && this.pager.currentPageResults.find(cp => cp.id == pos.chapterId);
    if (cp && cp.playing) {
      cp.sfen = node.sfen;
      cp.lastMove = node.usi;
      this.redraw();
    }
  }

  reload(onInsert?: boolean): void {
    if (this.pager && !onInsert) {
      this.loading = true;
      this.redraw();
    }
    xhrLoad(this.studyId, this.page, this.playing).then(p => {
      this.pager = p;
      if (p.nbPages < this.page) {
        if (!p.nbPages) this.page = 1;
        else this.setPage(p.nbPages);
      }
      this.loading = false;
      this.redraw();
    });
  }

  setPage = (page: number): void => {
    if (this.page != page) {
      this.page = page;
      this.reload();
    }
  };
  nextPage = (): void => this.setPage(this.page + 1);
  prevPage = (): void => this.setPage(this.page - 1);
  lastPage = (): void => {
    if (this.pager) this.setPage(this.pager.nbPages);
  };

  setPlaying = (v: boolean): void => {
    this.playing = v;
    this.reload();
  };
}

export function view(ctrl: MultiBoardCtrl, study: StudyCtrl): VNode | undefined {
  return h(
    'div.study__multiboard',
    {
      class: { loading: ctrl.loading, nopager: !ctrl.pager },
      hook: {
        insert() {
          ctrl.reload(true);
        },
      },
    },
    ctrl.pager ? renderPager(ctrl.pager, study) : [spinner()]
  );
}

function renderPager(pager: Paginator<ChapterPreview>, study: StudyCtrl): MaybeVNodes {
  const ctrl = study.multiBoard;
  if (pager.currentPageResults.some(p => p.variant.key === 'chushogi')) loadChushogiPieceSprite();
  if (pager.currentPageResults.some(p => p.variant.key === 'kyotoshogi'))
    loadKyotoshogiPieceSprite();
  return [
    h('div.top', [renderPagerNav(pager, ctrl), renderPlayingToggle(ctrl)]),
    h('div.now-playing', pager.currentPageResults.map(makePreview(study))),
  ];
}

function renderPlayingToggle(ctrl: MultiBoardCtrl): VNode {
  return h('label.playing', [
    h('input', {
      attrs: { type: 'checkbox' },
      hook: bind('change', e => {
        ctrl.setPlaying((e.target as HTMLInputElement).checked);
      }),
    }),
    i18n('study:playing'),
  ]);
}

function renderPagerNav(pager: Paginator<ChapterPreview>, ctrl: MultiBoardCtrl): VNode {
  const page = ctrl.page,
    from = Math.min(pager.nbResults, (page - 1) * pager.maxPerPage + 1),
    to = Math.min(pager.nbResults, page * pager.maxPerPage);
  return h('div.pager', [
    pagerButton(i18n('study:first'), 'W', () => ctrl.setPage(1), page > 1, ctrl),
    pagerButton(i18n('study:previous'), 'Y', ctrl.prevPage, page > 1, ctrl),
    h('span.page', `${from}-${to} / ${pager.nbResults}`),
    pagerButton(i18n('next'), 'X', ctrl.nextPage, page < pager.nbPages, ctrl),
    pagerButton(i18n('study:last'), 'V', ctrl.lastPage, page < pager.nbPages, ctrl),
  ]);
}

function pagerButton(
  text: string,
  icon: string,
  click: () => void,
  enable: boolean,
  ctrl: MultiBoardCtrl
): VNode {
  return h('button.fbt', {
    attrs: {
      'data-icon': icon,
      disabled: !enable,
      title: text,
    },
    hook: bind('mousedown', click, ctrl.redraw),
  });
}

function makePreview(study: StudyCtrl) {
  return (preview: ChapterPreview) => {
    const contents = preview.players
      ? [
          makePlayer(preview.players[opposite(preview.orientation)]),
          makeSg(preview),
          makePlayer(preview.players[preview.orientation]),
        ]
      : [h('div.name', preview.name), makeSg(preview), h('div.name')]; // empty name to keep board centered
    return h(
      'a.' + preview.id,
      {
        attrs: { title: preview.name },
        class: {
          active: !study.multiBoard.loading && study.vm.chapterId == preview.id,
        },
        hook: bind('mousedown', _ => study.setChapter(preview.id)),
      },
      contents
    );
  };
}

function makePlayer(player: ChapterPreviewPlayer): VNode {
  return h('div.player', [
    player.title ? `${player.title} ${player.name}` : player.name,
    player.rating && h('span', '' + player.rating),
  ]);
}

function usiToLastMove(lm?: string): Key[] | undefined {
  return lm ? usiToSquareNames(lm) : undefined;
}

function makeSg(preview: ChapterPreview): VNode {
  const variant = preview.variant.key;
  return h(
    `div.mini-board.v-${variant}`,
    h('div.sg-wrap', {
      hook: {
        insert(vnode) {
          const sg = Shogiground(
            {
              coordinates: { enabled: false },
              drawable: { enabled: false, visible: false },
              viewOnly: true,
              orientation: preview.orientation,
              sfen: {
                board: preview.sfen,
                hands:
                  preview.sfen && preview.sfen.split(' ').length > 2
                    ? preview.sfen.split(' ')[2]
                    : '',
              },
              hands: {
                inlined: variant !== 'chushogi',
                roles: handRoles(variant),
              },
              lastDests: usiToLastMove(preview.lastMove),
              forsyth: {
                fromForsyth: forsythToRole(variant),
                toForsyth: roleToForsyth(variant),
              },
            },
            { board: vnode.elm as HTMLElement }
          );
          vnode.data!.cp = { sg, sfen: preview.sfen };
        },
        postpatch(old, vnode) {
          if (old.data!.cp.sfen !== preview.sfen) {
            old.data!.cp.sg.set({
              sfen: {
                board: preview.sfen,
                hands:
                  preview.sfen && preview.sfen.split(' ').length > 2
                    ? preview.sfen.split(' ')[2]
                    : '',
              },
              lastDests: usiToLastMove(preview.lastMove),
            });
            old.data!.cp.sfen = preview.sfen;
          }
          vnode.data!.cp = old.data!.cp;
        },
      },
    })
  );
}
