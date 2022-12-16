import { VNode, h } from 'snabbdom';
import { Close, Redraw, bind, header } from './util';

type Piece = string;

export interface PieceData {
  current: Piece;
  list: Piece[];
}

export interface PieceCtrl {
  std: PieceData;
  chu: PieceData;
  trans: Trans;
  set(t: Piece): void;
  close: Close;
}

export function ctrl(std: PieceData, chu: PieceData, trans: Trans, redraw: Redraw, close: Close): PieceCtrl {
  return {
    trans,
    std: std,
    chu: chu,
    set(t: Piece) {
      if (isChu(t)) chu.current = t;
      else std.current = t;

      applyPiece(t);
      $.post(`/pref/${isChu(t) ? 'chuPieceSet' : 'pieceSet'}`, {
        set: t,
      }).fail(() => window.lishogi.announce({ msg: 'Failed to save piece set preference' }));
      redraw();
    },
    close,
  };
}

export function view(ctrl: PieceCtrl): VNode {
  return h('div.sub.piece.', [
    header(ctrl.trans.noarg('pieceSet'), () => ctrl.close()),
    h('div.list', ctrl.std.list.map(pieceView(ctrl.std.current, ctrl.set))),
    h('div.chu-piece.text', { attrs: { 'data-icon': '(' } }, ctrl.trans.noarg('chushogi')),
    h('div.list', ctrl.chu.list.map(pieceView(ctrl.chu.current, ctrl.set))),
  ]);
}

function pieceImage(t: Piece) {
  const piece = isChu(t) ? '0_KIRIN' : '0KI';
  return `piece/${t}/${piece}.${isPngPiece(t) ? 'png' : 'svg'}`;
}

function isChu(t: Piece): boolean {
  return t.startsWith('Chu_');
}

function isPngPiece(t: Piece): boolean {
  return t === 'Portella' || t === 'Portella_2Kanji' || t === 'Intl_Portella';
}

function pieceView(current: Piece, set: (t: Piece) => void) {
  return (t: Piece) =>
    h(
      'a.no-square',
      {
        attrs: { title: t },
        hook: bind('click', () => set(t)),
        class: { active: current === t },
      },
      [
        h('piece', {
          attrs: {
            style: `background-image:url(${window.lishogi.assetUrl(pieceImage(t))}); will-change: ${
              isPngPiece(t) ? 'transform' : 'auto'
            }`,
          },
        }),
      ]
    );
}

function applyPiece(t: Piece) {
  const sprite = $(`#${isChu(t) ? 'chu-' : ''}piece-sprite`);
  if (sprite.length) sprite.attr('href', sprite.attr('href').replace(/\w+\.css/, t + '.css'));

  if (isChu(t)) document.body.dataset.chuPieceSet = t;
  else document.body.dataset.pieceSet = t;
}
