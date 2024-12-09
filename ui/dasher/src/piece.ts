import { h, type VNode } from 'snabbdom';
import { text as xhrText, form as xhrForm } from 'common/xhr';
import { header, elementScrollBarWidthSlowGuess } from './util';
import { bind } from 'common/snabbdom';
import { type DasherCtrl, PaneCtrl } from './interfaces';
import { pubsub } from 'common/pubsub';

type Piece = string;
type PieceDimData = { current: Piece; list: Piece[] };

export interface PieceData {
  d2: PieceDimData;
  d3: PieceDimData;
}

export class PieceCtrl extends PaneCtrl {
  constructor(root: DasherCtrl) {
    super(root);
  }

  render(): VNode {
    const maxHeight = window.innerHeight - 150; // safari vh brokenness
    const pieceSize = (222 - elementScrollBarWidthSlowGuess()) / 4;
    const pieceImage = (t: Piece) =>
      this.is3d
        ? `images/staunton/piece/${t}/White-Knight${t === 'Staunton' ? '-Preview' : ''}.png`
        : `piece/${t}/wN.svg`;

    return h('div.sub.piece.' + this.dimension, [
      header(i18n.site.pieceSet, () => this.close()),
      h(
        'div.list',
        { attrs: { style: `max-height:${maxHeight}px;` } },
        this.dimData.list.map((t: Piece) =>
          h(
            'button.no-square',
            {
              attrs: { title: t, type: 'button', style: `width: ${pieceSize}px; height: ${pieceSize}px` },
              hook: bind('click', () => this.set(t)),
              class: { active: this.dimData.current === t },
            },
            [h('piece', { attrs: { style: `background-image:url(${site.asset.url(pieceImage(t))})` } })],
          ),
        ),
      ),
    ]);
  }

  apply = (t: Piece = this.dimData.current): void => {
    this.dimData.current = t;
    document.body.dataset[this.is3d ? 'pieceSet3d' : 'pieceSet'] = t;
    if (!this.is3d) {
      const sprite = document.getElementById('piece-sprite') as HTMLLinkElement;
      sprite.href = site.asset.url(`piece-css/${t}.css`);
    }
    pubsub.emit('board.change', this.is3d);
  };

  private get dimData() {
    return this.root.data.piece[this.dimension];
  }

  private set = (t: Piece) => {
    this.apply(t);
    const field = `pieceSet${this.is3d ? '3d' : ''}`;
    xhrText(`/pref/${field}`, { body: xhrForm({ [field]: t }), method: 'post' }).catch(() =>
      site.announce({ msg: 'Failed to save piece set  preference' }),
    );
    this.redraw();
  };
}
