var drag = require("./drag");
var draw = require("./draw");
var util = require("./util");
var svg = require("./svg");
var makeCoords = require("./coords");
var m = require("mithril");

var pieceTag = "piece";
var squareTag = "square";

function pieceClass(p) {
  return p.role + " " + p.color;
}

function renderPiece(d, key, ctx) {
  var attrs = {
    key: "p" + key,
    style: {},
    class: pieceClass(d.pieces[key]),
  };
  var translate = posToTranslate(util.key2pos(key), ctx);
  var draggable = d.draggable.current;
  if (draggable.orig === key && draggable.started) {
    translate[0] += draggable.pos[0] + draggable.dec[0];
    translate[1] += draggable.pos[1] + draggable.dec[1];
    attrs.class += " dragging";
  } else if (d.animation.current.anims) {
    var animation = d.animation.current.anims[key];
    if (animation) {
      translate[0] += animation[1][0];
      translate[1] += animation[1][1];
    }
  }
  attrs.style[ctx.transformProp] = util.translate(translate);
  if (d.pieceKey) attrs["data-key"] = key;
  return {
    tag: pieceTag,
    attrs: attrs,
  };
}

function renderSquare(key, classes, ctx) {
  var attrs = {
    key: "s" + key,
    class: classes,
    style: {},
  };
  attrs.style[ctx.transformProp] = util.translate(
    posToTranslate(util.key2pos(key), ctx)
  );
  return {
    tag: squareTag,
    attrs: attrs,
  };
}

function posToTranslate(pos, ctx) {
  return [
    ((ctx.asWhite ? pos[0] - 1 : 9 - pos[0]) * ctx.bounds.width) / 9,
    ((ctx.asWhite ? 9 - pos[1] : pos[1] - 1) * ctx.bounds.height) / 9,
  ];
}

function renderGhost(key, piece, ctx) {
  if (!piece) return;
  var attrs = {
    key: "g" + key,
    style: {},
    class: pieceClass(piece) + " ghost",
  };
  attrs.style[ctx.transformProp] = util.translate(
    posToTranslate(util.key2pos(key), ctx)
  );
  return {
    tag: pieceTag,
    attrs: attrs,
  };
}

function renderFading(cfg, ctx) {
  var attrs = {
    key: "f" + cfg.piece.key,
    class: "fading " + pieceClass(cfg.piece),
    style: {
      opacity: cfg.opacity,
    },
  };
  attrs.style[ctx.transformProp] = util.translate(
    posToTranslate(cfg.piece.pos, ctx)
  );
  return {
    tag: pieceTag,
    attrs: attrs,
  };
}

function addSquare(squares, key, klass) {
  if (squares[key]) squares[key].push(klass);
  else squares[key] = [klass];
}

function renderSquares(ctrl, ctx) {
  var d = ctrl.data;
  var squares = {};
  if (d.lastMove && d.highlight.lastMove)
    d.lastMove.forEach(function (k) {
      addSquare(squares, k, "last-move");
    });
  if (d.check && d.highlight.check) addSquare(squares, d.check, "check");
  if (d.selected) {
    addSquare(squares, d.selected, "selected");
    var over = d.draggable.current.over;
    var dests = d.movable.dests[d.selected];
    if (dests)
      dests.forEach(function (k) {
        if (k === over) addSquare(squares, k, "move-dest drag-over");
        else if (d.movable.showDests)
          addSquare(squares, k, "move-dest" + (d.pieces[k] ? " oc" : ""));
      });
    var pDests = d.premovable.dests;
    if (pDests)
      pDests.forEach(function (k) {
        if (k === over) addSquare(squares, k, "premove-dest drag-over");
        else if (d.movable.showDests)
          addSquare(squares, k, "premove-dest" + (d.pieces[k] ? " oc" : ""));
      });
  }
  var premove = d.premovable.current;
  if (premove)
    premove.forEach(function (k) {
      addSquare(squares, k, "current-premove");
    });
  else if (d.predroppable.current.key)
    addSquare(squares, d.predroppable.current.key, "current-premove");

  if (ctrl.vm.exploding)
    ctrl.vm.exploding.keys.forEach(function (k) {
      addSquare(squares, k, "exploding" + ctrl.vm.exploding.stage);
    });

  var dom = [];
  if (d.items) {
    for (var i = 0; i < 81; i++) {
      var key = util.allKeys[i];
      var square = squares[key];
      var item = d.items.render(util.key2pos(key), key);
      if (square || item) {
        var sq = renderSquare(
          key,
          square ? square.join(" ") + (item ? " has-item" : "") : "has-item",
          ctx
        );
        if (item) sq.children = [item];
        dom.push(sq);
      }
    }
  } else {
    for (var key in squares)
      dom.push(renderSquare(key, squares[key].join(" "), ctx));
  }
  return dom;
}

function renderContent(ctrl) {
  var d = ctrl.data;
  if (!d.bounds) return;
  var ctx = {
    asWhite: d.orientation === "white",
    bounds: d.bounds(),
    transformProp: util.transformProp(),
  };
  var children = renderSquares(ctrl, ctx);
  if (d.animation.current.fadings)
    d.animation.current.fadings.forEach(function (p) {
      children.push(renderFading(p, ctx));
    });

  // must insert pieces in the right order
  // for 3D to display correctly
  var keys = ctx.asWhite ? util.allKeys : util.invKeys;
  if (d.items) {
    for (var i = 0; i < 81; i++) {
      if (d.pieces[keys[i]] && !d.items.render(util.key2pos(keys[i]), keys[i]))
        children.push(renderPiece(d, keys[i], ctx));
    }
  } else {
    for (var i = 0; i < 81; i++) {
      if (d.pieces[keys[i]]) children.push(renderPiece(d, keys[i], ctx));
    }
    // the hack to drag new pieces on the board (editor and crazyhouse)
    // is to put it on a0 then set it as being dragged
    if (d.draggable.current && d.draggable.current.newPiece)
      children.push(renderPiece(d, "a0", ctx));
  }

  if (d.draggable.showGhost) {
    var dragOrig = d.draggable.current.orig;
    if (dragOrig && !d.draggable.current.newPiece)
      children.push(renderGhost(dragOrig, d.pieces[dragOrig], ctx));
  }
  if (d.drawable.enabled) children.push(svg(ctrl));
  return children;
}

function startDragOrDraw(d) {
  return function (e) {
    if (util.isRightButton(e) && d.draggable.current.orig) {
      if (d.draggable.current.newPiece)
        delete d.pieces[d.draggable.current.orig];
      d.draggable.current = {};
      d.selected = null;
    } else if ((e.shiftKey || util.isRightButton(e)) && d.drawable.enabled)
      draw.start(d, e);
    else drag.start(d, e);
  };
}

function dragOrDraw(d, withDrag, withDraw) {
  return function (e) {
    if ((e.shiftKey || util.isRightButton(e)) && d.drawable.enabled)
      withDraw(d, e);
    else if (!d.viewOnly) withDrag(d, e);
  };
}

function bindEvents(ctrl, el, context) {
  var d = ctrl.data;
  var onstart = startDragOrDraw(d);
  var onmove = dragOrDraw(d, drag.move, draw.move);
  var onend = dragOrDraw(d, drag.end, draw.end);
  var startEvents = ["touchstart", "mousedown"];
  var moveEvents = ["touchmove", "mousemove"];
  var endEvents = ["touchend", "mouseup"];
  startEvents.forEach(function (ev) {
    el.addEventListener(ev, onstart);
  });
  moveEvents.forEach(function (ev) {
    document.addEventListener(ev, onmove);
  });
  endEvents.forEach(function (ev) {
    document.addEventListener(ev, onend);
  });
  context.onunload = function () {
    startEvents.forEach(function (ev) {
      el.removeEventListener(ev, onstart);
    });
    moveEvents.forEach(function (ev) {
      document.removeEventListener(ev, onmove);
    });
    endEvents.forEach(function (ev) {
      document.removeEventListener(ev, onend);
    });
  };
}

function renderBoard(ctrl) {
  var d = ctrl.data;
  return {
    tag: "cg-board",
    attrs: {
      config: function (el, isUpdate, context) {
        if (isUpdate) return;
        if (!d.viewOnly || d.drawable.enabled) bindEvents(ctrl, el, context);
        // this function only repaints the board itself.
        // it's called when dragging or animating pieces,
        // to prevent the full application embedding chessground
        // rendering on every animation frame
        d.render = function () {
          m.render(el, renderContent(ctrl));
        };
        d.renderRAF = function () {
          util.requestAnimationFrame(d.render);
        };
        d.bounds = util.memo(el.getBoundingClientRect.bind(el));
        d.element = el;
        d.render();
      },
    },
    children: [],
  };
}

module.exports = function (ctrl) {
  var d = ctrl.data;
  return {
    tag: "div",
    attrs: {
      config: function (el, isUpdate) {
        if (isUpdate) {
          if (d.redrawCoords) d.redrawCoords(d.orientation);
          return;
        }
        if (d.coordinates) d.redrawCoords = makeCoords(d, el);
        el.addEventListener("contextmenu", function (e) {
          if (d.disableContextMenu || d.drawable.enabled) {
            e.preventDefault();
            return false;
          }
        });
        if (!d.stats.boundWindowEvents) {
          d.stats.boundWindowEvents = 1;
          if (d.resizable)
            document.body.addEventListener(
              "chessground.resize",
              function (e) {
                d.bounds.clear();
                d.render();
              },
              false
            );
          ["onscroll", "onresize"].forEach(function (n) {
            var prev = window[n];
            window[n] = function () {
              prev && prev();
              d.bounds.clear();
            };
          });
        }
      },
      class: [
        "cg-wrap",
        "orientation-" + d.orientation,
        d.viewOnly ? "view-only" : "manipulable",
      ].join(" "),
    },
    children: [
      {
        tag: "cg-helper",
        children: [
          {
            tag: "cg-container",
            children: [renderBoard(ctrl)],
          },
        ],
      },
    ],
  };
};
