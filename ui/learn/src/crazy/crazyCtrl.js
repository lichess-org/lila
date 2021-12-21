var m = require('mithril');
var util = require('../og/main').util;
var drag = require('../og/main').drag;
var ground = require('../ground');
var ogDrag = require('../og/drag');
var ogDrop = require('../og/drop');
var setDropMode = ogDrop.setDropMode,
  cancelDropMode = ogDrop.cancelDropMode;

exports.drag = function (ctrl, color, e) {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ground.instance.data.movable.color !== color) return;
  var el = e.target,
    role = el.getAttribute('data-role'),
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  e.stopPropagation();
  e.preventDefault();
  ogDrag.dragNewPiece(ground.instance.data, { color: color, role: role }, e);
};

exports.selectToDrop = function (ctrl, color, e) {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (ground.instance.data.movable.color !== color) return;
  var el = e.target,
    role = el.getAttribute('data-role'),
    number = el.getAttribute('data-nb');
  if (!role || !color || number === '0') return;
  var dropMode = ground.instance.data.dropmode,
    dropPiece = ground.instance.data.dropmode.piece;
  if (!dropMode.active || (dropPiece && dropPiece.role !== role)) {
    setDropMode(ground.instance.data, { color: color, role: role });
    // ctrl.dropmodeActive = true;
  } else {
    cancelDropMode(ground.instance.data);
    // ctrl.dropmodeActive = false;
  }
  e.stopPropagation();
  e.preventDefault();
};

exports.shadowDrop = function (ctrl, color, e) {
  var el = e.target;
  var role = el.getAttribute('data-role') || (el.firstElementChild && el.firstElementChild.getAttribute('data-role'));
  var curPiece = ground.instance.data.drawable.piece;
  if (curPiece && curPiece.role == role && curPiece.color == color) ground.instance.data.drawable.piece = undefined;
  else ground.instance.data.drawable.piece = { role: role, color: color };
  e.stopPropagation();
  e.preventDefault();
  m.redraw();
};
