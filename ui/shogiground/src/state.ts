import * as fen from "./fen";
import { AnimCurrent } from "./anim";
import { DragCurrent } from "./drag";
import { Drawable } from "./draw";
import { timer } from "./util";
import * as cg from "./types";

export interface State {
  pieces: cg.Pieces;
  orientation: cg.Color; // board orientation. white | black
  turnColor: cg.Color; // turn to play. white | black
  check?: cg.Key; // square currently in check "a2"
  lastMove?: cg.Key[]; // squares part of the last move ["c3"; "c4"]
  selected?: cg.Key; // square currently selected "a1"
  coordinates: boolean; // include coords attributes
  autoCastle: boolean; // immediately complete the castle by moving the rook after king move
  viewOnly: boolean; // don't bind events: the user will never be able to move pieces around
  disableContextMenu: boolean; // because who needs a context menu on a chessboard
  resizable: boolean; // listens to shogiground.resize on document.body to clear bounds cache
  addPieceZIndex: boolean; // adds z-index values to pieces (for 3D)
  pieceKey: boolean; // add a data-key attribute to piece elements
  highlight: {
    lastMove: boolean; // add last-move class to squares
    check: boolean; // add check class to squares
  };
  animation: {
    enabled: boolean;
    duration: number;
    current?: AnimCurrent;
  };
  movable: {
    free: boolean; // all moves are valid - board editor
    color?: cg.Color | "both"; // color that can move. white | black | both
    dests?: cg.Dests; // valid moves. {"a2" ["a3" "a4"] "b1" ["a3" "c3"]}
    showDests: boolean; // whether to add the move-dest class on squares
    events: {
      after?: (orig: cg.Key, dest: cg.Key, metadata: cg.MoveMetadata) => void; // called after the move has been played
      afterNewPiece?: (
        role: cg.Role,
        key: cg.Key,
        metadata: cg.MoveMetadata
      ) => void; // called after a new piece is dropped on the board
    };
    rookCastle: boolean; // castle by moving the king to the rook
  };
  premovable: {
    enabled: boolean; // allow premoves for color that can not move
    showDests: boolean; // whether to add the premove-dest class on squares
    castle: boolean; // whether to allow king castle premoves
    dests?: cg.Key[]; // premove destinations for the current selection
    current?: cg.KeyPair; // keys of the current saved premove ["e2" "e4"]
    events: {
      set?: (
        orig: cg.Key,
        dest: cg.Key,
        metadata?: cg.SetPremoveMetadata
      ) => void; // called after the premove has been set
      unset?: () => void; // called after the premove has been unset
    };
  };
  predroppable: {
    enabled: boolean; // allow predrops for color that can not move
    current?: {
      // current saved predrop {role: 'knight'; key: 'e4'}
      role: cg.Role;
      key: cg.Key;
    };
    events: {
      set?: (role: cg.Role, key: cg.Key) => void; // called after the predrop has been set
      unset?: () => void; // called after the predrop has been unset
    };
  };
  draggable: {
    enabled: boolean; // allow moves & premoves to use drag'n drop
    distance: number; // minimum distance to initiate a drag; in pixels
    autoDistance: boolean; // lets shogiground set distance to zero when user drags pieces
    showGhost: boolean; // show ghost of piece being dragged
    deleteOnDropOff: boolean; // delete a piece when it is dropped off the board
    current?: DragCurrent;
  };
  dropmode: {
    active: boolean;
    piece?: cg.Piece;
  };
  selectable: {
    // disable to enforce dragging over click-click move
    enabled: boolean;
  };
  stats: {
    // was last piece dragged or clicked?
    // needs default to false for touch
    dragged: boolean;
    ctrlKey?: boolean;
  };
  events: {
    change?: () => void; // called after the situation changes on the board
    // called after a piece has been moved.
    // capturedPiece is undefined or like {color: 'white'; 'role': 'queen'}
    move?: (orig: cg.Key, dest: cg.Key, capturedPiece?: cg.Piece) => void;
    dropNewPiece?: (piece: cg.Piece, key: cg.Key) => void;
    select?: (key: cg.Key) => void; // called when a square is selected
    insert?: (elements: cg.Elements) => void; // when the board DOM has been (re)inserted
  };
  drawable: Drawable;
  dom: cg.Dom;
  hold: cg.Timer;
  notation: cg.Notation;
}

export function defaults(): Partial<State> {
  return {
    pieces: fen.read(fen.initial),
    orientation: "white",
    turnColor: "white",
    coordinates: true,
    autoCastle: true,
    viewOnly: false,
    disableContextMenu: false,
    resizable: true,
    addPieceZIndex: false,
    pieceKey: false,
    highlight: {
      lastMove: true,
      check: true,
    },
    animation: {
      enabled: true,
      duration: 200,
    },
    movable: {
      free: true,
      color: "both",
      showDests: true,
      events: {},
      rookCastle: true,
    },
    premovable: {
      enabled: true,
      showDests: true,
      castle: true,
      events: {},
    },
    predroppable: {
      enabled: false,
      events: {},
    },
    draggable: {
      enabled: true,
      distance: 3,
      autoDistance: true,
      showGhost: true,
      deleteOnDropOff: false,
    },
    dropmode: {
      active: false,
    },
    selectable: {
      enabled: true,
    },
    stats: {
      // on touchscreen, default to "tap-tap" moves
      // instead of drag
      dragged: !("ontouchstart" in window),
    },
    events: {},
    drawable: {
      enabled: true, // can draw
      visible: true, // can view
      eraseOnClick: true,
      shapes: [],
      autoShapes: [],
      brushes: {
        green: { key: "g", color: "#15781B", opacity: 1, lineWidth: 10 },
        red: { key: "r", color: "#882020", opacity: 1, lineWidth: 10 },
        blue: { key: "b", color: "#003088", opacity: 1, lineWidth: 10 },
        yellow: { key: "y", color: "#e68f00", opacity: 1, lineWidth: 10 },
        paleBlue: { key: "pb", color: "#003088", opacity: 0.4, lineWidth: 15 },
        paleGreen: { key: "pg", color: "#15781B", opacity: 0.4, lineWidth: 15 },
        paleRed: { key: "pr", color: "#882020", opacity: 0.4, lineWidth: 15 },
        paleGrey: {
          key: "pgr",
          color: "#4a4a4a",
          opacity: 0.35,
          lineWidth: 15,
        },
      },
      pieces: {
        baseUrl: "https://lishogi.org/assets/piece/cburnett/",
      },
      prevSvgHash: "",
    },
    hold: timer(),
  };
}
