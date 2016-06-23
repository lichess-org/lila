var m = require('mithril');
var stages = require('../stages');
var item = require('../item').builder;
var itemView = require('../item').view;
var chessground = require('chessground');
var Chess = require('chess.js').Chess;

function chessToDests(chess) {
  var dests = {};
  chess.SQUARES.forEach(function(s) {
    var ms = chess.moves({
      square: s,
      verbose: true
    });
    if (ms.length) dests[s] = ms.map(function(m) {
      return m.to;
    });
  });
  return dests;
}

function chessToColor(chess) {
  return chess.turn() == "w" ? "white" : "black";
}

function setTurn(chess, color) {
  chess.load(chess.fen().replace(/\s(w|b)\s/, color === 'white' ? ' w ' : ' b '));
}

function makeGround(opts) {
  var color = chessToColor(opts.chess);
  return new chessground.controller({
    fen: opts.chess.fen(),
    orientation: opts.orientation,
    coordinates: true,
    turnColor: color,
    movable: {
      free: false,
      color: color,
      dests: chessToDests(opts.chess)
    },
    events: {
      move: opts.onMove
    },
    items: opts.items,
    premovable: {
      enabled: true
    },
    drawable: {
      enabled: true,
      eraseOnClick: true
    },
    highlight: {
      lastMove: true,
      dragOver: true
    },
    animation: {
      enabled: true,
      duration: 500
    },
    disableContextMenu: true
  });
}

module.exports = function(opts) {

  var stage = stages.byId(m.route.param("id"));

  var onMove = function(orig, dest) {
    var move = chess.move({
      from: orig,
      to: dest
    });
    if (!move) throw 'Invalid move!';
    var item = items[move.to];
    if (item) {
      delete items[move.to];
    }
    setTurn(chess, color);
    ground.set({
      turnColor: color,
      movable: {
        color: color,
        dests: chessToDests(chess)
      }
    });
  };

  var color = 'white';
  var fen = '8/8/8/8/8/8/4R3/8 w - - 0 1';
  var items = {
    a1: item.apple(),
    a2: item.apple(),
    c5: item.apple()
  };
  var chess = new Chess(fen)
  var ground = makeGround({
    chess: chess,
    orientation: 'white',
    onMove: onMove,
    items: {
      render: function(pos, key) {
        if (items[key]) return itemView(items[key]);
      }
    }
  });
  return {
    stage: stage,
    chessground: ground
  };
};
