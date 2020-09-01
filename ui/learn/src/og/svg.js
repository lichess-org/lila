var m = require("mithril");
var key2pos = require("./util").key2pos;
var isTrident = require("./util").isTrident;

function circleWidth(current, bounds) {
  return ((current ? 3 : 4) / 512) * bounds.width;
}

function lineWidth(brush, current, bounds) {
  return (
    (((brush.lineWidth || 10) * (current ? 0.85 : 1)) / 512) * bounds.width
  );
}

function opacity(brush, current) {
  return (brush.opacity || 1) * (current ? 0.9 : 1);
}

function arrowMargin(current, bounds) {
  return isTrident() ? 0 : ((current ? 10 : 20) / 512) * bounds.width;
}

function pos2px(pos, bounds) {
  var squareSize = bounds.width / 9;
  return [(pos[0] - 0.5) * squareSize, (9.5 - pos[1]) * squareSize];
}

function circle(brush, pos, current, bounds) {
  var o = pos2px(pos, bounds);
  var width = circleWidth(current, bounds);
  var radius = bounds.width / 18;
  return {
    tag: "circle",
    attrs: {
      stroke: brush.color,
      "stroke-width": width,
      fill: "none",
      opacity: opacity(brush, current),
      cx: o[0],
      cy: o[1],
      r: radius - width / 2,
    },
  };
}

function arrow(brush, orig, dest, current, bounds) {
  var m = arrowMargin(current, bounds);
  var a = pos2px(orig, bounds);
  var b = pos2px(dest, bounds);
  var dx = b[0] - a[0],
    dy = b[1] - a[1],
    angle = Math.atan2(dy, dx);
  var xo = Math.cos(angle) * m,
    yo = Math.sin(angle) * m;
  return {
    tag: "line",
    attrs: {
      stroke: brush.color,
      "stroke-width": lineWidth(brush, current, bounds),
      "stroke-linecap": "round",
      "marker-end": isTrident() ? null : "url(#arrowhead-" + brush.key + ")",
      opacity: opacity(brush, current),
      x1: a[0],
      y1: a[1],
      x2: b[0] - xo,
      y2: b[1] - yo,
    },
  };
}

function piece(cfg, pos, piece, bounds) {
  var o = pos2px(pos, bounds);
  var size = (bounds.width / 9) * (piece.scale || 1);
  var name = piece.color === "white" ? "w" : "b";
  name += (piece.role === "knight" ? "n" : piece.role[0]).toUpperCase();
  var href = cfg.baseUrl + name + ".svg";
  return {
    tag: "image",
    attrs: {
      class: piece.color + " " + piece.role,
      x: o[0] - size / 2,
      y: o[1] - size / 2,
      width: size,
      height: size,
      href: href,
    },
  };
}

function defs(brushes) {
  return {
    tag: "defs",
    children: [
      brushes.map(function (brush) {
        return {
          tag: "marker",
          attrs: {
            id: "arrowhead-" + brush.key,
            orient: "auto",
            markerWidth: 4,
            markerHeight: 8,
            refX: 2.05,
            refY: 2.01,
          },
          children: [
            {
              tag: "path",
              attrs: {
                d: "M0,0 V4 L3,2 Z",
                fill: brush.color,
              },
            },
          ],
        };
      }),
    ],
  };
}

function orient(pos, color) {
  return color === "white" ? pos : [10 - pos[0], 10 - pos[1]];
}

function renderShape(data, current, bounds) {
  return function (shape, i) {
    if (shape.piece)
      return piece(
        data.drawable.pieces,
        orient(key2pos(shape.orig), data.orientation),
        shape.piece,
        bounds
      );
    else if (shape.brush) {
      var brush = shape.brushModifiers
        ? makeCustomBrush(
            data.drawable.brushes[shape.brush],
            shape.brushModifiers,
            i
          )
        : data.drawable.brushes[shape.brush];
      var orig = orient(key2pos(shape.orig), data.orientation);
      if (shape.orig && shape.dest)
        return arrow(
          brush,
          orig,
          orient(key2pos(shape.dest), data.orientation),
          current,
          bounds
        );
      else if (shape.orig) return circle(brush, orig, current, bounds);
    }
  };
}

function makeCustomBrush(base, modifiers, i) {
  return {
    key: "cb_" + i,
    color: modifiers.color || base.color,
    opacity: modifiers.opacity || base.opacity,
    lineWidth: modifiers.lineWidth || base.lineWidth,
  };
}

function computeUsedBrushes(d, drawn, current) {
  var brushes = [];
  var keys = [];
  var shapes = current && current.dest ? drawn.concat(current) : drawn;
  for (var i in shapes) {
    var shape = shapes[i];
    if (!shape.dest) continue;
    var brushKey = shape.brush;
    if (shape.brushModifiers)
      brushes.push(
        makeCustomBrush(d.brushes[brushKey], shape.brushModifiers, i)
      );
    else {
      if (keys.indexOf(brushKey) === -1) {
        brushes.push(d.brushes[brushKey]);
        keys.push(brushKey);
      }
    }
  }
  return brushes;
}

// don't use mithril keys here, they're buggy with SVG
// likely because of var dummy = $document.createElement("div");
// in handleKeysDiffer
module.exports = function (ctrl) {
  if (!ctrl.data.bounds) return;
  var d = ctrl.data.drawable;
  var allShapes = d.shapes.concat(d.autoShapes);
  if (!allShapes.length && !d.current.orig) return;
  var bounds = ctrl.data.bounds();
  if (bounds.width !== bounds.height) return;
  var usedBrushes = computeUsedBrushes(d, allShapes, d.current);
  var renderedShapes = allShapes.map(renderShape(ctrl.data, false, bounds));
  var renderedCurrent = renderShape(ctrl.data, true, bounds)(d.current, 9999);
  if (renderedCurrent) renderedShapes.push(renderedCurrent);
  return {
    tag: "svg",
    attrs: {
      key: "svg",
    },
    children: [defs(usedBrushes), renderedShapes],
  };
};
