var fen = require("./fen");
var configure = require("./configure");

module.exports = function (cfg) {
  var defaults = {
    pieces: fen.read(fen.initial),
    orientation: "white", // board orientation. white | black
    turnColor: "white", // turn to play. white | black
    check: null, // square currently in check "a2" | null
    lastMove: null, // squares part of the last move ["c3", "c4"] | null
    selected: null, // square currently selected "a1" | null
    coordinates: true, // include coords attributes
    render: null, // function that rerenders the board
    renderRAF: null, // function that rerenders the board using requestAnimationFrame
    element: null, // DOM element of the board, required for drag piece centering
    bounds: null, // function that calculates the board bounds
    autoCastle: false, // immediately complete the castle by moving the rook after king move
    viewOnly: false, // don't bind events: the user will never be able to move pieces around
    disableContextMenu: false, // because who needs a context menu on a chessboard
    resizable: true, // listens to chessground.resize on document.body to clear bounds cache
    pieceKey: false, // add a data-key attribute to piece elements
    highlight: {
      lastMove: true, // add last-move class to squares
      check: true, // add check class to squares
      dragOver: true, // add drag-over class to square when dragging over it
    },
    animation: {
      enabled: true,
      duration: 200,
      /*{ // current
       *  start: timestamp,
       *  duration: ms,
       *  anims: {
       *    a2: [
       *      [-30, 50], // animation goal
       *      [-20, 37]  // animation current status
       *    ], ...
       *  },
       *  fading: [
       *    {
       *      pos: [80, 120], // position relative to the board
       *      opacity: 0.34,
       *      role: 'rook',
       *      color: 'black'
       *    }
       *  }
       *}*/
      current: {},
    },
    movable: {
      free: true, // all moves are valid - board editor
      color: "both", // color that can move. white | black | both | null
      dests: {}, // valid moves. {"a2" ["a3" "a4"] "b1" ["a3" "c3"]} | null
      dropOff: "revert", // when a piece is dropped outside the board. "revert" | "trash"
      dropped: [], // last dropped [orig, dest], not to be animated
      showDests: true, // whether to add the move-dest class on squares
      events: {
        after: function (orig, dest, metadata) {}, // called after the move has been played
        afterNewPiece: function (role, pos) {}, // called after a new piece is dropped on the board
      },
      rookCastle: true, // castle by moving the king to the rook
    },
    premovable: {
      enabled: true, // allow premoves for color that can not move
      showDests: true, // whether to add the premove-dest class on squares
      castle: true, // whether to allow king castle premoves
      dests: [], // premove destinations for the current selection
      current: null, // keys of the current saved premove ["e2" "e4"] | null
      events: {
        set: function (orig, dest) {}, // called after the premove has been set
        unset: function () {}, // called after the premove has been unset
      },
    },
    predroppable: {
      enabled: false, // allow predrops for color that can not move
      current: {}, // current saved predrop {role: 'knight', key: 'e4'} | {}
      events: {
        set: function (role, key) {}, // called after the predrop has been set
        unset: function () {}, // called after the predrop has been unset
      },
    },
    draggable: {
      enabled: true, // allow moves & premoves to use drag'n drop
      distance: 3, // minimum distance to initiate a drag, in pixels
      autoDistance: true, // lets chessground set distance to zero when user drags pieces
      centerPiece: true, // center the piece on cursor at drag start
      showGhost: true, // show ghost of piece being dragged
      /*{ // current
       *  orig: "a2", // orig key of dragging piece
       *  rel: [100, 170] // x, y of the piece at original position
       *  pos: [20, -12] // relative current position
       *  dec: [4, -8] // piece center decay
       *  over: "b3" // square being moused over
       *  bounds: current cached board bounds
       *  started: whether the drag has started, as per the distance setting
       *}*/
      current: {},
    },
    selectable: {
      // disable to enforce dragging over click-click move
      enabled: true,
    },
    stats: {
      // was last piece dragged or clicked?
      // needs default to false for touch
      dragged: !("ontouchstart" in window),
    },
    events: {
      change: function () {}, // called after the situation changes on the board
      // called after a piece has been moved.
      // capturedPiece is null or like {color: 'white', 'role': 'queen'}
      move: function (orig, dest, capturedPiece) {},
      dropNewPiece: function (role, pos) {},
      capture: function (key, piece) {}, // DEPRECATED called when a piece has been captured
      select: function (key) {}, // called when a square is selected
    },
    items: null, // items on the board { render: key -> vdom }
    drawable: {
      enabled: false, // allows SVG drawings
      eraseOnClick: true,
      onChange: function (shapes) {},
      // user shapes
      shapes: [
        // {brush: 'green', orig: 'e8'},
        // {brush: 'yellow', orig: 'c4', dest: 'f7'}
      ],
      // computer shapes
      autoShapes: [
        // {brush: 'paleBlue', orig: 'e8'},
        // {brush: 'paleRed', orig: 'c4', dest: 'f7'}
      ],
      /*{ // current
       *  orig: "a2", // orig key of drawing
       *  pos: [20, -12] // relative current position
       *  dest: "b3" // square being moused over
       *  bounds: // current cached board bounds
       *  brush: 'green' // brush name for shape
       *}*/
      current: {},
      brushes: {
        green: {
          key: "g",
          color: "#15781B",
          opacity: 1,
          lineWidth: 10,
        },
        red: {
          key: "r",
          color: "#882020",
          opacity: 1,
          lineWidth: 10,
        },
        blue: {
          key: "b",
          color: "#003088",
          opacity: 1,
          lineWidth: 10,
        },
        yellow: {
          key: "y",
          color: "#e68f00",
          opacity: 1,
          lineWidth: 10,
        },
        paleBlue: {
          key: "pb",
          color: "#003088",
          opacity: 0.4,
          lineWidth: 15,
        },
        paleGreen: {
          key: "pg",
          color: "#15781B",
          opacity: 0.4,
          lineWidth: 15,
        },
        paleRed: {
          key: "pr",
          color: "#882020",
          opacity: 0.4,
          lineWidth: 15,
        },
        paleGrey: {
          key: "pgr",
          color: "#4a4a4a",
          opacity: 0.35,
          lineWidth: 15,
        },
      },
      // drawable SVG pieces, used for crazyhouse drop
      pieces: {
        baseUrl: "piece/cburnett/",
      },
      notation: 0,
    },
  };

  configure(defaults, cfg || {});

  return defaults;
};
