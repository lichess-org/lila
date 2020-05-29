import * as fen from './fen'
import { AnimCurrent } from './anim'
import { DragCurrent } from './drag'
import { Drawable } from './draw'
import { timer } from './util'
import * as cg from './types';

export interface State {
  pieces: cg.Pieces;
  boardSize: cg.BoardSize;
  orientation: cg.Color; // board orientation. white | black
  turnColor: cg.Color; // turn to play. white | black
  lastMove?: cg.Key[]; // ucis of the last move [32, 27]
  animateFrom?: number; // startindex in lastMove to animate
  /** square currently selected "a1" */
  selected?: cg.Key;
  coordinates: number; // include coords attributes
  coordSystem?: number; // coordinate system (0 = fieldnumbers, 1 = algebraic)
  viewOnly: boolean; // don't bind events: the user will never be able to move pieces around
  disableContextMenu: boolean; // because who needs a context menu on a draughtsboard
  resizable: boolean; // listens to draughtsground.resize on document.body to clear bounds cache
  addPieceZIndex: boolean; // adds z-index values to pieces (for 3D)
  pieceKey: boolean; // add a data-key attribute to piece elements
  highlight: {
    lastMove: boolean; // add last-move class to squares
    check: boolean; // add check class to squares
    kingMoves?: boolean; // show amount of king moves for frisian variants
  };
  animation: {
    enabled: boolean;
    duration: number;
    current?: AnimCurrent;
  };
  movable: {
    free: boolean; // all moves are valid - board editor
    color?: cg.Color | 'both'; // color that can move. white | black | both
    dests?: cg.Dests; // valid moves. {"a2" ["a3" "a4"] "b1" ["a3" "c3"]}
    captLen?: number;
    captureUci?: Array<string>
    variant?: string; // game variant, to determine motion rules
    showDests: boolean; // whether to add the move-dest class on squares
    events: {
      after?: (orig: cg.Key, dest: cg.Key, metadata: cg.MoveMetadata) => void; // called after the move has been played
      afterNewPiece?: (role: cg.Role, key: cg.Key, metadata: cg.MoveMetadata) => void; // called after a new piece is dropped on the board
    };
  };
  premovable: {
    enabled: boolean; // allow premoves for color that can not move
    showDests: boolean; // whether to add the premove-dest class on squares
    dests?: cg.Key[]; // premove destinations for the current selection
    variant?: string; // game variant, to determine premove squares
    current?: cg.KeyPair; // keys of the current saved premove ["e2" "e4"]
    events: {
      set?: (orig: cg.Key, dest: cg.Key, metadata?: cg.SetPremoveMetadata) => void; // called after the premove has been set
      unset?: () => void;  // called after the premove has been unset
    }
  };
  predroppable: {
    enabled: boolean; // allow predrops for color that can not move
    current?: { // current saved predrop {role: 'knight'; key: 'e4'}
      role: cg.Role;
      key: cg.Key
    };
    events: {
      set?: (role: cg.Role, key: cg.Key) => void; // called after the predrop has been set
      unset?: () => void; // called after the predrop has been unset
    }
  };
  draggable: {
    enabled: boolean; // allow moves & premoves to use drag'n drop
    distance: number; // minimum distance to initiate a drag; in pixels
    autoDistance: boolean; // lets draughtsground set distance to zero when user drags pieces
    centerPiece: boolean; // center the piece on cursor at drag start
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
    enabled: boolean
  };
  stats: {
    // was last piece dragged or clicked?
    // needs default to false for touch
    dragged: boolean,
    ctrlKey?: boolean
  };
  events: {
    change?: () => void; // called after the situation changes on the board
    // called after a piece has been moved.
    // capturedPiece is undefined or like {color: 'white'; 'role': 'king'}
    move?: (orig: cg.Key, dest: cg.Key, capturedPiece?: cg.Piece) => void;
    dropNewPiece?: (piece: cg.Piece, key: cg.Key) => void;
    select?: (key: cg.Key) => void // called when a square is selected
    insert?: (elements: cg.Elements) => void; // when the board DOM has been (re)inserted
  };
  items?: (pos: cg.Pos, key: cg.Key) => any | undefined; // items on the board { render: key -> vdom }
  drawable: Drawable,
  exploding?: cg.Exploding;
  dom: cg.Dom,
  hold: cg.Timer
}

export function defaults(): Partial<State> {
  return {
    pieces: fen.read(fen.initial),
    boardSize: [10, 10],
    orientation: 'white',
    turnColor: 'white',
    coordinates: 2,
    viewOnly: false,
    disableContextMenu: false,
    resizable: true,
    addPieceZIndex: false,
    pieceKey: false,
    highlight: {
      lastMove: true,
      check: true
    },
    animation: {
      enabled: true,
      duration: 200
    },
    movable: {
      free: true,
      color: 'both',
      showDests: true,
      events: {}
    },
    premovable: {
      enabled: true,
      showDests: true,
      events: {}
    },
    predroppable: {
      enabled: false,
      events: {}
    },
    draggable: {
      enabled: true,
      distance: 3,
      autoDistance: true,
      centerPiece: true,
      showGhost: true,
      deleteOnDropOff: false
    },
    dropmode: {
      active: false
    },
    selectable: {
      enabled: true
    },
    stats: {
      // on touchscreen, default to "tap-tap" moves
      // instead of drag
      dragged: !('ontouchstart' in window)
    },
    events: {},
    drawable: {
      enabled: true, // can draw
      visible: true, // can view
      eraseOnClick: true,
      shapes: [],
      autoShapes: [],
      brushes: {
        green: { key: 'g', color: '#15781B', opacity: 1, lineWidth: 10 },
        red: { key: 'r', color: '#882020', opacity: 1, lineWidth: 10 },
        blue: { key: 'b', color: '#003088', opacity: 1, lineWidth: 10 },
        yellow: { key: 'y', color: '#e68f00', opacity: 1, lineWidth: 10 },
        paleBlue: { key: 'pb', color: '#003088', opacity: 0.4, lineWidth: 15 },
        paleBlue2: { key: 'pb2', color: '#003088', opacity: 0.65, lineWidth: 15 },
        paleBlue3: { key: 'pb3', color: '#003088', opacity: 0.3, lineWidth: 15 },
        paleGreen: { key: 'pg', color: '#15781B', opacity: 0.4, lineWidth: 15 },
        paleRed: { key: 'pr', color: '#882020', opacity: 0.4, lineWidth: 15 },
        paleGrey: { key: 'pgr', color: '#4a4a4a', opacity: 0.35, lineWidth: 15 }
      },
      pieces: {
        baseUrl: 'https://lidraughts.org/assets/piece/wide/'
      },
      prevSvgHash: ''
    },
    hold: timer()
  };
}
