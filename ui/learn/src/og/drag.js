var board = require("./board");
var util = require("./util");
var draw = require("./draw");

var originTarget;

function hashPiece(piece) {
  return piece ? piece.color + piece.role : "";
}

function computeSquareBounds(data, bounds, key) {
  var pos = util.key2pos(key);
  if (data.orientation !== "white") {
    pos[0] = 10 - pos[0];
    pos[1] = 10 - pos[1];
  }
  return {
    left: bounds.left + (bounds.width * (pos[0] - 1)) / 9,
    top: bounds.top + (bounds.height * (9 - pos[1])) / 9,
    width: bounds.width / 9,
    height: bounds.height / 9,
  };
}

function start(data, e) {
  if (e.button !== undefined && e.button !== 0) return; // only touch or left click
  if (e.touches && e.touches.length > 1) return; // support one finger touch only
  e.stopPropagation();
  e.preventDefault();
  originTarget = e.target;
  var previouslySelected = data.selected;
  var position = util.eventPosition(e);
  var bounds = data.bounds();
  var orig = board.getKeyAtDomPos(data, position, bounds);
  var piece = data.pieces[orig];
  if (
    !previouslySelected &&
    (data.drawable.eraseOnClick || !piece || piece.color !== data.turnColor)
  )
    draw.clear(data);
  if (data.viewOnly) return;
  var hadPremove = !!data.premovable.current;
  var hadPredrop = !!data.predroppable.current.key;
  data.stats.ctrlKey = e.ctrlKey;
  board.selectSquare(data, orig);
  var stillSelected = data.selected === orig;
  if (piece && stillSelected && board.isDraggable(data, orig)) {
    var squareBounds = computeSquareBounds(data, bounds, orig);
    data.draggable.current = {
      previouslySelected: previouslySelected,
      orig: orig,
      piece: hashPiece(piece),
      rel: position,
      epos: position,
      pos: [0, 0],
      dec: data.draggable.centerPiece
        ? [
            position[0] - (squareBounds.left + squareBounds.width / 2),
            position[1] - (squareBounds.top + squareBounds.height / 2),
          ]
        : [0, 0],
      bounds: bounds,
      started: data.draggable.autoDistance && data.stats.dragged,
    };
  } else {
    if (hadPremove) board.unsetPremove(data);
    if (hadPredrop) board.unsetPredrop(data);
  }
  processDrag(data);
}

function processDrag(data) {
  util.requestAnimationFrame(function () {
    var cur = data.draggable.current;
    if (cur.orig) {
      // cancel animations while dragging
      if (
        data.animation.current.start &&
        data.animation.current.anims[cur.orig]
      )
        data.animation.current = {};
      // if moving piece is gone, cancel
      if (hashPiece(data.pieces[cur.orig]) !== cur.piece) cancel(data);
      else {
        if (
          !cur.started &&
          util.distance(cur.epos, cur.rel) >= data.draggable.distance
        )
          cur.started = true;
        if (cur.started) {
          cur.pos = [cur.epos[0] - cur.rel[0], cur.epos[1] - cur.rel[1]];
          cur.over = board.getKeyAtDomPos(data, cur.epos, cur.bounds);
        }
      }
    }
    data.render();
    if (cur.orig) processDrag(data);
  });
}

function move(data, e) {
  if (e.touches && e.touches.length > 1) return; // support one finger touch only
  if (data.draggable.current.orig)
    data.draggable.current.epos = util.eventPosition(e);
}

function end(data, e) {
  var cur = data.draggable.current;
  var orig = cur ? cur.orig : null;
  if (!orig) return;
  // comparing with the origin target is an easy way to test that the end event
  // has the same touch origin
  if (e.type === "touchend" && originTarget !== e.target && !cur.newPiece) {
    data.draggable.current = {};
    return;
  }
  board.unsetPremove(data);
  board.unsetPredrop(data);
  var eventPos = util.eventPosition(e);
  var dest = eventPos
    ? board.getKeyAtDomPos(data, eventPos, cur.bounds)
    : cur.over;
  if (cur.started) {
    if (cur.newPiece) board.dropNewPiece(data, orig, dest);
    else {
      if (orig !== dest) data.movable.dropped = [orig, dest];
      data.stats.ctrlKey = e.ctrlKey;
      if (board.userMove(data, orig, dest)) data.stats.dragged = true;
    }
  }
  if (orig === cur.previouslySelected && (orig === dest || !dest))
    board.setSelected(data, null);
  else if (!data.selectable.enabled) board.setSelected(data, null);
  data.draggable.current = {};
}

function cancel(data) {
  if (data.draggable.current.orig) {
    data.draggable.current = {};
    board.selectSquare(data, null);
  }
}

module.exports = {
  start: start,
  move: move,
  end: end,
  cancel: cancel,
  processDrag: processDrag, // must be exposed for board editors
};
