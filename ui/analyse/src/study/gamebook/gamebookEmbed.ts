import { piotr } from 'draughts'
import { read } from 'draughtsground/fen';
import { Key } from 'draughtsground/types'
import { field2key, movesDown100, movesUp100, movesHorizontal100, movesDown64, movesUp64, movesHorizontal64, opposite } from 'draughtsground/util'

function key2piotr(key: Key) {
  for (let p in piotr) {
    if (piotr[p] == key)
      return p;
  }
  return '';
}

export function calcDests(fen: string, variant: Variant): string {

  const pieces = read(fen),
    turnColor: Color = fen.substr(0, 1).toUpperCase() === 'W' ? 'white' : 'black',
    whitePlays = turnColor === 'white',
    piecesKeys: Key[] = Object.keys(pieces) as Key[];

  const frisianVariant = variant.key === 'frisian' || variant.key === 'frysk',
    is100 = variant.board.size[0] === 10,
    movesUp = is100 ? movesUp100 : movesUp64,
    movesDown = is100 ? movesDown100 : movesDown64,
    movesHorizontal = is100 ? movesHorizontal100 : movesHorizontal64;

  let allDests = '', captures = false;
  for (const key of piecesKeys) {
    const piece = pieces[key];
    if (piece && piece.color === turnColor) {

      let dests = '';
      const field = Number(key);
      if (piece.role === 'man') {
        for (let d = 0; d < (frisianVariant ? 3 : 2); d++) {
          const direction = d == 0 ? movesUp : (d == 2 ? movesHorizontal : movesDown);
          for (let i = 0; i < ((d != 2 && frisianVariant) ? 3 : 2); i++) {
            const f = direction[field][i];
            if (f !== -1) {
              let fkey = field2key(f),
                fpiece = pieces[fkey];
              if (!captures && !fpiece && i < 2 && d < 2 && (d == 0) == whitePlays)
                dests += key2piotr(fkey);
              else if (fpiece && (fpiece.role === 'man' || fpiece.role === 'king') && fpiece.color === opposite(turnColor)) {
                const f2 = direction[f][i];
                if (f2 !== -1) {
                  fkey = field2key(f2);
                  fpiece = pieces[fkey];
                  if (!fpiece) {
                    if (!captures) {
                      dests = '';
                      allDests = '';
                    }
                    dests += key2piotr(fkey);
                    captures = true;
                  }
                }
              }
            }
          }
        }
      } else if (piece.role === 'king') {
        for (let d = 0; d < (frisianVariant ? 3 : 2); d++) {
          const direction = d == 0 ? movesUp : (d == 2 ? movesHorizontal : movesDown);
          for (let i = 0; i < ((d != 2 && frisianVariant) ? 3 : 2); i++) {
            let f = direction[field][i];
            while (f !== -1) {
              let fkey = field2key(f),
                fpiece = pieces[fkey];
              if (!captures && !fpiece && i < 2 && d < 2)
                dests += key2piotr(fkey);
              else if (fpiece && (fpiece.role === 'man' || fpiece.role === 'king') && fpiece.color === opposite(turnColor)) {
                let f2 = direction[f][i];
                while (f2 !== -1) {
                  fkey = field2key(f2);
                  fpiece = pieces[fkey];
                  if (!fpiece) {
                    if (!captures) {
                      dests = '';
                      allDests = '';
                    }
                    dests += key2piotr(fkey);
                    captures = true;
                  } else
                    break;
                  f2 = direction[f2][i];
                }
                break;
              }
              f = direction[f][i];
            }
          }
        }
      }

      if (dests.length != 0) {
        if (allDests.length != 0) allDests += ' ';
        allDests += key2piotr(key) + dests;
      }

    }
  }

  return captures ? ('#1 ' + allDests) : allDests;

}