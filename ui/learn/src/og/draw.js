var board = require("./board");
var util = require("./util");

var brushes = ["green", "red", "blue", "yellow"];

function hashPiece(piece) {
  return piece ? piece.color + " " + piece.role : "";
}

function start(data, e) {
  if (e.touches && e.touches.length > 1) return; // support one finger touch only
  e.stopPropagation();
  e.preventDefault();
  board.cancelMove(data);
  var position = util.eventPosition(e);
  var bounds = data.bounds();
  var orig = board.getKeyAtDomPos(data, position, bounds);
  data.drawable.current = {
    orig: orig,
    epos: position,
    bounds: bounds,
    brush: brushes[(e.shiftKey & util.isRightButton(e)) + (e.altKey ? 2 : 0)],
  };
  processDraw(data);
}

function processDraw(data) {
  util.requestAnimationFrame(function () {
    var cur = data.drawable.current;
    if (cur.orig) {
      var dest = board.getKeyAtDomPos(data, cur.epos, cur.bounds);
      if (cur.orig === dest) cur.dest = undefined;
      else cur.dest = dest;
    }
    data.render();
    if (cur.orig) processDraw(data);
  });
}

function move(data, e) {
  if (data.drawable.current.orig)
    data.drawable.current.epos = util.eventPosition(e);
}

function end(data, e) {
  var drawable = data.drawable;
  var orig = drawable.current.orig;
  var dest = drawable.current.dest;
  if (orig && dest) addLine(drawable, orig, dest);
  else if (orig) addCircle(drawable, orig);
  drawable.current = {};
  data.render();
}

function cancel(data) {
  if (data.drawable.current.orig) data.drawable.current = {};
}

function clear(data) {
  if (data.drawable.shapes.length) {
    data.drawable.shapes = [];
    data.render();
    onChange(data.drawable);
  }
}

function not(f) {
  return function (x) {
    return !f(x);
  };
}

function addCircle(drawable, key) {
  var brush = drawable.current.brush;
  var sameCircle = function (s) {
    return s.orig === key && !s.dest;
  };
  var similar = drawable.shapes.filter(sameCircle)[0];
  if (similar) drawable.shapes = drawable.shapes.filter(not(sameCircle));
  if (!similar || similar.brush !== brush)
    drawable.shapes.push({
      brush: brush,
      orig: key,
    });
  onChange(drawable);
}

function addLine(drawable, orig, dest) {
  var brush = drawable.current.brush;
  var sameLine = function (s) {
    return (
      s.orig &&
      s.dest &&
      ((s.orig === orig && s.dest === dest) ||
        (s.dest === orig && s.orig === dest))
    );
  };
  var exists = drawable.shapes.filter(sameLine).length > 0;
  if (exists) drawable.shapes = drawable.shapes.filter(not(sameLine));
  else
    drawable.shapes.push({
      brush: brush,
      orig: orig,
      dest: dest,
    });
  onChange(drawable);
}

function onChange(drawable) {
  drawable.onChange(drawable.shapes);
}

module.exports = {
  start: start,
  move: move,
  end: end,
  cancel: cancel,
  clear: clear,
  processDraw: processDraw,
};
