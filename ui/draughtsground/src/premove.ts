import * as cg from './types'
import { field2key, movesDown100, movesUp100, movesHorizontal100, movesDown64, movesUp64, movesHorizontal64 } from './util'

export default function premove(pieces: cg.Pieces, boardSize: cg.BoardSize, key: cg.Key, variant?: string): cg.Key[] {

  const piece = pieces[key],
    field: number = Number(key);

  if (piece === undefined || isNaN(field)) return new Array<cg.Key>();

  const frisianVariant = variant && (variant === "frisian" || variant === "frysk"),
    is100 = boardSize[0] === 10,
    movesUp = is100 ? movesUp100 : movesUp64,
    movesDown = is100 ? movesDown100 : movesDown64,
    movesHorizontal = is100 ? movesHorizontal100 : movesHorizontal64;

  const dests: cg.Key[] = new Array<cg.Key>();
  switch (piece.role) {

    case 'man':

      //
      //It is always impossible to premove a capture if the first field in that direction contains a piece of our own color:
      //enemy pieces can never land there because you only take pieces from the board after capture sequence is completed
      //

      for (let i = 0; i < (frisianVariant ? 3 : 2); i++) {
        let f = movesUp[field][i];
        if (f != -1) {

          const key = field2key(f);
          if (piece.color === 'white' && i < 2)
            dests.push(key);

          const pc = pieces[key];
          if (pc === undefined || pc.color !== piece.color) {
            f = movesUp[f][i];
            if (f !== -1)
              dests.push(field2key(f));
          }

        }
      }

      for (let i = 0; i < (frisianVariant ? 3 : 2); i++) {
        let f = movesDown[field][i];
        if (f != -1) {

          const key = field2key(f);
          if (piece.color === 'black' && i < 2)
            dests.push(key);

          const pc = pieces[key];
          if (pc === undefined || pc.color !== piece.color) {
            f = movesDown[f][i];
            if (f !== -1)
              dests.push(field2key(f));
          }

        }
      }

      if (frisianVariant) {
        for (let i = 0; i < 2; i++) {
          let f = movesHorizontal[field][i];
          if (f != -1) {

            const pc = pieces[field2key(f)];
            if (pc === undefined || pc.color !== piece.color) {
              f = movesHorizontal[f][i];
              if (f !== -1)
                dests.push(field2key(f));
            }

          }
        }
      }

      break;

    case 'king':

      //
      //As far as I can tell there is no configuration of pieces that makes any square theoretically impossible to be premovable 
      //

      for (let i = 0; i < (frisianVariant ? 3 : 2); i++) {
        let f = movesUp[field][i], k = 0;
        while (f != -1) {
          if (i < 2 || k > 0)
            dests.push(field2key(f));
          f = movesUp[f][i];
          k++;
        }
      }

      for (let i = 0; i < (frisianVariant ? 3 : 2); i++) {
        let f = movesDown[field][i], k = 0;
        while (f != -1) {
          if (i < 2 || k > 0)
            dests.push(field2key(f));
          f = movesDown[f][i];
          k++;
        }
      }

      if (frisianVariant) {
        for (let i = 0; i < 2; i++) {
          let f = movesHorizontal[field][i], k = 0;
          while (f != -1) {
            if (k > 0)
              dests.push(field2key(f));
            f = movesHorizontal[f][i];
            k++;
          }
        }
      }

      break;

  }

  return dests;
};
