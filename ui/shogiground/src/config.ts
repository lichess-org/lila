import { HeadlessState } from './state';
import { setCheck, setSelected } from './board';
import { getDimensions, read as sfenRead } from './sfen';
import { DrawShape, DrawBrushes } from './draw';
import { makePockets } from './pocket';
import * as cg from './types';

export interface Config {
  sfen?: cg.Sfen; // shogi position in Forsyth notation
  hasPockets?: boolean;
  pockets?: string;
  orientation?: cg.Color; // board orientation. sente | gote
  turnColor?: cg.Color; // turn to play. sente | gote
  check?: cg.Color | boolean; // true for current color, false to unset
  lastMove?: cg.Key[]; // squares part of the last move ["c3", "c4"]
  selected?: cg.Key; // square currently selected "a1"
  coordinates?: boolean; // include coords attributes
  viewOnly?: boolean; // don't bind events: the user will never be able to move pieces around
  disableContextMenu?: boolean; // because who needs a context menu on a board
  blockTouchScroll?: boolean; // block scrolling via touch dragging on the board, e.g. for coordinate training
  resizable?: boolean; // listens to shogiground.resize on document.body to clear bounds cache
  // pieceKey: boolean; // add a data-key attribute to piece elements
  highlight?: {
    lastMove?: boolean; // add last-move class to squares
    check?: boolean; // add check class to squares
  };
  animation?: {
    enabled?: boolean;
    duration?: number;
  };
  movable?: {
    free?: boolean; // all moves are valid - board editor
    color?: cg.Color | 'both'; // color that can move. sente | gote | both | undefined
    dests?: cg.Dests; // valid moves. {"a2" ["a3" "a4"] "b1" ["a3" "c3"]}
    showDests?: boolean; // whether to add the move-dest class on squares
    events?: {
      after?: (orig: cg.Key, dest: cg.Key, metadata: cg.MoveMetadata) => void; // called after the move has been played
      afterNewPiece?: (role: cg.Role, key: cg.Key, metadata: cg.MoveMetadata) => void; // called after a new piece is dropped on the board
    };
  };
  premovable?: {
    enabled?: boolean; // allow premoves for color that can not move
    showDests?: boolean; // whether to add the premove-dest class on squares
    dests?: cg.Key[]; // premove destinations for the current selection
    events?: {
      set?: (orig: cg.Key, dest: cg.Key, metadata?: cg.SetPremoveMetadata) => void; // called after the premove has been set
      unset?: () => void; // called after the premove has been unset
    };
  };
  predroppable?: {
    enabled?: boolean; // allow predrops for color that can not move
    showDropDests?: boolean; // whether to add the premove-dest class on squares for drops
    dropDests?: cg.Key[]; // premove destinations for the drop selection
    events?: {
      set?: (role: cg.Role, key: cg.Key) => void; // called after the predrop has been set
      unset?: () => void; // called after the predrop has been unset
    };
  };
  draggable?: {
    enabled?: boolean; // allow moves & premoves to use drag'n drop
    distance?: number; // minimum distance to initiate a drag; in pixels
    autoDistance?: boolean; // lets shogiground set distance to zero when user drags pieces
    showGhost?: boolean; // show ghost of piece being dragged
    deleteOnDropOff?: boolean; // delete a piece when it is dropped off the board
  };
  selectable?: {
    // disable to enforce dragging over click-click move
    enabled?: boolean;
  };
  events?: {
    change?: () => void; // called after the situation changes on the board
    // called after a piece has been moved.
    // capturedPiece is undefined or like {color: 'sente'; 'role': 'bishop'}
    move?: (orig: cg.Key, dest: cg.Key, capturedPiece?: cg.Piece) => void;
    dropNewPiece?: (piece: cg.Piece, key: cg.Key) => void;
    select?: (key: cg.Key) => void; // called when a square is selected
    insert?: (elements: cg.Elements) => void; // when the board DOM has been (re)inserted
  };
  dropmode?: {
    active?: boolean;
    piece?: cg.Piece;
    showDropDests?: boolean; // whether to add the move-dest class on squares for drops
    dropDests?: cg.DropDests; // valid drops. {"pawn" ["a3" "a4"] "lance" ["a3" "c3"]}
  };
  drawable?: {
    enabled?: boolean; // can draw
    visible?: boolean; // can view
    eraseOnClick?: boolean;
    shapes?: DrawShape[];
    autoShapes?: DrawShape[];
    brushes?: DrawBrushes;
    pieces?: {
      baseUrl?: string;
    };
    onChange?: (shapes: DrawShape[]) => void; // called after drawable shapes change
  };
  notation?: cg.Notation;
  dimensions?: cg.Dimensions;
}

export function configure(state: HeadlessState, config: Config): void {
  // don't merge destinations and autoShapes. Just override.
  if (config.movable?.dests) state.movable.dests = undefined;
  if (config.dropmode?.dropDests) state.dropmode.dropDests = undefined;
  if (config.drawable?.autoShapes) state.drawable.autoShapes = [];

  merge(state, config);

  // if a sfen was provided, replace the pieces
  if (config.sfen) {
    state.dimensions = config.dimensions || getDimensions(config.sfen);
    const pieceToDrop = state.pieces.get('00');
    state.pieces = sfenRead(config.sfen, state.dimensions);
    if (pieceToDrop) state.pieces.set('00', pieceToDrop);
    state.drawable.shapes = [];
  }

  if (config.hasPockets) {
    state.pockets = makePockets(config.pockets);
  }

  // apply config values that could be undefined yet meaningful
  if ('check' in config) setCheck(state, config.check || false);
  if ('lastMove' in config && !config.lastMove) state.lastMove = undefined;
  // in case of drop last move, there's a single square.
  // if the previous last move had two squares,
  // the merge algorithm will incorrectly keep the second square.
  else if (config.lastMove) state.lastMove = config.lastMove;

  // fix move/premove dests
  if (state.selected) setSelected(state, state.selected);

  // no need for such short animations
  if (!state.animation.duration || state.animation.duration < 100) state.animation.enabled = false;
}

function merge(base: any, extend: any): void {
  for (const key in extend) {
    if (isObject(base[key]) && isObject(extend[key])) merge(base[key], extend[key]);
    else base[key] = extend[key];
  }
}

function isObject(o: unknown): boolean {
  return typeof o === 'object';
}
