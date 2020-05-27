import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
// import { GameData } from 'game';
import { Piece, Pieces } from 'draughtsground/types';
import { pos2key, key2pos, allKeys } from 'draughtsground/util';
import { Setting, makeSetting } from './setting';

export type Style = 'notation' | 'short' | 'full';

const filesTop = [' ', '1', ' ', '2', ' ', '3', ' ', '4', ' ', '5'],
      filesBottom = ['46', '', '47', '', '48', '', '49', '', '50'];
const ranks = ['  ', ' 6', '  ', '16', '  ', '26', '  ', '36', '  ', '46'],
      ranksInv = [' 5', '  ', '15', '  ', '25', '  ', '35', '  ', '45', '  '];
const roles: { [letter: string]: string } = { M: 'man', K: 'king', X: 'captured' };
const letters = { man: 'm', king: 'k', ghostman: 'x', ghostking: 'x' };

export function styleSetting(): Setting<Style> {
  return makeSetting<Style>({
    choices: [
      ['notation', 'Notation: 32-27, 18x29'],
      ['short', 'Short: 32 27, 18 takes 29'],
      ['full', 'Full: 32 to 27, 18 takes 29']
    ],
    default: 'full',
    storage: window.lidraughts.storage.make('nvui.moveNotation')
  });
}

function rolePlural(r: String, c: number) {
  if (r === 'man') return c > 1 ? 'men' : 'man';
  else return c > 1 ? r + 's' : r;
}

function roleName(r: String) {
  if (r === 'ghostman') return 'captured man';
  else if (r === 'ghostking') return 'captured king';
  else return r;
}

export function renderSan(san: San, style: Style) {
  if (!san) return ''
  else if (style === 'notation') return san;

  const lowerSan = san.toLowerCase(),
    isCapture = lowerSan.toLowerCase().includes('x'),
    fields = lowerSan.split(isCapture ? 'x' : '-');
  if (fields.length <= 1) return san;

  if (style === 'short') {
    if (isCapture) return [fields[0], 'takes', ...fields.slice(1)].join(' ');
    else return fields.join(' ');
  }
  return [fields[0], isCapture ? 'takes' : 'to', ...fields.slice(1)].join(' ');
}

export function renderPieces(pieces: Pieces): VNode {
  return h('div', ['white', 'black'].map(color => {
    const lists: any = [];
    ['king', 'man'].forEach(role => {
      const keys = [];
      for (let key in pieces) {
        if (pieces[key]!.color === color && pieces[key]!.role === role) keys.push(key);
      }
      if (keys.length) lists.push([rolePlural(role, keys.length), ...keys.sort().map(key => key[0] === '0' ? key.slice(1) : key)]);
    });
    return h('div', [
      h('h3', `${color} pieces`),
      ...lists.map((l: any) =>
        `${l[0]}: ${l.slice(1).join(', ')}`
      ).join(', ')
    ]);
  }));
}

export function renderPieceKeys(pieces: Pieces, p: string): string {
  let name = `${p === p.toUpperCase() ? 'white' : 'black'} ${roles[p.toUpperCase()]}`;
  let res: Key[] = [], piece: Piece | undefined;
  for (let k in pieces) {
    piece = pieces[k];
    if (piece && `${piece.color} ${piece.role}` === name) res.push(k as Key);
  }
  return `${name}: ${res.length ? res.sort().map(key => key[0] === '0' ? key.slice(1) : key).join(', ') : 'none'}`;
}

export function renderPiecesOn(pieces: Pieces, boardSize: [number, number], lineNumber: number, reverse?: boolean): string {
  let res: string[] = [], piece: Piece | undefined;
  for (let k of allKeys) {
    if (key2pos(k, boardSize)[1] === lineNumber) {
      piece = pieces[k];
      res.push(piece ? `${piece.color} ${roleName(piece.role)}` : 'empty');
    }
  }
  return (reverse ? res.reverse() : res).join(', ');
}

/*
      1     2     3     4     5
   -  M  -  M  -  M  -  M  -  M  
6  M  -  M  -  M  -  M  -  M  -  
   -  M  -  M  -  M  -  M  -  M  
16 M  -  M  -  M  -  M  -  M  -  
   -  +  -  +  -  +  -  +  -  +
26 +  -  +  -  +  -  +  -  +  -
   -  m  -  m  -  m  -  m  -  m  
36 m  -  m  -  m  -  m  -  m  -  
   -  m  -  m  -  m  -  m  -  m  
46 m  -  m  -  m  -  m  -  m  -  
   46    47    48    49    50
 */

export function renderBoard(pieces: Pieces, boardSize: [number, number], pov: Color): string {
  const white = pov === 'white',
    board = [white ? ['  ', ...filesTop] : [...filesTop, '  ']];
  for(let y = 1; y <= boardSize[1]; y++) {
    let line = [];
    for(let x = 0; x < boardSize[0]; x++) {
      const piece = (x % 2 !== y % 2) ? undefined : pieces[pos2key([(x - y % 2) / 2 + 1, y], boardSize)];
      if (piece) {
        const letter = letters[piece.role];
        line.push(piece.color === 'white' ? letter.toUpperCase() : letter);
      } else line.push((x % 2 !== y % 2) ? '-' : '+');
    }
    board.push(white ? ['' + ranks[y - 1], ...line] : [...line, '' + ranksInv[y - 1]]);
  }
  board.push(white ? ['  ', ...filesBottom] : [...filesBottom, ' ', '  ']);
  if (!white) {
    board.reverse();
    board.forEach(r => r.reverse());
  }
  return board.map(line => line.join(' ')).join('\n');
}
