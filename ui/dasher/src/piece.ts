import { h, VNode } from 'snabbdom';
import * as xhr from 'common/xhr';
import { header, Close } from './util';
import { bind, Redraw } from 'common/snabbdom';
import debounce from 'common/debounce';
import * as licon from 'common/licon';

type Piece = string;

interface PieceDimData {
  current: Piece;
  list: Piece[];
}

export interface PieceData {
  image: string;
  d2: PieceDimData;
  d3: PieceDimData;
}

export class PieceCtrl {
  constructor(
    private readonly data: PieceData,
    readonly trans: Trans,
    readonly dimension: () => 'd2' | 'd3',
    readonly redraw: Redraw,
    readonly close: Close,
  ) {}
  dimensionData = () => this.data[this.dimension()];
  set = (t: Piece) => {
    const d = this.dimensionData();
    d.current = t;
    applyPiece(t, d.list, this.dimension() === 'd3', this.data.image);
    const field = `pieceSet${this.dimension() === 'd3' ? '3d' : ''}`;
    xhr
      .text(`/pref/${field}`, { body: xhr.form({ [field]: t }), method: 'post' })
      .catch(() => lichess.announce({ msg: 'Failed to save piece set preference' }));
    this.redraw();
  };

  getImage = () => this.data.image;
  setImage = (i: string) => {
    this.data.image = i;
    xhr
      .textRaw('/pref/pieceSetImg', { body: xhr.form({ pieceSetImg: i }), method: 'post' })
      .then(res => (res.ok ? res.text() : Promise.reject(res.text())))
      .catch(err => err.then((msg: any) => lichess.announce({ msg: `Failed to save piece set preference: ${msg}` })));
    applyPiece('custom', this.data.d2.list, false, i);
    this.redraw();
  };
}

export function view(ctrl: PieceCtrl): VNode {
  const d = ctrl.dimensionData();

  return h('div.sub.piece.' + ctrl.dimension(), [
    header(ctrl.trans.noarg('pieceSet'), () => ctrl.close()),
    h('div.list', d.list.map(pieceView(d.current, ctrl.set, ctrl.dimension() == 'd3'))),
    h('div.selector.large',
      h(
        'button.text',
        {
          class: { active: d.current === 'custom' },
          attrs: { 'data-icon': licon.Checkmark, type: 'button' },
          hook: bind('click', () => ctrl.set('custom')),
        },
        'Custom',
      ),
    ),
    d.current === 'custom' ? imageInput(ctrl) : undefined,
  ]);
}

function imageInput(ctrl: PieceCtrl) {
  return h('div.image', [
    h('p', 'Piece set sprite URL:'),
    h('input', {
      attrs: { type: 'text', placeholder: 'https://', value: ctrl.getImage() },
      hook: {
        insert: vnode => {
          const elm = vnode.elm as HTMLElement;
          elm.focus();
          $(elm).on(
            'change keyup paste',
            debounce(function (this: HTMLInputElement) {
              const url = this.value.trim();
              if (
                (url.startsWith('https://') || url.startsWith('//')) &&
                url.length >= 10 &&
                url.length <= 400
              )
                ctrl.setImage(url);
            }, 300),
          );
        },
      },
    }),
  ]);
}

function pieceImage(t: Piece, is3d: boolean) {
  if (is3d) {
    const preview = t == 'Staunton' ? '-Preview' : '';
    return `images/staunton/piece/${t}/White-Knight${preview}.png`;
  }
  return `piece/${t}/wN.svg`;
}

const pieceView = (current: Piece, set: (t: Piece) => void, is3d: boolean) => (t: Piece) =>
  t === 'custom'
  ? undefined
  : h(
      'button.no-square',
      {
        attrs: { title: t, type: 'button' },
        hook: bind('click', () => set(t)),
        class: { active: current === t },
      },
      [h('piece', { attrs: { style: `background-image:url(${lichess.asset.url(pieceImage(t, is3d))})` } })],
    );

function applyPiece(t: Piece, list: Piece[], is3d: boolean, url: string) {
  if (is3d) {
    $('body').removeClass(list.join(' ')).addClass(t);
  } else {
    const sprite = document.getElementById('piece-sprite') as HTMLLinkElement;
    sprite.href = sprite.href.replace(/[\w-]+(\.external|)\.css/, t + '$1.css');
    document.body.dataset.pieceSet = t;
    if (t === 'custom') {
      try {
        const {href} = new URL(url);
        document.body.style.setProperty('--piece-set-url', `url('${href}')`);
      } catch {
        lichess.announce({ msg: 'Invalid custom piece set URL' });
      }
    }
  }
  lichess.pubsub.emit('theme.change');
}
