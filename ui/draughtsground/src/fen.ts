import * as cg from './types'
import { allKeys, algebraicKeys } from './util'

export const initial: cg.FEN = 'W31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50:B1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20';

export function read(fen: cg.FEN, fields?: number): cg.Pieces {
  const pieces: cg.Pieces = {};
  if (!fen) return pieces;
  if (fen === 'start') fen = initial;
  for (let fenPart of fen.split(':')) {
    if (fenPart.length <= 1) continue;
    let first = fenPart.slice(0, 1), clr: cg.Color;
    if (first === 'W') clr = 'white';
    else if (first === 'B') clr = 'black';
    else continue;
    const fenPieces = fenPart.slice(1).split(',');
    for (let fenPiece of fenPieces) {
      if (!fenPiece) continue;
      let fieldStr, role: cg.Role;
      switch (fenPiece.slice(0, 1)) {
        case 'K':
          role = 'king';
          fieldStr = fenPiece.slice(1);
          break;
        case 'G':
          role = 'ghostman';
          fieldStr = fenPiece.slice(1);
          break;
        case 'P':
          role = 'ghostking';
          fieldStr = fenPiece.slice(1);
          break;
        default:
          role = 'man';
          fieldStr = fenPiece;
          break;
      }
      if (fieldStr.length === 1) fieldStr = '0' + fieldStr;
      let fieldNumber = parseInt(fieldStr);
      if (!fieldNumber) {
        fieldNumber = algebraicKeys.indexOf(fieldStr) + 1;
      }
      if (fieldNumber && (!fields || fieldNumber <= fields)) {
        pieces[fieldStr as cg.Key] = {
          color: clr,
          role
        };
      }
    }
  }
  return pieces;
}

export function write(pieces: cg.Pieces, fields?: number, algebraic?: boolean): cg.FEN {
  const max = fields || 50;
  let fenW = 'W', fenB = 'B';
  for (let f = 1; f <= max; f++) {
    const key = allKeys[f - 1], piece = pieces[key];
    if (!piece) continue;
    if (piece.color === 'white') {
      if (fenW.length > 1) fenW += ',';
      if (piece.role === 'king')
        fenW += 'K';
      else if (piece.role === 'ghostman')
        fenW += 'G';
      else if (piece.role === 'ghostking')
        fenW += 'P';
      if (algebraic) fenW += algebraicKeys[f - 1];
      else fenW += f.toString();
    } else {
      if (fenB.length > 1) fenB += ',';
      if (piece.role === 'king')
        fenB += 'K';
      else if (piece.role === 'ghostman')
        fenB += 'G';
      else if (piece.role === 'ghostking')
        fenB += 'P';
      if (algebraic) fenB += algebraicKeys[f - 1];
      else fenB += f.toString();
    }
  }
  return fenW + ':' + fenB;
}

export function toggleCoordinates(fen: cg.FEN, algebraic: boolean, fields?: number): string {
  if (!fen) return fen;
  if (fen === 'start') fen = initial;
  let fenOut = '', fenW = 'W', fenB = 'B';
  for (let fenPart of fen.split(':')) {
    let first = fenPart.slice(0, 1), clr: boolean;
    if (first === 'W') clr = true;
    else if (first === 'B') clr = false;
    else continue;
    if (fenPart.length === 1) {
      fenOut = first;
      continue;
    }
    const fenPieces = fenPart.slice(1).split(',');
    for (let fenPiece of fenPieces) {
      if (!fenPiece) continue;
      let fieldStr, role = fenPiece.slice(0, 1);
      switch (role) {
        case 'K':
        case 'G':
        case 'P':
          fieldStr = fenPiece.slice(1);
          break;
        default:
          fieldStr = fenPiece;
          role = '';
          break;
      }
      if (algebraic) {
        const fieldNumber = parseInt(fieldStr);
        if (fieldNumber && (!fields || fieldNumber <= fields)) {
          if (clr) {
            if (fenW.length > 1) fenW += ',';
            fenW += role + algebraicKeys[fieldNumber - 1];
          } else {
            if (fenB.length > 1) fenB += ',';
            fenB += role + algebraicKeys[fieldNumber - 1];
          }
        } else if (!fieldNumber && algebraicKeys.includes(fieldStr)) {
          return fen; // assume the FEN is already algebraic
        }
      } else {
        const coordIndex = algebraicKeys.indexOf(fieldStr);
        if (coordIndex !== -1 && (!fields || coordIndex < fields)) {
          if (clr) {
            if (fenW.length > 1) fenW += ',';
            fenW += role + (coordIndex + 1).toString();
          } else {
            if (fenB.length > 1) fenB += ',';
            fenB += role + (coordIndex + 1).toString();
          }
        } else if (coordIndex === -1 && parseInt(fieldStr)) {
          return fen; // assume the FEN is already fieldnumbers
        }
      }
    }
  }
  return (fenOut ? fenOut + ':' + fenW : fenW) + ':' + fenB;
}

export function countGhosts(fen: cg.FEN): number {
  if (!fen) return 0;
  if (fen === 'start') fen = initial;
  let ghosts = 0;
  for (let fenPart of fen.split(':')) {
    if (fenPart.length <= 1) continue;
    let first = fenPart.slice(0, 1);
    if (first === 'W' || first === 'B') {
      const fenPieces = fenPart.slice(1).split(',');
      for (let fenPiece of fenPieces) {
        first = fenPiece.slice(0, 1);
        if (first === 'G' || first === 'P')
          ghosts++;
      }
    }
  }
  return ghosts;
}

export function readKingMoves(fen: cg.FEN): cg.KingMoves | undefined {
  if (!fen) return undefined;
  if (fen === 'start') fen = initial;

  const fenParts = fen.split(':'),
    kingMoves = fenParts.length ? fenParts[fenParts.length - 1] : '';
  if (kingMoves.indexOf('+') !== 0)
    return undefined;

  const playerMoves = kingMoves.split('+').filter(function (e) { return e.length != 0; });
  if (playerMoves.length !== 2)
    return undefined;

  const whiteMoves = parseInt(playerMoves[1].slice(0, 1)),
    blackMoves = parseInt(playerMoves[0].slice(0, 1)),
    result: cg.KingMoves = { 
      white: { count: whiteMoves },
      black: { count: blackMoves }
    };
  if (whiteMoves > 0)
    result.white.key = playerMoves[1].slice(1) as cg.Key;
  if (blackMoves > 0)
    result.black.key = playerMoves[0].slice(1) as cg.Key;

  return result;
}