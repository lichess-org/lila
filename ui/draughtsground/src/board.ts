import { State } from './state'
import { pos2key, key2pos, opposite, containsX, allKeys } from './util'
import premove from './premove'
import * as cg from './types'

export type Callback = (...args: any[]) => void;

export function callUserFunction(f: Callback | undefined, ...args: any[]): void {
  if (f) setTimeout(() => f(...args), 1);
}

export function toggleOrientation(state: State): void {
  state.orientation = opposite(state.orientation);
  state.animation.current =
  state.draggable.current =
  state.selected = undefined;
}

export function reset(state: State): void {
  state.lastMove = undefined;
  unselect(state);
  unsetPremove(state);
  unsetPredrop(state);
}

export function setPieces(state: State, pieces: cg.PiecesDiff): void {
  for (let key in pieces) {
    const piece = pieces[key];
    if (piece) state.pieces[key] = piece;
    else delete state.pieces[key];
  }
}

function setPremove(state: State, orig: cg.Key, dest: cg.Key, meta: cg.SetPremoveMetadata): void {
  unsetPredrop(state);
  state.premovable.current = [orig, dest];
  callUserFunction(state.premovable.events.set, orig, dest, meta);
}

export function unsetPremove(state: State): void {
  if (state.premovable.current) {
    state.premovable.current = undefined;
    callUserFunction(state.premovable.events.unset);
  } 
}

function setPredrop(state: State, role: cg.Role, key: cg.Key): void {
  unsetPremove(state);
  state.predroppable.current = {
    role: role,
    key: key
  };
  callUserFunction(state.predroppable.events.set, role, key);
}

export function unsetPredrop(state: State): void {
  const pd = state.predroppable;
  if (pd.current) {
    pd.current = undefined;
    callUserFunction(pd.events.unset);
  }
}

export function calcCaptKey(pieces: cg.Pieces, startX: number, startY: number, destX: number, destY: number): cg.Key | undefined {

    const xDiff: number = destX - startX, yDiff: number = destY - startY;

    //Frisian captures always satisfy condition: (x = 0, y >= +-2) or (x = +-1, y = 0)
    //In normal captures these combination is impossible: x = 0 means y = 1, while y = 0 is impossible
    const yStep: number = yDiff === 0 ? 0 : (yDiff > 0 ? ((xDiff === 0 && Math.abs(yDiff) >= 2) ? 2 : 1) : ((xDiff === 0 && Math.abs(yDiff) >= 2) ? -2 : -1));
    const xStep: number = xDiff === 0 ? 0 : (yDiff === 0 ? (xDiff > 0 ? 1 : -1) : (startY % 2 == 0 ? (xDiff < 0 ? -1 : 0) : (xDiff > 0 ? 1 : 0)));

    if (xStep === 0 && yStep === 0) return undefined;

    const captPos = [startX + xStep, startY + yStep] as cg.Pos;
    if (captPos === undefined) return undefined;

    const captKey: cg.Key = pos2key(captPos);

    const piece: cg.Piece | undefined = pieces[captKey];
    if (piece !== undefined && piece.role !== 'ghostman' && piece.role !== 'ghostking')
        return captKey
    else
        return calcCaptKey(pieces, startX + xStep, startY + yStep, destX, destY)

}

export function baseMove(state: State, orig: cg.Key, dest: cg.Key): cg.Piece | boolean {

    if (orig === dest || !state.pieces[orig]) return false;

    const origPos: cg.Pos = key2pos(orig), destPos: cg.Pos = key2pos(dest);
    const isCapture = (state.movable.captLen !== undefined && state.movable.captLen > 0);
    const captKey: cg.Key | undefined = isCapture ? calcCaptKey(state.pieces, origPos[0], origPos[1], destPos[0], destPos[1]) : undefined;
    const captPiece: cg.Piece | undefined = (isCapture && captKey) ? state.pieces[captKey] : undefined;
    const origPiece = state.pieces[orig];

    if (dest == state.selected) unselect(state);
    callUserFunction(state.events.move, orig, dest, captPiece);

    if (!state.movable.free && (state.movable.captLen === undefined || state.movable.captLen <= 1) && origPiece.role === 'man' && ((origPiece.color === 'white' && destPos[1] === 1) || (origPiece.color === 'black' && destPos[1] === 10)))
        state.pieces[dest] = {
            role: 'king',
            color: origPiece.color
        };
    else
        state.pieces[dest] = state.pieces[orig];

    delete state.pieces[orig];

    if (isCapture && captKey) {

        const captColor = state.pieces[captKey].color;
        const captRole = state.pieces[captKey].role;
        delete state.pieces[captKey]

        //Show a ghostpiece when we capture more than once
        if (state.movable.captLen !== undefined && state.movable.captLen > 1) {
            if (captRole === 'man') {
                state.pieces[captKey] = {
                    role: 'ghostman',
                    color: captColor
                };
            } else if (captRole === 'king') {
                state.pieces[captKey] = {
                    role: 'ghostking',
                    color: captColor
                };
            }
        } else {
            //Remove any remaing ghost pieces if capture sequence is done
            for (let i = 0; i < allKeys.length; i++) {
                const pc = state.pieces[allKeys[i]];
                if (pc !== undefined && (pc.role == 'ghostking' || pc.role == 'ghostman'))
                    delete state.pieces[allKeys[i]];
            }
        }
    }

    if (state.lastMove && state.lastMove.length > 0 && isCapture) {
        if (state.lastMove[state.lastMove.length - 1] == orig)
            state.lastMove.push(dest);
        else
            state.lastMove = [orig, dest];
    }
    else
        state.lastMove = [orig, dest];

    callUserFunction(state.events.change);
    return captPiece || true;

}

export function baseNewPiece(state: State, piece: cg.Piece, key: cg.Key, force?: boolean): boolean {
  if (state.pieces[key]) {
    if (force) delete state.pieces[key];
    else return false;
  }
  callUserFunction(state.events.dropNewPiece, piece, key);
  state.pieces[key] = piece;
  state.lastMove = [key];
  callUserFunction(state.events.change);
  state.movable.dests = undefined;
  state.turnColor = opposite(state.turnColor);
  return true;
}

function baseUserMove(state: State, orig: cg.Key, dest: cg.Key): cg.Piece | boolean {
    const result = baseMove(state, orig, dest);
    if (result) {
        state.movable.dests = undefined;
        if (!state.movable.captLen || state.movable.captLen <= 1)
            state.turnColor = opposite(state.turnColor);
        state.animation.current = undefined;
    }
    return result;
}

/**
 * User has finished a move, either by drag or click src->dest
 */
export function userMove(state: State, orig: cg.Key, dest: cg.Key): boolean {
  if (canMove(state, orig, dest)) {
    const result = baseUserMove(state, orig, dest);
    if (result) {
      const holdTime = state.hold.stop();
      unselect(state);
      const metadata: cg.MoveMetadata = {
        premove: false,
        ctrlKey: state.stats.ctrlKey,
        holdTime: holdTime
      };
      if (result !== true) metadata.captured = result;
      callUserFunction(state.movable.events.after, orig, dest, metadata);
      return true;
    }
  } else if (canPremove(state, orig, dest)) {
    setPremove(state, orig, dest, {
      ctrlKey: state.stats.ctrlKey
    });
    unselect(state);
  } else if (isMovable(state, dest) || isPremovable(state, dest)) {
    setSelected(state, dest);
    state.hold.start();
  } else unselect(state);
  return false;
}

export function dropNewPiece(state: State, orig: cg.Key, dest: cg.Key, force?: boolean): void {
  if (canDrop(state, orig, dest) || force) {
    const piece = state.pieces[orig];
    delete state.pieces[orig];
    baseNewPiece(state, piece, dest, force);
    callUserFunction(state.movable.events.afterNewPiece, piece.role, dest, {
      predrop: false
    });
  } else if (canPredrop(state, orig, dest)) {
    setPredrop(state, state.pieces[orig].role, dest);
  } else {
    unsetPremove(state);
    unsetPredrop(state);
  }
  delete state.pieces[orig];
  unselect(state);
}

export function selectSquare(state: State, key: cg.Key, force?: boolean): void {
    if (state.selected) {
        if (state.selected === key && !state.draggable.enabled) {
            unselect(state);
            state.hold.cancel();
        } else if ((state.selectable.enabled || force) && state.selected !== key) {
            if (userMove(state, state.selected, key)) {
                state.stats.dragged = false;
                //If we can continue capturing keep the piece selected to enable quickly clicking all target squares one after the other
                if (state.movable.captLen !== undefined && state.movable.captLen > 1)
                    setSelected(state, key);
            }
        } else state.hold.start();
    } else if (isMovable(state, key) || isPremovable(state, key)) {
        setSelected(state, key);
        state.hold.start();
    }
    callUserFunction(state.events.select, key);
}

export function setSelected(state: State, key: cg.Key): void {
  state.selected = key;
  if (isPremovable(state, key)) {
    state.premovable.dests = premove(state.pieces, key, state.premovable.variant);
  }
  else state.premovable.dests = undefined;
}

export function unselect(state: State): void {
  state.selected = undefined;
  state.premovable.dests = undefined;
  state.hold.cancel();
}

function isMovable(state: State, orig: cg.Key): boolean {
  const piece = state.pieces[orig];
  return piece && (
    state.movable.color === 'both' || (
      state.movable.color === piece.color &&
        state.turnColor === piece.color
    ));
}

export function canMove(state: State, orig: cg.Key, dest: cg.Key): boolean {
  return orig !== dest && isMovable(state, orig) && (
    state.movable.free || (!!state.movable.dests && containsX(state.movable.dests[orig], dest))
  );
}

function canDrop(state: State, orig: cg.Key, dest: cg.Key): boolean {
  const piece = state.pieces[orig];
  return piece && dest && (orig === dest || !state.pieces[dest]) && (
    state.movable.color === 'both' || (
      state.movable.color === piece.color &&
        state.turnColor === piece.color
    ));
}


function isPremovable(state: State, orig: cg.Key): boolean {
  const piece = state.pieces[orig];
  return piece && state.premovable.enabled &&
  state.movable.color === piece.color &&
    state.turnColor !== piece.color;
}

function canPremove(state: State, orig: cg.Key, dest: cg.Key): boolean {
  return orig !== dest &&
  isPremovable(state, orig) &&
  containsX(premove(state.pieces, orig, state.premovable.variant), dest);
}

function canPredrop(state: State, orig: cg.Key, dest: cg.Key): boolean {
  const piece = state.pieces[orig];
  return piece && dest &&
  (!state.pieces[dest] || state.pieces[dest].color !== state.movable.color) &&
  state.predroppable.enabled &&
  state.movable.color === piece.color &&
    state.turnColor !== piece.color;
}

export function isDraggable(state: State, orig: cg.Key): boolean {
  const piece = state.pieces[orig];
  return piece && state.draggable.enabled && (
    state.movable.color === 'both' || (
      state.movable.color === piece.color && (
        state.turnColor === piece.color || state.premovable.enabled
      )
    )
  );
}

export function playPremove(state: State): boolean {
  const move = state.premovable.current;
  if (!move) return false;
  const orig = move[0], dest = move[1];
  let success = false;
  if (canMove(state, orig, dest)) {
    const result = baseUserMove(state, orig, dest);
    if (result) {
      const metadata: cg.MoveMetadata = { premove: true };
      if (result !== true) metadata.captured = result;
      callUserFunction(state.movable.events.after, orig, dest, metadata);
      success = true;
    }
  }
  unsetPremove(state);
  return success;
}

export function playPredrop(state: State, validate: (drop: cg.Drop) => boolean): boolean {
  let drop = state.predroppable.current,
  success = false;
  if (!drop) return false;
  if (validate(drop)) {
    const piece = {
      role: drop.role,
      color: state.movable.color
    } as cg.Piece;
    if (baseNewPiece(state, piece, drop.key)) {
      callUserFunction(state.movable.events.afterNewPiece, drop.role, drop.key, {
        predrop: true
      });
      success = true;
    }
  }
  unsetPredrop(state);
  return success;
}

export function cancelMove(state: State): void {
  unsetPremove(state);
  unsetPredrop(state);
  unselect(state);
}

export function stop(state: State): void {
  state.movable.color =
  state.movable.dests =
  state.animation.current = undefined;
  cancelMove(state);
}

export function getKeyAtDomPos(pos: cg.NumberPair, asWhite: boolean, bounds: ClientRect): cg.Key | undefined {

    let row = Math.ceil(10 * ((pos[1] - bounds.top) / bounds.height));
    if (!asWhite) row = 11 - row;

    //On odd rows we skip fields 1,3,5 etc and on even rows 2,4,6 etc
    let col = Math.ceil(10 * ((pos[0] - bounds.left) / bounds.width));
    if (!asWhite) col = 11 - col;

    if (row % 2 !== 0) {
        if (col % 2 !== 0)
            return undefined;
        else
            col = col / 2;
    } else {
        if (col % 2 === 0)
            return undefined;
        else
            col = (col + 1) / 2;
    }

    return (col > 0 && col < 6 && row > 0 && row < 11) ? pos2key([col, row]) : undefined;

}
