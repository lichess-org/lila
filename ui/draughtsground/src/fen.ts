import * as cg from './types'

export const initial: cg.FEN = 'W31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50:B1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20';

export function read(fen: cg.FEN): cg.Pieces {

  if (fen === 'start') fen = initial;
  const pieces: cg.Pieces = {};

  const fenParts: string[] = fen.split(':');
  for (let i = 0; i < fenParts.length; i++) {
    let clr: string = "";
    if (fenParts[i].slice(0, 1) === "W")
      clr = "white";
    else if (fenParts[i].slice(0, 1) === "B")
      clr = "black";
    if (clr.length !== 0 && fenParts[i].length > 1) {

      const fenPieces: string[] = fenParts[i].slice(1).split(',');
      for (let k = 0; k < fenPieces.length; k++) {
        let fieldNumber: string, role: cg.Role;
        if (fenPieces[k].slice(0, 1) === "K") {
          role = "king" as cg.Role;
          fieldNumber = fenPieces[k].slice(1);
        } else if (fenPieces[k].slice(0, 1) === "G") {
          role = "ghostman" as cg.Role;
          fieldNumber = fenPieces[k].slice(1);
        } else if (fenPieces[k].slice(0, 1) === "P") {
          role = "ghostking" as cg.Role;
          fieldNumber = fenPieces[k].slice(1);
        } else {
          role = "man" as cg.Role;
          fieldNumber = fenPieces[k];
        }
        if (fieldNumber.length == 1) fieldNumber = "0" + fieldNumber;
        pieces[fieldNumber as cg.Key] = {
          role: role,
          color: clr as cg.Color
        };
      }

    }
  }

  return pieces;
}

export function write(pieces: cg.Pieces): cg.FEN {

  let fenW: string = "W";
  let fenB: string = "B";

  for (let f = 1; f <= 50; f++) {

    const piece = pieces[(f < 10 ? "0" + f.toString() : f.toString()) as cg.Key];
    if (piece) {
      if (piece.color === "white") {
        if (fenW.length > 1) fenW += ',';
        if (piece.role === "king")
          fenW += "K";
        else if (piece.role === "ghostman")
          fenW += "G";
        else if (piece.role === "ghostking")
          fenW += "P";
        fenW += f.toString();
      } else {
        if (fenB.length > 1) fenB += ',';
        if (piece.role === "king")
          fenB += "K";
        else if (piece.role === "ghostman")
          fenB += "G";
        else if (piece.role === "ghostking")
          fenB += "P";
        fenB += f.toString();
      }
    }
  }

  return fenW + ":" + fenB;

}

export function countGhosts(fen: cg.FEN): number {

  if (fen === 'start') fen = initial;
  var ghosts: number = 0;

  const fenParts: string[] = fen.split(':');
  for (let i = 0; i < fenParts.length; i++) {
    let clr: string = "";
    if (fenParts[i].slice(0, 1) === "W")
      clr = "white";
    else if (fenParts[i].slice(0, 1) === "B")
      clr = "black";
    if (clr.length !== 0 && fenParts[i].length > 1) {

      const fenPieces: string[] = fenParts[i].slice(1).split(',');
      for (let k = 0; k < fenPieces.length; k++) {
        if (fenPieces[k].slice(0, 1) === "G" || fenPieces[k].slice(0, 1) === "P")
          ghosts++;
      }

    }
  }

  return ghosts;
}