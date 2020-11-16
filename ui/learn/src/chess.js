var Shogi = require("shogiutil/vendor/Shogi").Shogi;
var util = require("./util");
const { roleToSan } = require("./util");

module.exports = function (fen, appleKeys) {
  var shogi = Shogi.init(fen);
  var oldShogi;

  // adds enemy pawns on apples, for collisions
  if (appleKeys) {
    var color = shogi.player === "white" ? "black" : "white";
    appleKeys.forEach(function (key) {
      shogi = Shogi.init(Shogi.place(shogi.fen, "pawn", color, key));
    });
  }

  function updateShogi(s) {
    oldShogi = shogi;
    shogi = s;
  }

  function undo() {
    shogi = oldShogi;
  }

  function placePiece(role, color, key) {
    shogi = Shogi.init(Shogi.place(shogi.fen, role, color, key));
  }

  function getSquarePiece(key) {
    if (shogi.pieceMap[key]) {
      var role = shogi.pieceMap[key].slice(6, 7);
      var color = shogi.pieceMap[key].slice(0, 5);
      return { type: role, color: color };
    } else {
      return null;
    }
  }

  function getColor() {
    return shogi.player == "white" ? "white" : "black";
  }

  function setColor(c) {
    var turn = c === "white" ? "w" : "b";
    var newFen = util.setFenTurn(shogi.fen, turn);
    updateShogi(Shogi.init(newFen));
  }

  function filterPieces(c) {
    var allPos = [];
    for (var i in shogi.pieceMap) {
      if (shogi.pieceMap[i].startsWith(c)) {
        allPos.push(i);
      }
    }
    return allPos;
  }

  function findKing(c) {
    let king;
    if (color === "white") king = "white-king";
    else king = "black-king";
    for (var i in shogi.pieceMap) {
      if (shogi.pieceMap[i] === king) {
        return i;
      }
    }
  }

  var findCaptures = function () {
    var allCaptures = [];
    const pieces = filterPieces(getColor());
    for (var i in shogi.dests) {
      for (var j in shogi.dests[i]) {
        if (pieces.includes(shogi.dests[i][j]))
          allCaptures.push({ orig: i, dest: shogi.dests[i][j] });
      }
    }
    return allCaptures;
  };

  return {
    dests: function (opts) {
      opts = opts || {};
      if (opts.illegal) return Shogi.getUnsafeDests(shogi.fen);
      return shogi.dests;
    },
    pockets: function () {
      return shogi.crazyhouse;
    },
    color: function (c) {
      if (c) setColor(c);
      else return getColor();
    },
    fen: function () {
      return shogi.fen;
    },
    move: function (orig, dest, prom) {
      updateShogi(Shogi.move(shogi.fen, orig, dest, prom ? prom : "")); // valid?
      return { from: orig, to: dest, promotion: prom };
    },
    occupation: function () {
      return shogi.pieceMap;
    },
    kingKey: function (color) {
      return findKing(color);
    },
    findCapture: function () {
      return findCaptures()[0];
    },
    findUnprotectedCapture: function () {
      return false;
    },
    checks: function () {
      if (!shogi.check) return null;
      return shogi.check;
    },
    playRandomMove: function () {
      const orig = Object.keys(shogi.dests)[
        Math.floor(Math.random() * Object.keys(shogi.dests).length)
      ];
      const moves = shogi.dests[orig];
      const dest = moves[Math.floor(Math.random() * moves.length)];
      updateShogi(Shogi.move(shogi.fen, orig, dest));
      return { orig: orig, dest: dest };
    },
    get: getSquarePiece,
    undo: undo,
    place: placePiece,
    instance: shogi,
  };
};
