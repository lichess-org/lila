import { State } from './state'
import { setSelected, boardFields } from './board'
import { readKingMoves, read as fenRead } from './fen'
import { DrawShape, DrawBrush } from './draw'
import * as cg from './types'

export interface Config {
  fen?: cg.FEN; // draughts position in Forsyth notation
  boardSize?: cg.BoardSize; // board size (width / height)
  orientation?: cg.Color; // board orientation. white | black
  turnColor?: cg.Color; // turn to play. white | black
  captureLength?: number; //Amount of forced captures in this turn
  lastMove?: cg.Key[]; // squares part of the last move ["c3", "c4"]
  selected?: cg.Key; // square currently selected "a1"
  coordinates?: number; // include coords attributes
  coordSystem?: number; // coordinate system (0 = fieldnumbers, 1 = algebraic)
  viewOnly?: boolean; // don't bind events: the user will never be able to move pieces around
  disableContextMenu?: boolean; // because who needs a context menu on a draughtsboard
  resizable?: boolean; // listens to draughtsground.resize on document.body to clear bounds cache
  addPieceZIndex?: boolean; // adds z-index values to pieces (for 3D)
  // pieceKey: boolean; // add a data-key attribute to piece elements
  highlight?: {
    lastMove?: boolean; // add last-move class to squares
    check?: boolean; // add check class to squares
    kingMoves?: boolean; // amount of moves a king made for frisian variants
  };
  animation?: {
    enabled?: boolean;
    duration?: number;
  };
  movable?: {
    free?: boolean; // all moves are valid - board editor
    color?: cg.Color | 'both'; // color that can move. white | black | both | undefined
    dests?: {
      [key: string]: cg.Key[]
    }; // valid moves. {"a2" ["a3" "a4"] "b1" ["a3" "c3"]}
    showDests?: boolean; // whether to add the move-dest class on squares
    captureUci?: Array<string>; // possible multicaptures, when played by clicking to the final square (or first ambiguity)
    variant?: string; // game variant, to determine motion rules
    events?: {
      after?: (orig: cg.Key, dest: cg.Key, metadata: cg.MoveMetadata) => void; // called after the move has been played
      afterNewPiece?: (role: cg.Role, key: cg.Key, metadata: cg.MoveMetadata) => void; // called after a new piece is dropped on the board
    };
  };
  premovable?: {
    enabled?: boolean; // allow premoves for color that can not move
    showDests?: boolean; // whether to add the premove-dest class on squares
    castle?: boolean; // whether to allow king castle premoves
    variant?: string; // game variant, to determine premove squares
    dests?: cg.Key[]; // premove destinations for the current selection
    events?: {
      set?: (orig: cg.Key, dest: cg.Key, metadata?: cg.SetPremoveMetadata) => void; // called after the premove has been set
      unset?: () => void;  // called after the premove has been unset
    }
  };
  predroppable?: {
    enabled?: boolean; // allow predrops for color that can not move
    events?: {
      set?: (role: cg.Role, key: cg.Key) => void; // called after the predrop has been set
      unset?: () => void; // called after the predrop has been unset
    }
  };
  draggable?: {
    enabled?: boolean; // allow moves & premoves to use drag'n drop
    distance?: number; // minimum distance to initiate a drag; in pixels
    autoDistance?: boolean; // lets draughtsground set distance to zero when user drags pieces
    centerPiece?: boolean; // center the piece on cursor at drag start
    showGhost?: boolean; // show ghost of piece being dragged
    deleteOnDropOff?: boolean; // delete a piece when it is dropped off the board
  };
  selectable?: {
    // disable to enforce dragging over click-click move
    enabled?: boolean
  };
  events?: {
    change?: () => void; // called after the situation changes on the board
    // called after a piece has been moved.
    // capturedPiece is undefined or like {color: 'white'; 'role': 'king'}
    move?: (orig: cg.Key, dest: cg.Key, capturedPiece?: cg.Piece) => void;
    dropNewPiece?: (piece: cg.Piece, key: cg.Key) => void;
    select?: (key: cg.Key) => void; // called when a square is selected
    insert?: (elements: cg.Elements) => void; // when the board DOM has been (re)inserted
  };
  items?: (pos: cg.Pos, key: cg.Key) => any | undefined; // items on the board { render: key -> vdom }
  drawable?: {
    enabled?: boolean; // can draw
    visible?: boolean; // can view
    eraseOnClick?: boolean;
    shapes?: DrawShape[];
    autoShapes?: DrawShape[];
    brushes?: DrawBrush[];
    pieces?: {
      baseUrl?: string;
    }
  }
}

export function configure(state: State, config: Config) {

  // don't merge destinations. Just override.
  if (config.movable && config.movable.dests) state.movable.dests = undefined;
  if (config.movable && config.movable.captureUci) state.movable.captureUci = undefined;

  merge(state, config);  

  if (config.fen) {

    // if a fen was provided, replace the pieces
    state.pieces = fenRead(config.fen, boardFields(state));
    state.drawable.shapes = [];
    
    // show kingmoves for frisian variants
    if (state.highlight && state.highlight.kingMoves) {
      const kingMoves = readKingMoves(config.fen);
      if (kingMoves) doSetKingMoves(state, kingMoves);
    }
  }

  // apply config values that could be undefined yet meaningful
  if (config.hasOwnProperty('lastMove') && !config.lastMove) {
    state.lastMove = undefined;
    state.animateFrom = undefined;
  }
  // in case of ZH drop last move, there's a single square.
  // if the previous last move had two squares,
  // the merge algorithm will incorrectly keep the second square.
  else if (config.lastMove) {
    state.lastMove = config.lastMove;
    state.animateFrom = undefined;
  }

  if (config.captureLength !== undefined)
    state.movable.captLen = config.captureLength;

  // fix move/premove dests
  if (state.selected)
    setSelected(state, state.selected);

  // no need for such short animations
  if (!state.animation.duration || state.animation.duration < 100) state.animation.enabled = false;

};

export function setKingMoves(state: State, kingMoves: cg.KingMoves) {
  const fields = boardFields(state);
  for (let f = 1; f <= fields; f++) {
    const key = (f < 10 ? '0' + f.toString() : f.toString()) as cg.Key,
      piece = state.pieces[key];
    if (piece && piece.kingMoves)
      piece.kingMoves = undefined;
  }
  doSetKingMoves(state, kingMoves);
}

function doSetKingMoves(state: State, kingMoves: cg.KingMoves) {
  if (kingMoves.white.count > 0 && kingMoves.white.key) {
    const piece = state.pieces[kingMoves.white.key];
    if (piece && piece.role === 'king' && piece.color === 'white')
      piece.kingMoves = kingMoves.white.count;
  }

  if (kingMoves.black.count > 0 && kingMoves.black.key) {
    const piece = state.pieces[kingMoves.black.key];
    if (piece && piece.role === 'king' && piece.color === 'black')
      piece.kingMoves = kingMoves.black.count;
  }
}

function merge(base: any, extend: any) {
  for (let key in extend) {
    if (isObject(base[key]) && isObject(extend[key])) merge(base[key], extend[key]);
    else base[key] = extend[key];
  }
}

function isObject(o: any): boolean {
  return typeof o === 'object';
}
