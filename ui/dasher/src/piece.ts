import { h, type VNode } from 'snabbdom';
import { text as xhrText, form as xhrForm } from 'lib/xhr';
import { header, elementScrollBarWidthSlowGuess, moreButton } from './util';
import { bind } from 'lib/snabbdom';
import { type DasherCtrl, PaneCtrl } from './interfaces';
import { pubsub } from 'lib/pubsub';
import { type Toggle, toggle } from 'lib';

export class PieceCtrl extends PaneCtrl {
  featured: { [key in 'd2' | 'd3']: string[] } = { d2: [], d3: [] };
  more: Toggle;

  constructor(root: DasherCtrl) {
    super(root);
    this.more = toggle(false, root.redraw);
    for (const dim of ['d2', 'd3'] as const) {
      this.featured[dim] = this.root.data.piece[dim].list.filter(t => t.featured).map(t => t.name);
    }
  }

  get pieceList(): string[] {
    const all = this.dimData.list.map(t => t.name);
    const visible = this.featured[this.dimension].slice();
    if (!visible.includes(this.dimData.current)) visible.push(this.dimData.current);
    return this.more() ? all : visible;
  }

  render(): VNode {
    const maxHeight = window.innerHeight - 150; // safari vh brokenness
    const pieceSize = (222 - elementScrollBarWidthSlowGuess()) / (this.more() ? 4 : 3);
    const pieceImage = (t: string) =>
      this.is3d
        ? `images/staunton/piece/${t}/White-Knight${t === 'Staunton' ? '-Preview' : ''}.png`
        : `piece/${t}/wN.svg`;

    return h('div.sub.piece.' + this.dimension, [
      header(i18n.site.pieceSet, () => this.close()),
      h(
        'div.list',
        { attrs: { style: `max-height:${maxHeight}px;` } },
        this.pieceList.map((t: string) =>
          h(
            'button.no-square',
            {
              key: t,
              attrs: { title: t, type: 'button', style: `width: ${pieceSize}px; height: ${pieceSize}px` },
              hook: bind('click', () => this.set(t)),
              class: { active: this.dimData.current === t },
            },
            [h('piece', { attrs: { style: `background-image:url(${site.asset.url(pieceImage(t))})` } })],
          ),
        ),
      ),
      moreButton(this.more),
    ]);
  }

  apply = (t: string = this.dimData.current): void => {
    this.dimData.current = t;
    document.body.dataset[this.is3d ? 'pieceSet3d' : 'pieceSet'] = t;
    if (!this.is3d) {
      pieceVarRules(t);
    }
    pubsub.emit('board.change', this.is3d);
  };

  private get dimData() {
    return this.root.data.piece[this.dimension];
  }

  private set = (t: string) => {
    this.apply(t);
    const field = `pieceSet${this.is3d ? '3d' : ''}`;
    xhrText(`/pref/${field}`, { body: xhrForm({ [field]: t }), method: 'post' }).catch(() =>
      site.announce({ msg: 'Failed to save piece set  preference' }),
    );
    this.redraw();
  };
}

const pieceVars = [
  ['---white-pawn', 'wP'],
  ['---black-pawn', 'bP'],
  ['---white-knight', 'wN'],
  ['---black-knight', 'bN'],
  ['---white-bishop', 'wB'],
  ['---black-bishop', 'bB'],
  ['---white-rook', 'wR'],
  ['---black-rook', 'bR'],
  ['---white-queen', 'wQ'],
  ['---black-queen', 'bQ'],
  ['---white-king', 'wK'],
  ['---black-king', 'bK'],
];

function pieceVarRules(theme: string) {
  for (const [varName, fileName] of pieceVars) {
    const url = site.asset.url(`piece/${theme}/${fileName}.svg`, { pathOnly: true });
    document.body.style.setProperty(varName, `url(${url})`);
  }
}
