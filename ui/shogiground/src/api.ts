import { State } from './state'
import * as board from './board'
import { write as fenWrite } from './fen'
import { Config, configure } from './config'
import { anim, render } from './anim'
import { cancel as dragCancel, dragNewPiece } from './drag'
import { DrawShape } from './draw'
import * as cg from './types'

export interface Api {

  // reconfigure the instance. Accepts all config options, except for viewOnly & drawable.visible.
  // board will be animated accordingly, if animations are enabled.
  set(config: Config): void;

  // read shogiground state; write at your own risks.
  state: State;

  // get the position as a FEN string (only contains pieces, no flags)
  getFen(): cg.FEN;

  // change the view angle
  toggleOrientation(): void;

  // perform a move programmatically
  move(orig: cg.Key, dest: cg.Key): void;

  // add and/or remove arbitrary pieces on the board
  setPieces(pieces: cg.PiecesDiff): void;

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
  destroy: cg.Unbind;
}

// see API types and documentations in dts/api.d.ts
export function start(state: State, redrawAll: cg.Redraw): Api {

  function toggleOrientation(): void {
    board.toggleOrientation(state);
    redrawAll();
  }

  return {

    set(config): void {
      if (config.orientation && config.orientation !== state.orientation) toggleOrientation();
      (config.fen ? anim : render)(state => configure(state, config), state);
    },

    state,

    getFen: () => fenWrite(state.pieces),

    toggleOrientation,

    setPieces(pieces): void {
      anim(state => board.setPieces(state, pieces), state);
    },

    selectSquare(key, force): void {
      if (key) anim(state => board.selectSquare(state, key, force), state);
      else if (state.selected) {
        board.unselect(state);
        state.dom.redraw();
      }
    },

    move(orig, dest): void {
      anim(state => board.baseMove(state, orig, dest), state);
    },

    newPiece(piece, key): void {
      anim(state => board.baseNewPiece(state, piece, key), state);
    },

    playPremove(): boolean {
      if (state.premovable.current) {
        if (anim(board.playPremove, state)) return true;
        // if the premove couldn't be played, redraw to clear it up
        state.dom.redraw();
      }
      return false;
    },

    playPredrop(validate): boolean {
      if (state.predroppable.current) {
        const result = board.playPredrop(state, validate);
        state.dom.redraw();
        return result;
      }
      return false;
    },

    cancelPremove(): void {
      render(board.unsetPremove, state);
    },

    cancelPredrop(): void {
      render(board.unsetPredrop, state);
    },

    cancelMove(): void {
      render(state => { board.cancelMove(state); dragCancel(state); }, state);
    },

    stop(): void {
      render(state => { board.stop(state); dragCancel(state); }, state);
    },

    setAutoShapes(shapes: DrawShape[]): void {
      render(state => state.drawable.autoShapes = shapes, state);
    },

    setShapes(shapes: DrawShape[]): void {
      render(state => state.drawable.shapes = shapes, state);
    },

    getKeyAtDomPos(pos): cg.Key | undefined {
      return board.getKeyAtDomPos(pos, board.whitePov(state), state.dom.bounds());
    },

    redrawAll,

    dragNewPiece(piece, event, force): void {
      dragNewPiece(state, piece, event, force)
    },

    destroy(): void {
      board.stop(state);
      state.dom.unbind && state.dom.unbind();
      state.dom.destroyed = true;
    }
  };
}
