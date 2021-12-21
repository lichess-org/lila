const crazyCtrl = require('./crazyCtrl');
const ground = require('../ground');
const m = require('mithril');
const oKeys = ['pawn', 'lance', 'knight', 'silver', 'gold', 'bishop', 'rook'];
const eventNames1 = ['mousedown', 'touchmove'];
const eventNames2 = ['click'];
const eventNames3 = ['contextmenu'];

function reverse(color) {
  return color == 'sente' ? 'gote' : 'sente';
}

exports.renderPocket = function (ctrl, position) {
  if (!ctrl.level.pockets) return;
  const data = ground.instance.data;
  const shadowPiece = data.drawable.piece;
  const bottomColor = ctrl.level.blueprint.color;
  const color = position == 'bottom' ? bottomColor : reverse(bottomColor);
  const usable = position == 'bottom';
  const pocket = ctrl.level.pockets[color];
  return m(
    `div.pocket.is2d.pocket-${position}.pos-${ctrl.level.blueprint.color}` + (usable ? '.usable' : ''),
    {
      config: function (element, isInitialized) {
        if (isInitialized) return;
        eventNames1.forEach(function (name) {
          element.addEventListener(name, function (e) {
            crazyCtrl.drag(ctrl, color, e);
            m.redraw();
          });
        });
        eventNames2.forEach(function (name) {
          element.addEventListener(name, function (e) {
            crazyCtrl.selectToDrop(ctrl, color, e);
            m.redraw();
          });
        });
        eventNames3.forEach(function (name) {
          element.addEventListener(name, function (e) {
            crazyCtrl.shadowDrop(ctrl, color, e);
          });
        });
      },
    },
    oKeys.map(role => {
      // doNotShowPawnsInPocket is for when the pawns are stars/apples
      const nb = role === 'pawn' && ctrl.level.blueprint.doNotShowPawnsInPocket ? 0 : pocket[role];
      const sp = shadowPiece && role === shadowPiece.role && color == shadowPiece.color;
      const selectedSquare =
        data.dropmode.active &&
        data.dropmode.piece &&
        data.dropmode.piece.color === color &&
        data.dropmode.piece.role === role &&
        data.dropmode.piece.color === data.movable.color;
      return m(
        'div.pocket-c1',
        m(
          'div.pocket-c2',
          {
            class: sp ? 'shadow-piece' : '',
          },
          m('piece.' + role + '.' + color, {
            class: selectedSquare ? 'selected-square' : '',
            'data-role': role,
            'data-color': color,
            'data-nb': nb,
            cursor: 'pointer',
          })
        )
      );
    })
  );
};
