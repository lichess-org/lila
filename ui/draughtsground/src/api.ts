import { State } from './state'
import * as board from './board'
import { write as fenWrite } from './fen'
import { Config, configure, setKingMoves } from './config'
import { anim, render } from './anim'
import { cancel as dragCancel, dragNewPiece } from './drag'
import { DrawShape } from './draw'
import explosion from './explosion'
import * as cg from './types'

export interface Api {

  // reconfigure the instance. Accepts all config options, except for viewOnly & drawable.visible.
  // board will be animated accordingly, if animations are enabled.
  set(config: Config, noCaptSequences?: boolean): void;

  // read draughtsground state; write at your own risks.
  state: State;

  // get the position as a FEN string (only contains pieces, no flags)
  // e.g. W31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50:B1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20
  getFen(algebraic?: boolean): cg.FEN;

  // change the view angle
  toggleOrientation(): void;

  // perform a move programmatically
  move(orig: cg.Key, dest: cg.Key, finishCapture?: Boolean): void;

  // add and/or remove arbitrary pieces on the board
  setPieces(pieces: cg.PiecesDiff): void;

  // update kingmoves counters
  setKingMoves(kingMoves: cg.KingMoves): void;

  // click a square programmatically
  selectSquare(key: cg.Key | null, force?: boolean): void;

  // put a new piece on the board
  newPiece(piece: cg.Piece, key: cg.Key): void;

  // play the current premove, if any; returns true if premove was played
  playPremove(): boolean;

  // cancel the current premove, if any
  cancelPremove(): void;

  // play the current predrop, if any; returns true if premove was played
  playPredrop(validate: (drop: cg.Drop) => boolean): boolean;

  // cancel the current predrop, if any
  cancelPredrop(): void;

  // cancel the current move being made
  cancelMove(): void;

  // cancel current move and prevent further ones
  stop(): void;

  // make squares explode (atomic chess)
  explode(keys: cg.Key[]): void;

  // programmatically draw user shapes
  setShapes(shapes: DrawShape[]): void;

  // programmatically draw auto shapes
  setAutoShapes(shapes: DrawShape[]): void;

  // square name at this DOM position (like "e4")
  getKeyAtDomPos(pos: cg.NumberPair): cg.Key | undefined;

  // only useful when CSS changes the board width/height ratio (for 3D)
  redrawAll: cg.Redraw;

  // for crazyhouse and board editors
  dragNewPiece(piece: cg.Piece, event: cg.MouchEvent, force?: boolean): void;

  // unbinds all events
  // (important for document-wide events like scroll and mousemove)
  destroy: cg.Unbind
}

// see API types and documentations in dts/api.d.ts
export function start(state: State, redrawAll: cg.Redraw): Api {

  function toggleOrientation() {
    board.toggleOrientation(state);
    redrawAll();
  };

  return {

    set(config, noCaptSequences: boolean = false) {
      if (config.orientation && config.orientation !== state.orientation) toggleOrientation();
      if (config.fen) {
        anim(state => configure(state, config), state, false, noCaptSequences);
        if (state.selected && !state.pieces[state.selected])
          state.selected = undefined;
      } else render(state => configure(state, config), state);
    },

    state,

    getFen: (algebraic?: boolean) => fenWrite(state.pieces, board.boardFields(state), algebraic),

    toggleOrientation,

    setPieces(pieces) {
      anim(state => board.setPieces(state, pieces), state);
    },

    selectSquare(key, force) {
      if (key) anim(state => board.selectSquare(state, key, force), state);
      else if (state.selected) {
        board.unselect(state);
        state.dom.redraw();
      }
    },

    move(orig, dest, finishCapture?: Boolean) {
      anim(state => board.baseMove(state, orig, dest, finishCapture), state);
    },

    newPiece(piece, key) {
      anim(state => board.baseNewPiece(state, piece, key), state);
    },

    setKingMoves(kingMoves: cg.KingMoves) {
      setKingMoves(state, kingMoves);
    },

    playPremove() {
      if (state.premovable.current) {
        const dest = state.premovable.current ? state.premovable.current[1] : '00';
        if (anim(board.playPremove, state)) {
          //If we can continue capturing keep the piece selected to enable quickly clicking all target squares one after the other
          if (state.movable.captLen && state.movable.captLen > 1)
            board.setSelected(state, dest);
          return true;
        }
        // if the premove couldn't be played, redraw to clear it up
        state.dom.redraw();
      }
      return false;
    },

    playPredrop(validate) {
      if (state.predroppable.current) {
        const result = board.playPredrop(state, validate);
        state.dom.redraw();
        return result;
      }
      return false;
    },

    cancelPremove() {
      render(board.unsetPremove, state);
    },

    cancelPredrop() {
      render(board.unsetPredrop, state);
    },

    cancelMove() {
      render(state => { board.cancelMove(state); dragCancel(state); }, state);
    },

    stop() {
      render(state => { board.stop(state); dragCancel(state); }, state);
    },

    explode(keys: cg.Key[]) {
      explosion(state, keys);
    },

    setAutoShapes(shapes: DrawShape[]) {
      render(state => state.drawable.autoShapes = shapes, state);
    },

    setShapes(shapes: DrawShape[]) {
      render(state => state.drawable.shapes = shapes, state);
    },

    getKeyAtDomPos(pos) {
      return board.getKeyAtDomPos(pos, state.boardSize, board.whitePov(state), state.dom.bounds());
    },

    redrawAll,

    dragNewPiece(piece, event, force) {
      dragNewPiece(state, piece, event, force)
    },

    destroy() {
      board.stop(state);
      state.dom.unbind && state.dom.unbind();
      state.dom.destroyed = true;
    }
  };
}
