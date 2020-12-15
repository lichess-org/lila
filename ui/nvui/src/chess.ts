import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Pieces } from 'chessground/types';
import { Rank, File } from 'chessground/types';
import { invRanks, allKeys } from 'chessground/util';
import { Setting, makeSetting } from './setting';
import { files } from 'chessground/types';

export type Style = 'uci' | 'san' | 'literate' | 'nato' | 'anna';

const nato: { [letter: string]: string } = { a: 'alpha', b: 'bravo', c: 'charlie', d: 'delta', e: 'echo', f: 'foxtrot', g: 'golf', h: 'hotel' };
const anna: { [letter: string]: string } = { a: 'anna', b: 'bella', c: 'cesar', d: 'david', e: 'eva', f: 'felix', g: 'gustav', h: 'hector' };
const roles: { [letter: string]: string } = { P: 'pawn', R: 'rook', N: 'knight', B: 'bishop', Q: 'queen', K: 'king' };
const letters = { pawn: 'p', rook: 'r', knight: 'n', bishop: 'b', queen: 'q', king: 'k' };

export function supportedVariant(key: string) {
  return [
    'standard', 'chess960', 'kingOfTheHill', 'threeCheck', 'fromPosition'
  ].includes(key);
}

export function styleSetting(): Setting<Style> {
  return makeSetting<Style>({
    choices: [
      ['san', 'SAN: Nxf3'],
      ['uci', 'UCI: g1f3'],
      ['literate', 'Literate: knight takes f 3'],
      ['anna', 'Anna: knight takes felix 3'],
      ['nato', 'Nato: knight takes foxtrot 3']
    ],
    default: 'anna', // all the rage in OTB blind chess tournaments
    storage: lichess.storage.make('nvui.moveNotation')
  });
}

export function renderSan(san: San, uci: Uci | undefined, style: Style) {
  if (!san) return '';
  let move: string;
  if (san.includes('O-O-O')) move = 'long castling';
  else if (san.includes('O-O')) move = 'short castling';
  else if (style === 'san') move = san.replace(/[\+#]/, '');
  else if (style === 'uci') move = uci || san;
  else {
    move = san.replace(/[\+#]/, '').split('').map(c => {
      if (c == 'x') return 'takes';
      if (c == '+') return 'check';
      if (c == '#') return 'checkmate';
      if (c == '=') return 'promotion';
      const code = c.charCodeAt(0);
      if (code > 48 && code < 58) return c; // 1-8
      if (code > 96 && code < 105) return renderFile(c, style); // a-g
      return roles[c] || c;
    }).join(' ');
  }
  if (san.includes('+')) move += ' check';
  if (san.includes('#')) move += ' checkmate';
  return move;
}

export function renderPieces(pieces: Pieces, style: Style): VNode {
  return h('div', ['white', 'black'].map(color => {
    const lists: any = [];
    ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].forEach(role => {
      const keys = [];
      for (const [key, piece] of pieces) {
        if (piece.color === color && piece.role === role) keys.push(key);
      }
      if (keys.length) lists.push([`${role}${keys.length > 1 ? 's' : ''}`, ...keys]);
    });
    return h('div', [
      h('h3', `${color} pieces`),
      ...lists.map((l: any) =>
        `${l[0]}: ${l.slice(1).map((k: string) => renderKey(k, style)).join(', ')}`
      ).join(', ')
    ]);
  }));
}

export function renderPieceKeys(pieces: Pieces, p: string, style: Style): string {
  const name = `${p === p.toUpperCase() ? 'white' : 'black'} ${roles[p.toUpperCase()]}`;
  const res: Key[] = [];
  for (const [k, piece] of pieces) {
    if (piece && `${piece.color} ${piece.role}` === name) res.push(k as Key);
  }
  return `${name}: ${res.length ? res.map(k => renderKey(k, style)).join(', ') : 'none'}`;
}

export function renderPiecesOn(pieces: Pieces, rankOrFile: string, style: Style): string {
  const res: string[] = [];
  for (const k of allKeys) {
    if (k.includes(rankOrFile)) {
      const piece = pieces.get(k);
      if (piece) res.push(`${renderKey(k, style)} ${piece.color} ${piece.role}`);
    }
  }
  return res.length ? res.join(', ') : 'blank';
}

export function renderBoard(pieces: Pieces, pov: Color): VNode {
  const board = [[' ', ...files, ' ']];
  for (let rank of invRanks) {
    let line = [];
    for (let file of files) {
      let key = file + rank as Key;
      const piece = pieces.get(key);
      if (piece) {
        const letter = letters[piece.role];
        line.push(piece.color === 'white' ? letter.toUpperCase() : letter);
      } else line.push((key.charCodeAt(0) + key.charCodeAt(1)) % 2 ? '-' : '+');
    }
    board.push(['' + rank, ...line, '' + rank]);
  }
  board.push([' ', ...files, ' ']);
  if (pov === 'black') {
    board.reverse();
    board.forEach(r => r.reverse());
  }
  const boardString = board.map(line => line.join(' ')).join('\n');
  /* NEW CODE */
  const doFileHeaders = (pov: Color): VNode => {
    let fileHeaders = [
      h('th'),
      ...files.map(file => h('th', {attrs: {scope: 'col'}}, file)),
      h('th')
    ]
    return h('tr', pov === 'white' ? fileHeaders : fileHeaders.reverse());
  }
  const doPieceButton = (rank: Rank, file: File, text: string): VNode => {
    return h('button', {
      attrs: { rank: rank, file: file },
      hook: {
        insert: (vnode) => {
          const $btn = $(vnode.elm as HTMLButtonElement);
          $btn.on('click', (ev) => {
            const $evBtn = ev.target;
            const $pos = $evBtn.getAttribute('file') + $evBtn.getAttribute('rank');
            const $moveBox = document.querySelector('.move') as HTMLInputElement;
            console.log($pos);
            console.log($moveBox);
            // if the table has a hidden field for current move under it
            if ($moveBox) {
              if ($moveBox.value === '') {
                console.log("Set")
                $moveBox.value = $pos;
              } else {
                console.log("submit")
                $moveBox.value += $pos;
                const $label = $moveBox.parentElement;
                if ($label && $label.parentElement) {
                  ($label.parentElement as HTMLFormElement).dispatchEvent(new Event('submit')); 
                  console.log("goood!")
                }
              }
            }
          });
        }
      }
    }, text);
  }
  const doPiece = (rank: Rank, file: File): VNode => {
    const key = file + rank as Key;
    const piece = pieces.get(key);
    if (piece) {        
      const letter = letters[piece.role];
      let pieceLetters = piece.color === 'white' ? letter.toUpperCase() : letter;
      pieceLetters = (pieceLetters === pieceLetters.toUpperCase() ? 'w' : 'b') + pieceLetters;
      return h('td', doPieceButton(rank, file, pieceLetters));
    } else {
      const letter = (key.charCodeAt(0) + key.charCodeAt(1)) % 2 ? '-' : '+';
      return h('td', doPieceButton(rank, file, letter));
    }
  }
  const doRank = (pov: Color, rank: Rank): VNode => {      
    let rankElements = [
      h('th', {attrs: {scope: 'row'}}, rank),
      ...files.map(file => doPiece(rank, file)),
      h('th', {attrs: {scope: 'row'}}, rank)
    ];
    return h('tr', pov === 'white' ? rankElements : rankElements.reverse());
  }
  return h('tbody', [
    doFileHeaders(pov),
    ...invRanks.map(rank => doRank(pov, rank)),
    doFileHeaders(pov)
  ]);
  /* END OF NEW CODE */
  let rankEles = [];
  let splitBoard = boardString.split('\n');
  for (var i = 0; i < splitBoard.length; i++) {
    let fileEles = [];
    for (var j = 0; j < splitBoard[i].length; j++) {
      const tile = splitBoard[i][j];
      if (j === 0 || j === splitBoard[i].length-1) {
        fileEles.push(h('th', {attrs: {scope: 'row'}}, tile));
      } else if (tile !== ' ' && (i === 0 || i === splitBoard.length-1)) {
        fileEles.push(h('th', {attrs: {scope: 'col'}}, tile));
      } else if (tile !== ' ') {
        if (tile === '-' || tile === '+') {
          fileEles.push(h('td', tile));
        } else {
          fileEles.push(h('td', [h('button', {
            attrs: {
              rank: invRanks[i-1],
              file: files[j]
            },
            hook: {
              insert: (vnode) => {
                const $btn = $(vnode.elm as HTMLButtonElement);
                $btn.on('click', (ev) => {
                  const rank = ev.target.getAttribute('rank');
                  const file = ev.target.getAttribute('file');
                  console.log(rank + file);
                });
              }
            }
          }, (tile === tile.toUpperCase() ? "w" : "b") + tile)]));
        }
      }
    }
    rankEles.push(h('tr', fileEles));
  }
  rankEles.push(doFileHeaders(pov));
  let boardTable = h('tbody', rankEles);
  /*
  let boardTable = h('tbody', [
    h('tr', [
      h('td'),
      ...files.map(file => h('td', file)),
      h('td')
    ]),
    ...invRanks.map(rank => h('tr', [
      h('th', rank),
      ...files.map(function(file) {
        let key = file + rank as Key;
        const piece = pieces.get(key);
        const isBlack = ((key.charCodeAt(0) + key.charCodeAt(1)) % 2 === 0);
        let tdText = "";
        if (piece && pov === 'white') {
          const letter = letters[piece.role];
          tdText = piece.color === 'white' ? letter.toUpperCase() : letter;
        } else tdText = isBlack ? '-' : '+';
        return h('td.' + (isBlack ? 'black' : 'white'), tdText);
      }),
      h('th', rank)
    ])),
    h('tr', [
      h('td'),
      ...files.map(file => h('td', file)),
      h('td')
    ]),
  ]);*/
  return boardTable;
  /*
  let board = "<tbody><tr>";
  for (let file of files) {
   board += "<th>" + file + "</th>";
  }
  board += "</tr>";
  for (let rank of invRanks) {
    board += "<tr><th>" + rank + "<th>";
    for (let file of files) {
      board += "<td>";
      let key = file + rank as Key;
      const piece = pieces.get(key);
      if (piece){
        const letter = letters[piece.role];
        board += (pov === piece.color ? 'm' : 'o') + (piece.color === 'white' ? letter.toUpperCase() : letter);
      } else {
        board += " ";
      }
      board += "</td>";
    }
    board += "<th>" + rank + "</th>";
    board += "</tr>";
  }
  board += "</tbody>";
  return board;
  */
}

export function renderFile(f: string, style: Style): string {
  return style === 'nato' ? nato[f] : (style === 'anna' ? anna[f] : f);
}

export function renderKey(key: string, style: Style): string {
  return `${renderFile(key[0], style)} ${key[1]}`;
}

export function castlingFlavours(input: string): string {
  switch(input.toLowerCase().replace(/[-\s]+/g, '')) {
    case 'oo': case '00': return 'o-o';
    case 'ooo': case '000': return 'o-o-o';
  }
  return input;
}
