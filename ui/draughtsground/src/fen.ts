import * as cg from './types'

export const initial: cg.FEN = 'W31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50:B1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20';

export function read(fen: cg.FEN): cg.Pieces {
  const pieces: cg.Pieces = {};
  if (!fen) return pieces;
  if (fen === 'start') fen = initial;
  for (let fenPart of fen.split(':')) {
    if (fenPart.length <= 1) continue;
    let first = fenPart.slice(0, 1), clr;
    if (first === 'W') clr = 'white';
    else if (first === 'B') clr = 'black';
    else continue;
    const fenPieces = fenPart.slice(1).split(',');
    for (let fenPiece of fenPieces) {
      if (!fenPiece) continue;
      let fieldNumber, role;
      switch (fenPiece.slice(0, 1)) {
        case 'K':
          role = 'king';
          fieldNumber = fenPiece.slice(1);
          break;
        case 'G':
          role = 'ghostman';
          fieldNumber = fenPiece.slice(1);
          break;
        case 'P':
          role = 'ghostking';
          fieldNumber = fenPiece.slice(1);
          break;
        default:
          role = 'man';
          fieldNumber = fenPiece;
          break;
      }
      if (fieldNumber.length === 1) fieldNumber = '0' + fieldNumber;
      pieces[fieldNumber as cg.Key] = {
        role: role as cg.Role,
        color: clr as cg.Color
      };
    }
  }
  return pieces;
}

export function write(pieces: cg.Pieces): cg.FEN {
  let fenW = 'W', fenB = 'B';
  for (let f = 1; f <= 50; f++) {
    const fStr = f.toString(), piece = pieces[(f < 10 ? '0' + fStr : fStr) as cg.Key];
    if (!piece) continue;
    if (piece.color === 'white') {
      if (fenW.length > 1) fenW += ',';
      if (piece.role === 'king')
        fenW += 'K';
      else if (piece.role === 'ghostman')
        fenW += 'G';
      else if (piece.role === 'ghostking')
        fenW += 'P';
      fenW += fStr;
    } else {
      if (fenB.length > 1) fenB += ',';
      if (piece.role === 'king')
        fenB += 'K';
      else if (piece.role === 'ghostman')
        fenB += 'G';
      else if (piece.role === 'ghostking')
        fenB += 'P';
      fenB += fStr;
    }
  }
  return fenW + ':' + fenB;
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