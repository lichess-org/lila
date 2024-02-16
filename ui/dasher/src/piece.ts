import { h, VNode } from 'snabbdom';
import * as xhr from 'common/xhr';
import { header, Close } from './util';
import { bind, Redraw } from 'common/snabbdom';
import { elementScrollBarWidth } from 'common/scroll';

type Piece = string;

interface PieceDimData {
  current: Piece;
  list: Piece[];
}

export interface PieceData {
  d2: PieceDimData;
  d3: PieceDimData;
}

export class PieceCtrl {
  constructor(
    private readonly data: PieceData,
    readonly trans: Trans,
    readonly dimension: () => keyof PieceData,
    readonly redraw: Redraw,
    readonly close: Close,
  ) {}
  dimensionData = () => this.data[this.dimension()];
  set = (t: Piece) => {
    const d = this.dimensionData();
    d.current = t;
    applyPiece(t, d.list, this.dimension() === 'd3');
    const field = `pieceSet${this.dimension() === 'd3' ? '3d' : ''}`;
    xhr
      .text(`/pref/${field}`, { body: xhr.form({ [field]: t }), method: 'post' })
      .catch(() => lichess.announce({ msg: 'Failed to save piece set  preference' }));
    this.redraw();
  };
}

export function view(ctrl: PieceCtrl): VNode {
  const d = ctrl.dimensionData();
  const maxHeight = window.innerHeight - 150; // safari vh brokenness
  return h('div.sub.piece.' + ctrl.dimension(), [
    header(ctrl.trans.noarg('pieceSet'), () => ctrl.close()),
    h(
      'div.list',
      { attrs: { style: `max-height:${maxHeight}px;` } },
      d.list.map(pieceView(d.current, ctrl.set, ctrl.dimension() == 'd3')),
    ),
  ]);
}

function pieceImage(t: Piece, is3d: boolean) {
  if (is3d) {
    const preview = t == 'Staunton' ? '-Preview' : '';
    return `images/staunton/piece/${t}/White-Knight${preview}.png`;
  }
  return `piece/${t}/wN.svg`;
}

const pieceView = (current: Piece, set: (t: Piece) => void, is3d: boolean) => (t: Piece) => {
  const pieceSize = (225 - elementScrollBarWidth()) / 3;
  return h(
    'button.no-square',
    {
      attrs: { title: t, type: 'button', style: `width: ${pieceSize}px; height: ${pieceSize}px` },
      hook: bind('click', () => set(t)),
      class: { active: current === t },
    },
    [h('piece', { attrs: { style: `background-image:url(${lichess.asset.url(pieceImage(t, is3d))})` } })],
  );
};
function applyPiece(t: Piece, list: Piece[], is3d: boolean) {
  if (is3d) {
    $('body').removeClass(list.join(' ')).addClass(t);
  } else {
    const sprite = document.getElementById('piece-sprite') as HTMLLinkElement;
    sprite.href = sprite.href.replace(/[\w-]+(\.external|)\.css/, t + '$1.css');
    document.body.dataset.pieceSet = t;
  }
  lichess.pubsub.emit('theme.change');
}
