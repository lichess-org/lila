var util = require("./util");
var premove = require("./premove");
var anim = require("./anim");
var hold = require("./hold");

function callUserFunction(f) {
  setTimeout(f, 1);
}

function toggleOrientation(data) {
  data.orientation = util.opposite(data.orientation);
}

function reset(data) {
  data.lastMove = null;
  setSelected(data, null);
  unsetPremove(data);
  unsetPredrop(data);
}

function setPieces(data, pieces) {
  Object.keys(pieces).forEach(function (key) {
    if (pieces[key]) data.pieces[key] = pieces[key];
    else delete data.pieces[key];
  });
  data.movable.dropped = [];
}

function setCheck(data, color) {
  var checkColor = color || data.turnColor;
  Object.keys(data.pieces).forEach(function (key) {
    if (
      data.pieces[key].color === checkColor &&
      data.pieces[key].role === "king"
    )
      data.check = key;
  });
}

function setPremove(data, orig, dest, meta) {
  unsetPredrop(data);
  data.premovable.current = [orig, dest];
  callUserFunction(util.partial(data.premovable.events.set, orig, dest, meta));
}

function unsetPremove(data) {
  if (data.premovable.current) {
    data.premovable.current = null;
    callUserFunction(data.premovable.events.unset);
  }
}

function setPredrop(data, role, key) {
  unsetPremove(data);
  data.predroppable.current = {
    role: role,
    key: key,
  };
  callUserFunction(util.partial(data.predroppable.events.set, role, key));
}

function unsetPredrop(data) {
  if (data.predroppable.current.key) {
    data.predroppable.current = {};
    callUserFunction(data.predroppable.events.unset);
  }
}

function tryAutoCastle(data, orig, dest) {
  if (!data.autoCastle) return;
  var king = data.pieces[dest];
  if (king.role !== "king") return;
  var origPos = util.key2pos(orig);
  if (origPos[0] !== 5) return;
  if (origPos[1] !== 1 && origPos[1] !== 8) return;
  var destPos = util.key2pos(dest),
    oldRookPos,
    newRookPos,
    newKingPos;
  if (destPos[0] === 7 || destPos[0] === 8) {
    oldRookPos = util.pos2key([8, origPos[1]]);
    newRookPos = util.pos2key([6, origPos[1]]);
    newKingPos = util.pos2key([7, origPos[1]]);
  } else if (destPos[0] === 3 || destPos[0] === 1) {
    oldRookPos = util.pos2key([1, origPos[1]]);
    newRookPos = util.pos2key([4, origPos[1]]);
    newKingPos = util.pos2key([3, origPos[1]]);
  } else return;
  delete data.pieces[orig];
  delete data.pieces[dest];
  delete data.pieces[oldRookPos];
  data.pieces[newKingPos] = {
    role: "king",
    color: king.color,
  };
  data.pieces[newRookPos] = {
    role: "rook",
    color: king.color,
  };
}

function baseMove(data, orig, dest) {
  var success = anim(function () {
    if (orig === dest || !data.pieces[orig]) return false;
    var captured =
      data.pieces[dest] && data.pieces[dest].color !== data.pieces[orig].color
        ? data.pieces[dest]
        : null;
    callUserFunction(util.partial(data.events.move, orig, dest, captured));
    data.pieces[dest] = data.pieces[orig];
    delete data.pieces[orig];
    data.lastMove = [orig, dest];
    data.check = null;
    tryAutoCastle(data, orig, dest);
    callUserFunction(data.events.change);
    return true;
  }, data)();
  if (success) data.movable.dropped = [];
  return success;
}

function baseNewPiece(data, piece, key) {
  if (data.pieces[key]) return false;
  callUserFunction(util.partial(data.events.dropNewPiece, piece, key));
  data.pieces[key] = piece;
  data.lastMove = [key, key];
  data.check = null;
  callUserFunction(data.events.change);
  data.movable.dropped = [];
  data.movable.dests = {};
  data.turnColor = util.opposite(data.turnColor);
  data.renderRAF();
  return true;
}

function baseUserMove(data, orig, dest) {
  var result = baseMove(data, orig, dest);
  if (result) {
    data.movable.dests = {};
    data.turnColor = util.opposite(data.turnColor);
  }
  return result;
}

function apiMove(data, orig, dest) {
  return baseMove(data, orig, dest);
}

function apiNewPiece(data, piece, key) {
  return baseNewPiece(data, piece, key);
}

function userMove(data, orig, dest) {
  if (!dest) {
    hold.cancel();
    setSelected(data, null);
    if (data.movable.dropOff === "trash") {
      delete data.pieces[orig];
      callUserFunction(data.events.change);
    }
  } else if (canMove(data, orig, dest)) {
    if (baseUserMove(data, orig, dest)) {
      var holdTime = hold.stop();
      setSelected(data, null);
      callUserFunction(
        util.partial(data.movable.events.after, orig, dest, {
          premove: false,
          ctrlKey: data.stats.ctrlKey,
          holdTime: holdTime,
        })
      );
      return true;
    }
  } else if (canPremove(data, orig, dest)) {
    setPremove(data, orig, dest, {
      ctrlKey: data.stats.ctrlKey,
    });
    setSelected(data, null);
  } else if (isMovable(data, dest) || isPremovable(data, dest)) {
    setSelected(data, dest);
    hold.start();
  } else setSelected(data, null);
}

function dropNewPiece(data, orig, dest) {
  if (canDrop(data, orig, dest)) {
    var piece = data.pieces[orig];
    delete data.pieces[orig];
    baseNewPiece(data, piece, dest);
    data.movable.dropped = [];
    callUserFunction(
      util.partial(data.movable.events.afterNewPiece, piece.role, dest, {
        predrop: false,
      })
    );
  } else if (canPredrop(data, orig, dest)) {
    setPredrop(data, data.pieces[orig].role, dest);
  } else {
    unsetPremove(data);
    unsetPredrop(data);
  }
  delete data.pieces[orig];
  setSelected(data, null);
}

function selectSquare(data, key, force) {
  if (data.selected) {
    if (key) {
      if (data.selected === key && !data.draggable.enabled) {
        setSelected(data, null);
        hold.cancel();
      } else if ((data.selectable.enabled || force) && data.selected !== key) {
        if (userMove(data, data.selected, key)) data.stats.dragged = false;
      } else hold.start();
    } else {
      setSelected(data, null);
      hold.cancel();
    }
  } else if (isMovable(data, key) || isPremovable(data, key)) {
    setSelected(data, key);
    hold.start();
  }
  if (key) callUserFunction(util.partial(data.events.select, key));
}

function setSelected(data, key) {
  data.selected = key;
  if (key && isPremovable(data, key))
    data.premovable.dests = premove(data.pieces, key, data.premovable.castle);
  else data.premovable.dests = null;
}

function isMovable(data, orig) {
  var piece = data.pieces[orig];
  return (
    piece &&
    (data.movable.color === "both" ||
      (data.movable.color === piece.color && data.turnColor === piece.color))
  );
}

function canMove(data, orig, dest) {
  return (
    orig !== dest &&
    isMovable(data, orig) &&
    (data.movable.free || util.containsX(data.movable.dests[orig], dest))
  );
}

function canDrop(data, orig, dest) {
  var piece = data.pieces[orig];
  return (
    piece &&
    dest &&
    (orig === dest || !data.pieces[dest]) &&
    (data.movable.color === "both" ||
      (data.movable.color === piece.color && data.turnColor === piece.color))
  );
}

function isPremovable(data, orig) {
  var piece = data.pieces[orig];
  return (
    piece &&
    data.premovable.enabled &&
    data.movable.color === piece.color &&
    data.turnColor !== piece.color
  );
}

function canPremove(data, orig, dest) {
  return (
    orig !== dest &&
    isPremovable(data, orig) &&
    util.containsX(premove(data.pieces, orig, data.premovable.castle), dest)
  );
}

function canPredrop(data, orig, dest) {
  var piece = data.pieces[orig];
  return (
    piece &&
    dest &&
    (!data.pieces[dest] || data.pieces[dest].color !== data.movable.color) &&
    data.predroppable.enabled &&
    data.movable.color === piece.color &&
    data.turnColor !== piece.color
  );
}

function isDraggable(data, orig) {
  var piece = data.pieces[orig];
  return (
    piece &&
    data.draggable.enabled &&
    (data.movable.color === "both" ||
      (data.movable.color === piece.color &&
        (data.turnColor === piece.color || data.premovable.enabled)))
  );
}

function playPremove(data) {
  var move = data.premovable.current;
  if (!move) return;
  var orig = move[0],
    dest = move[1],
    success = false;
  if (canMove(data, orig, dest)) {
    if (baseUserMove(data, orig, dest)) {
      callUserFunction(
        util.partial(data.movable.events.after, orig, dest, {
          premove: true,
        })
      );
      success = true;
    }
  }
  unsetPremove(data);
  return success;
}

function playPredrop(data, validate) {
  var drop = data.predroppable.current,
    success = false;
  if (!drop.key) return;
  if (validate(drop)) {
    var piece = {
      role: drop.role,
      color: data.movable.color,
    };
    if (baseNewPiece(data, piece, drop.key)) {
      callUserFunction(
        util.partial(data.movable.events.afterNewPiece, drop.role, drop.key, {
          predrop: true,
        })
      );
      success = true;
    }
  }
  unsetPredrop(data);
  return success;
}

function cancelMove(data) {
  unsetPremove(data);
  unsetPredrop(data);
  selectSquare(data, null);
}

function stop(data) {
  data.movable.color = null;
  data.movable.dests = {};
  cancelMove(data);
}

function getKeyAtDomPos(data, pos, bounds) {
  if (!bounds && !data.bounds) return;
  bounds = bounds || data.bounds(); // use provided value, or compute it
  var file = Math.ceil(9 * ((pos[0] - bounds.left) / bounds.width));
  file = data.orientation === "white" ? file : 10 - file;
  var rank = Math.ceil(9 - 9 * ((pos[1] - bounds.top) / bounds.height));
  rank = data.orientation === "white" ? rank : 10 - rank;
  if (file > 0 && file <= 9 && rank > 0 && rank <= 9)
    return util.pos2key([file, rank]);
}

// {white: {pawn: 3 queen: 1}, black: {bishop: 2}}
function getMaterialDiff(data) {
  var counts = {
    king: 0,
    lance: 0,
    silver: 0,
    gold: 0,
    promotedSilver: 0,
    promotedKnight: 0,
    promotedLance: 0,
    dragon: 0,
    horse: 0,
    rook: 0,
    bishop: 0,
    knight: 0,
    pawn: 0,
    tokin: 0,
  };
  for (var k in data.pieces) {
    var p = data.pieces[k];
    counts[p.role] += p.color === "white" ? 1 : -1;
  }
  var diff = {
    white: {},
    black: {},
  };
  for (var role in counts) {
    var c = counts[role];
    if (c > 0) diff.white[role] = c;
    else if (c < 0) diff.black[role] = -c;
  }
  return diff;
}

var pieceScores = {
  king: 0,
  lance: 1,
  silver: 1,
  gold: 1,
  promotedSilver: 1,
  promotedKnight: 1,
  promotedLance: 1,
  dragon: 5,
  horse: 5,
  rook: 5,
  bishop: 5,
  knight: 1,
  pawn: 1,
  tokin: 1,
};

function getScore(data) {
  var score = 0;
  for (var k in data.pieces) {
    score +=
      pieceScores[data.pieces[k].role] *
      (data.pieces[k].color === "white" ? 1 : -1);
  }
  return score;
}

module.exports = {
  reset: reset,
  toggleOrientation: toggleOrientation,
  setPieces: setPieces,
  setCheck: setCheck,
  selectSquare: selectSquare,
  setSelected: setSelected,
  isDraggable: isDraggable,
  canMove: canMove,
  userMove: userMove,
  dropNewPiece: dropNewPiece,
  apiMove: apiMove,
  apiNewPiece: apiNewPiece,
  playPremove: playPremove,
  playPredrop: playPredrop,
  unsetPremove: unsetPremove,
  unsetPredrop: unsetPredrop,
  cancelMove: cancelMove,
  stop: stop,
  getKeyAtDomPos: getKeyAtDomPos,
  getMaterialDiff: getMaterialDiff,
  getScore: getScore,
};
