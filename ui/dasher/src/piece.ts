import { Chessground } from '@lichess-org/chessground';
import type { Api as CgApi } from '@lichess-org/chessground/api';
import type { Color } from '@lichess-org/chessground/types';
import { h, type VNode } from 'snabbdom';

import { type Toggle, toggle } from 'lib';
import { isTouchDevice, isMobile } from 'lib/device';
import { bind } from 'lib/view';
import { text as xhrText, form as xhrForm } from 'lib/xhr';

import type { DasherCtrl } from '@/ctrl';

import { type Dimension, PaneCtrl } from './interfaces';
import { header, moreButton } from './util';

type PiecesRole = {
  code: string;
  name: string;
};

export class PieceCtrl extends PaneCtrl {
  private previewGround?: CgApi;

  private readonly boardPreviewSize = 240;
  private readonly pieceRoles: readonly PiecesRole[] = [
    { code: 'R', name: 'Rook' },
    { code: 'N', name: 'Knight' },
    { code: 'B', name: 'Bishop' },
    { code: 'Q', name: 'Queen' },
    { code: 'K', name: 'King' },
    { code: 'P', name: 'Pawn' },
  ];

  featured: Record<Dimension, string[]> = { d2: [], d3: [] };
  more: Toggle;

  constructor(root: DasherCtrl) {
    super(root);
    this.more = toggle(false, root.redraw);
    for (const dim of ['d2', 'd3'] as Dimension[]) {
      this.featured[dim] = this.root.data.piece[dim].list.filter(set => set.featured).map(set => set.name);
    }
  }

  render(): VNode {
    return h('div.sub.piece.' + this.dimension, [
      header(i18n.site.pieceSet, () => this.close()),
      h(
        'div.list',
        this.pieceList.map(set =>
          h(
            'button.no-square',
            {
              key: set,
              attrs: { title: set, type: 'button' },
              hook: {
                insert: vnode => {
                  bind('click', () => this.set(set)).insert?.(vnode);
                  if (!isTouchDevice() && !isMobile()) {
                    this.initPowertip(vnode.elm as HTMLElement, set);
                  }
                },
                destroy: vnode => {
                  $.powerTip.destroy(vnode.elm);
                },
              },
              class: { active: this.dimData.current === set },
            },
            this.pieceRoles.map(role =>
              h('piece', {
                key: role.code,
                attrs: {
                  'aria-hidden': 'true',
                  style: this.pieceImageVars(set, role, this.is3d),
                },
              }),
            ),
          ),
        ),
      ),
      moreButton(this.more),
    ]);
  }

  private get pieceList(): string[] {
    const all = this.dimData.list.map(({ name }) => name);
    const visible = this.featured[this.dimension].slice();
    if (!visible.includes(this.dimData.current)) visible.push(this.dimData.current);
    return this.more() ? all : visible;
  }

  apply = (t: string = this.dimData.current): void => {
    this.dimData.current = t;
    document.body.dataset[this.is3d ? 'pieceSet3d' : 'pieceSet'] = t;
    if (!this.is3d) this.pieceVarRules(t, document.body);
  };

  private readonly initPowertip = (el: HTMLElement, pieceSet: string) => {
    $(el).powerTip({
      placement: 'w',
      popupId: 'piecePreview',
      defaultSize: [this.boardPreviewSize, this.boardPreviewSize],
      render: async () => {
        const popup = document.getElementById('piecePreview')!;
        this.previewGround?.destroy();
        popup.replaceChildren();

        const preview = document.createElement('div');
        preview.className = this.is3d ? 'is3d' : 'is2d';

        const previewBoard = document.createElement('div');
        previewBoard.className = 'preview-board';
        previewBoard.style.width = `${this.boardPreviewSize}px`;

        const board = document.createElement('div');
        board.className = 'cg-wrap';
        previewBoard.appendChild(board);
        preview.appendChild(previewBoard);
        popup.appendChild(preview);

        this.previewGround = Chessground(board, {
          fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR',
          orientation: 'white',
          viewOnly: true,
          coordinates: false,
          addPieceZIndex: this.is3d,
          drawable: { enabled: false, visible: false },
        });

        if (this.is3d) {
          this.set3DPieceImages(board, pieceSet);
        } else {
          this.pieceVarRules(pieceSet, preview);
        }
      },
    });
  };

  private get dimData() {
    return this.root.data.piece[this.dimension];
  }

  private readonly set = (t: string) => {
    this.apply(t);
    const field = `pieceSet${this.is3d ? '3d' : ''}`;
    xhrText(`/pref/${field}`, { body: xhrForm({ [field]: t }), method: 'post' }).catch(() =>
      site.announce({ msg: 'Failed to save piece set  preference' }),
    );
    this.redraw();
  };

  private pieceImage(set: string, color: Color, role: PiecesRole, is3d = false) {
    if (is3d)
      return `images/staunton/piece/${set}/${color === 'white' ? 'White' : 'Black'}-${role.name}${
        color === 'white' && role.name === 'Knight' && set === 'Staunton' ? '-Preview' : ''
      }.webp`;

    const code = `${color === 'white' ? 'w' : 'b'}${role.code}`;
    return `piece/${set}/${code}.${site.manifest.hashed[`piece/${set}/${code}.webp`] ? 'webp' : 'svg'}`;
  }

  private pieceImageVars(set: string, role: PiecesRole, is3d = false) {
    return `--piece-image-default:url(${site.asset.url(this.pieceImage(set, 'white', role, is3d))});
   --piece-image-light:url(${site.asset.url(this.pieceImage(set, 'black', role, is3d))})`;
  }

  private pieceVarRules(theme: string, element: HTMLElement) {
    for (const role of this.pieceRoles) {
      for (const color of ['white', 'black'] as const) {
        const url = site.asset.url(this.pieceImage(theme, color, role), { pathOnly: true });
        element.style.setProperty(`---${color}-${role.name.toLowerCase()}`, `url(${url})`);
      }
    }
  }

  private set3DPieceImages(board: HTMLElement, theme: string) {
    board.querySelectorAll<HTMLElement>('piece').forEach(piece => {
      const role = this.pieceRoles.find(({ name }) => piece.classList.contains(name.toLowerCase()));
      const color = piece.classList.contains('white') ? 'White' : 'Black';
      if (role)
        piece.style.backgroundImage = `url(${site.asset.url(
          `images/staunton/piece/${theme}/${color}-${role.name}.webp`,
        )})`;
    });
  }
}
