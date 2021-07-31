const m = require('mithril');
const chessground = require('chessground');
const ground = require('./ground');
const opposite = chessground.util.opposite;
const key2pos = chessground.util.key2pos;

let promoting = false;

function start(orig, dest, callback) {
  const piece = ground.pieces()[dest];
  if (
    piece &&
    piece.role == 'pawn' &&
    ((dest[1] == 1 && piece.color == 'black') || (dest[1] == 8 && piece.color == 'white'))
  ) {
    promoting = {
      orig: orig,
      dest: dest,
      callback: callback,
    };
    m.redraw();
    return true;
  }
  return false;
}

function finish(role) {
  if (promoting) ground.promote(promoting.dest, role);
  if (promoting.callback) promoting.callback(promoting.orig, promoting.dest, role);
  promoting = false;
}

function renderPromotion(ctrl, dest, pieces, color, orientation, explain) {
  if (!promoting) return;

  let left = (8 - key2pos(dest)[0]) * 12.5;
  if (orientation === 'white') left = 87.5 - left;

  const vertical = color === orientation ? 'top' : 'bottom';

  return m('div#promotion-choice.' + vertical, [
    pieces.map(function (serverRole, i) {
      return m(
        'square',
        {
          style: vertical + ': ' + i * 12.5 + '%;left: ' + left + '%',
          onclick: function (e) {
            e.stopPropagation();
            finish(serverRole);
          },
        },
        m('piece.' + serverRole + '.' + color)
      );
    }),
    explain ? renderExplanation(ctrl) : null,
  ]);
}

function renderExplanation(ctrl) {
  return m('div.explanation', [
    m('h2', ctrl.trans.noarg('pawnPromotion')),
    m('p', ctrl.trans.noarg('yourPawnReachedTheEndOfTheBoard')),
    m('p', ctrl.trans.noarg('itNowPromotesToAStrongerPiece')),
    m('p', ctrl.trans.noarg('selectThePieceYouWant')),
  ]);
}

module.exports = {
  start: start,

  view: function (ctrl, stage) {
    if (!promoting) return;
    const pieces = ['queen', 'knight', 'rook', 'bishop'];

    return renderPromotion(
      ctrl,
      promoting.dest,
      pieces,
      opposite(ground.data().turnColor),
      ground.data().orientation,
      stage.blueprint.explainPromotion
    );
  },

  reset: function () {
    promoting = false;
  },
};
