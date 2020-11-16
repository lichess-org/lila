var util = require("../og/main").util;
var drag = require("../og/main").drag;

module.exports = function (cg, e) {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click

  var role = e.target.firstChild.getAttribute("data-role"),
    color = e.target.firstChild.getAttribute("data-color");
  if (!role || !color) return;
  e.stopPropagation();
  e.preventDefault();
  var key = "a0";
  var coords = util.key2pos(
    cg.data.orientation === "white" ? key : util.invertKey(key)
  );
  var piece = {
    role: role,
    color: color,
  };
  var obj = {};
  obj[key] = piece;
  cg.setPieces(obj);
  var bounds = cg.data.bounds();
  var squareBounds = e.target.parentNode.getBoundingClientRect();
  var rel = [
    (coords[0] - 1) * squareBounds.width + bounds.left,
    (9 - coords[1]) * squareBounds.height + bounds.top,
  ];
  cg.data.draggable.current = {
    orig: key,
    piece: piece.color + piece.role,
    rel: rel,
    epos: [e.clientX, e.clientY],
    pos: [e.clientX - rel[0], e.clientY - rel[1]],
    dec: [-squareBounds.width / 2, -squareBounds.height / 2],
    bounds: bounds,
    started: true,
    newPiece: true,
  };
  drag.processDrag(cg.data);
};
