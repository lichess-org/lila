import * as co from 'chessops';
import { hashBoard, hashChess } from 'lib/game/hash';
import { deepFreeze } from 'lib/algo';
import { normalizeMove } from 'chessops/chess';
import type {
  OpeningMove,
  OpeningBook,
  PgnProgress,
  PgnFilter,
  PolyglotResult,
  PolyglotOpts,
} from 'lib/game/polyglot';
import type { Board } from 'chessops';

export async function initModule(o: PolyglotOpts): Promise<PolyglotResult | ((board: Board) => bigint)> {
  if (!o) return hashBoard;
  const book =
    'bytes' in o
      ? makeBookPolyglot(o.bytes)
      : 'pgn' in o
        ? await makeBookPgn(o.pgn, o.ply, o.progress)
        : { getMoves: o.getMoves };
  return {
    ...book,
    cover: o.cover
      ? await makeCover(book.getMoves, typeof o.cover === 'object' ? o.cover.boardSize : 192, book.positions)
      : undefined,
  };
}

async function makeBookPgn(
  blob: Blob,
  maxPly: number,
  progress?: PgnProgress,
  filter?: PgnFilter,
): Promise<PolyglotResult> {
  let bigMap: Map<bigint, Map<string, number>> | undefined = new Map();
  for await (const game of pgnFromBlob(blob, 8 * 1024 * 1024, progress)) {
    if (!game) return { getMoves: () => Promise.resolve([]), positions: 0 };
    if (filter && !filter(game)) continue;
    traverseTree(co.pgn.startingPosition(game.headers).unwrap(), game.moves, maxPly);
  }
  const posMap = new Map<bigint, OpeningMove[]>();
  for (const [hash, map] of bigMap) {
    const moves = Array.from(map, ([uci, weight]) => ({ uci, weight }));
    const sum = moves.reduce((sum, m) => sum + m.weight, 0);
    for (const m of moves) m.weight = Math.round((m.weight / sum) * 65535);
    posMap.set(hash, moves);
  }
  bigMap = undefined; // phew
  deepFreeze(posMap);
  const polyglot = new Blob(
    Array.from(posMap, ([hash, moves]) => {
      const buffer = new ArrayBuffer(16 * moves.length);
      const view = new DataView(buffer);
      let pos = 0;
      for (const { uci, weight } of moves) {
        view.setBigUint64(pos, hash);
        view.setUint16(pos + 8, uciToShort(uci));
        view.setUint16(pos + 10, weight);
        pos += 16;
      }
      return buffer;
    }),
  );
  return { getMoves: getMoves(posMap), polyglot, positions: posMap.size };

  function traverseTree(chess: co.Chess, node: co.pgn.Node<co.pgn.PgnNodeData>, plyToGo: number) {
    if (plyToGo === 0) return;
    const zobrist = hashChess(chess);
    const moves = bigMap!.get(zobrist) ?? new Map<string, number>();
    if (plyToGo > 1)
      for (const nextNode of node.children) {
        const move = co.san.parseSan(chess, nextNode.data.san);
        if (!move) continue;
        const nextChess = chess.clone();
        const uci = co.makeUci(move);
        moves.set(uci, (moves.get(uci) ?? 0) + 1);
        nextChess.play(move);
        traverseTree(nextChess, nextNode, plyToGo - 1);
      }
    bigMap!.set(zobrist, moves);
  }

  // commenting this out because we're just going to count moves for now
  // function getWeightThatIsAlmostCertainlyWrong(node: co.pgn.ChildNode<co.pgn.PgnNodeData>) {
  //   const weightstr = node.data.comments?.find(x => /W[^:]*:\s*([0-9.]+)/.exec(x))?.[1];
  //   const weight = weightstr ? parseFloat(weightstr) : 1;
  //   // in case weights are from 0 to 1. crush the top because you cannot have big weights AND nice things
  //   return weight < 100 ? weight * 100 : Math.min(65535, 9900 + weight);
  // }
}

function makeBookPolyglot(bytes: DataView): PolyglotResult {
  const book = new Map<bigint, OpeningMove[]>();

  for (let i = 0; i < bytes.byteLength - 15; i += 16) {
    const hash = bytes.getBigUint64(i);
    const move = bytes.getUint16(i + 8);
    const weight = bytes.getUint16(i + 10);

    const moves = book.get(hash) ?? [];
    moves.push({ uci: shortToUci(move), weight });
    book.set(hash, moves);
  }
  deepFreeze(book);
  return { getMoves: getMoves(book), positions: book.size };
}

async function* pgnFromBlob(blob: Blob, chunkSize: number, progress?: PgnProgress) {
  const totalSize = blob.size;
  let offset = 0;
  let residual = '';
  while (offset < totalSize) {
    if (progress?.(offset, totalSize) === false) {
      yield undefined;
      return;
    }
    const chunk = blob.slice(offset, offset + chunkSize);
    const textChunk = await chunk.text();
    const crlfLast = textChunk.lastIndexOf('\r\n\r\n[');
    const lfLast = textChunk.lastIndexOf('\n\n[');
    const wholePgnsChunk =
      offset + chunk.size === totalSize || Math.max(crlfLast, lfLast) === -1
        ? textChunk
        : textChunk.slice(0, Math.max(lfLast + 2, crlfLast + 4));
    const games = co.pgn.parsePgn(residual + wholePgnsChunk).filter(game => {
      const tag = game.headers.get('Variant');
      return !tag || tag.toLowerCase() === 'standard';
    });
    for (const game of games) yield game;
    residual = textChunk.slice(wholePgnsChunk.length);
    offset += chunkSize;
  }
}

type Composition = { boards: number; squares: Map<number, Map<Color, Map<co.Role, number>>> };

async function makeCover(polyglotBook: OpeningBook, boardSize: number, numMoves?: number): Promise<Blob> {
  const squareSize = boardSize / 8;

  if (typeof OffscreenCanvas !== 'function') throw 'no OffscreenCanvas support';

  const canvas = new OffscreenCanvas(boardSize, boardSize);

  const ctx = canvas.getContext('2d')!;
  const composition: Composition = { boards: 1, squares: new Map() };

  for (let row = 0; row < 8; row++) {
    for (let col = 0; col < 8; col++) {
      const isLightSquare = (row + col) % 2 === 0;
      ctx.fillStyle = isLightSquare ? '#f0d9b5' : '#b58863';
      ctx.fillRect(col * squareSize, row * squareSize, squareSize, squareSize);
    }
  }

  await traverseTree(co.Chess.default(), 4);

  for (const [sq, comp] of composition.squares) {
    const [x, y] = [co.squareFile(sq) * squareSize, (7 - co.squareRank(sq)) * squareSize];
    for (const color of comp.keys()) {
      for (const role of comp.get(color)?.keys() ?? []) {
        ctx.globalAlpha = 0.4 + (0.6 * (comp.get(color)?.get(role) ?? 0)) / composition.boards;
        ctx.drawImage(pieces[color][role], x, y, squareSize, squareSize);
      }
    }
  }
  if (numMoves) {
    const moveText = numMoves.toLocaleString('en-US');
    ctx.font = `bold ${Math.floor(squareSize)}px Noto Sans`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.strokeStyle = 'black';
    ctx.globalAlpha = 0.8;
    ctx.lineWidth = 6;
    ctx.strokeText(moveText, boardSize / 2, (boardSize * 7) / 8);
    ctx.fillStyle = 'white';
    ctx.fillText(moveText, boardSize / 2, (boardSize * 7) / 8);
  }
  return await canvas.convertToBlob({ type: 'image/png' });

  async function traverseTree(chess: co.Chess, plyToGo: number) {
    if (plyToGo === 0) return;
    for (const om of await polyglotBook(chess)) {
      const { move } = normalMove(chess, om.uci) ?? {};
      if (!move) continue;
      const nextChess = chess.clone();
      nextChess.play(move);
      await traverseTree(nextChess, plyToGo - 1);
      composeBoard(nextChess);
    }
  }

  function composeBoard(chess: co.Chess) {
    [...chess.board.occupied].forEach(sq => {
      const { color, role } = chess.board.get(sq) ?? {};
      if (!color || !role) return;
      if (!composition.squares.has(sq)) composition.squares.set(sq, new Map());
      const sqcomp = composition.squares.get(sq)!;
      if (!sqcomp.has(color)) sqcomp.set(color, new Map());
      const colorMap = sqcomp.get(color)!;
      colorMap.set(role, (colorMap.get(role) ?? 0) + 1);
    });
    composition.boards++;
  }
}

function makeImage(svg: string) {
  const src =
    'data:image/svg+xml;base64,' +
    btoa(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 45 45">${svg}</svg>`);
  const img = new Image();
  img.src = src;
  return img;
}

function getMoves(book: Map<bigint, OpeningMove[]>): OpeningBook {
  return (pos: co.Chess): Promise<OpeningMove[]> => {
    const moves = book.get(hashChess(pos)) ?? [];
    const sum = moves.reduce((sum: number, m) => sum + m.weight, 0);
    return Promise.resolve(moves.map(m => ({ uci: m.uci, weight: m.weight / sum })));
  };
}

function shortToUci(move: number) {
  return (
    co.FILE_NAMES[(move >>> 6) & 0b111] +
    co.RANK_NAMES[(move >>> 9) & 0b111] +
    co.FILE_NAMES[move & 0b111] +
    co.RANK_NAMES[(move >>> 3) & 0b111] +
    promotes[(move >>> 12) & 0b111]
  );
}

function charDiff(a: string, b: string) {
  return a.charCodeAt(0) - b.charCodeAt(0);
}

function uciToShort(uci: Uci): number {
  const from = (charDiff(uci[1], '1') << 3) | charDiff(uci[0], 'a');
  const to = (charDiff(uci[3], '1') << 3) | charDiff(uci[2], 'a');
  const promotion = uci.length === 5 ? promotes.indexOf(uci[4]) : 0;
  return (promotion << 12) | (from << 6) | to;
}

function normalMove(chess: co.Chess, unsafeUci: Uci): { uci: Uci; move: co.NormalMove } | undefined {
  const unsafe = co.parseUci(unsafeUci);
  const move = unsafe && 'from' in unsafe ? { ...unsafe, ...normalizeMove(chess, unsafe) } : undefined;
  return move && chess.isLegal(move) ? { uci: co.makeUci(move), move } : undefined;
}

const promotes = ['', 'n', 'b', 'r', 'q', '?', '?', '?'];

const pieces: Record<Color, Record<co.Role, HTMLImageElement>> = {
  black: {
    bishop: makeImage(
      '<g fill="none" fill-rule="evenodd" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><g fill="#000" stroke-linecap="butt"><path d="M9 36c3.4-1 10.1.4 13.5-2 3.4 2.4 10.1 1 13.5 2 0 0 1.6.5 3 2-.7 1-1.6 1-3 .5-3.4-1-10.1.5-13.5-1-3.4 1.5-10.1 0-13.5 1-1.4.5-2.3.5-3-.5 1.4-2 3-2 3-2z"/><path d="M15 32c2.5 2.5 12.5 2.5 15 0 .5-1.5 0-2 0-2 0-2.5-2.5-4-2.5-4 5.5-1.5 6-11.5-5-15.5-11 4-10.5 14-5 15.5 0 0-2.5 1.5-2.5 4 0 0-.5.5 0 2z"/><path d="M25 8a2.5 2.5 0 1 1-5 0 2.5 2.5 0 1 1 5 0z"/></g><path stroke="#ececec" stroke-linejoin="miter" d="M17.5 26h10M15 30h15m-7.5-14.5v5M20 18h5"/></g>',
    ),
    king: makeImage(
      '<g fill="none" fill-rule="evenodd" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path stroke-linejoin="miter" d="M22.5 11.6V6"/><path fill="#000" stroke-linecap="butt" stroke-linejoin="miter" d="M22.5 25s4.5-7.5 3-10.5c0 0-1-2.5-3-2.5s-3 2.5-3 2.5c-1.5 3 3 10.5 3 10.5"/><path fill="#000" d="M11.5 37a22.3 22.3 0 0 0 21 0v-7s9-4.5 6-10.5c-4-6.5-13.5-3.5-16 4V27v-3.5c-3.5-7.5-13-10.5-16-4-3 6 5 10 5 10V37z"/><path stroke-linejoin="miter" d="M20 8h5"/><path stroke="#ececec" d="M32 29.5s8.5-4 6-9.7C34.1 14 25 18 22.5 24.6v2.1-2.1C20 18 9.9 14 7 19.9c-2.5 5.6 4.8 9 4.8 9"/><path stroke="#ececec" d="M11.5 30c5.5-3 15.5-3 21 0m-21 3.5c5.5-3 15.5-3 21 0m-21 3.5c5.5-3 15.5-3 21 0"/></g>',
    ),
    knight: makeImage(
      '<g fill="none" fill-rule="evenodd" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path fill="#000" d="M22 10c10.5 1 16.5 8 16 29H15c0-9 10-6.5 8-21"/><path fill="#000" d="M24 18c.38 2.91-5.55 7.37-8 9-3 2-2.82 4.34-5 4-1.04-.94 1.41-3.04 0-3-1 0 .19 1.23-1 2-1 0-4 1-4-4 0-2 6-12 6-12s1.89-1.9 2-3.5c-.73-1-.5-2-.5-3 1-1 3 2.5 3 2.5h2s.78-2 2.5-3c1 0 1 3 1 3"/><path fill="#ececec" stroke="#ececec" d="M9.5 25.5a.5.5 0 1 1-1 0 .5.5 0 1 1 1 0zm5.43-9.75a.5 1.5 30 1 1-.86-.5.5 1.5 30 1 1 .86.5z"/><path fill="#ececec" stroke="none" d="m24.55 10.4-.45 1.45.5.15c3.15 1 5.65 2.49 7.9 6.75S35.75 29.06 35.25 39l-.05.5h2.25l.05-.5c.5-10.06-.88-16.85-3.25-21.34-2.37-4.49-5.79-6.64-9.19-7.16l-.51-.1z"/></g>',
    ),
    pawn: makeImage(
      '<path stroke="#000" stroke-linecap="round" stroke-width="1.5" d="M22.5 9a4 4 0 0 0-3.22 6.38 6.48 6.48 0 0 0-.87 10.65c-3 1.06-7.41 5.55-7.41 13.47h23c0-7.92-4.41-12.41-7.41-13.47a6.46 6.46 0 0 0-.87-10.65A4.01 4.01 0 0 0 22.5 9z"/>',
    ),
    queen: makeImage(
      '<g fill-rule="evenodd" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><g stroke="none"><circle cx="6" cy="12" r="2.75"/><circle cx="14" cy="9" r="2.75"/><circle cx="22.5" cy="8" r="2.75"/><circle cx="31" cy="9" r="2.75"/><circle cx="39" cy="12" r="2.75"/></g><path stroke-linecap="butt" d="M9 26c8.5-1.5 21-1.5 27 0l2.5-12.5L31 25l-.3-14.1-5.2 13.6-3-14.5-3 14.5-5.2-13.6L14 25 6.5 13.5 9 26z"/><path stroke-linecap="butt" d="M9 26c0 2 1.5 2 2.5 4 1 1.5 1 1 .5 3.5-1.5 1-1.5 2.5-1.5 2.5-1.5 1.5.5 2.5.5 2.5 6.5 1 16.5 1 23 0 0 0 1.5-1 0-2.5 0 0 .5-1.5-1-2.5-.5-2.5-.5-2 .5-3.5 1-2 2.5-2 2.5-4-8.5-1.5-18.5-1.5-27 0z"/><path fill="none" stroke-linecap="butt" d="M11 38.5a35 35 1 0 0 23 0"/><path fill="none" stroke="#ececec" d="M11 29a35 35 1 0 1 23 0m-21.5 2.5h20m-21 3a35 35 1 0 0 22 0m-23 3a35 35 1 0 0 24 0"/></g>',
    ),
    rook: makeImage(
      '<g fill-rule="evenodd" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path stroke-linecap="butt" d="M9 39h27v-3H9v3zm3.5-7 1.5-2.5h17l1.5 2.5h-20zm-.5 4v-4h21v4H12z"/><path stroke-linecap="butt" stroke-linejoin="miter" d="M14 29.5v-13h17v13H14z"/><path stroke-linecap="butt" d="M14 16.5 11 14h23l-3 2.5H14zM11 14V9h4v2h5V9h5v2h5V9h4v5H11z"/><path fill="none" stroke="#ececec" stroke-linejoin="miter" stroke-width="1" d="M12 35.5h21m-20-4h19m-18-2h17m-17-13h17M11 14h23"/></g>',
    ),
  },
  white: {
    bishop: makeImage(
      '<g fill="none" fill-rule="evenodd" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><g fill="#fff" stroke-linecap="butt"><path d="M9 36c3.39-.97 10.11.43 13.5-2 3.39 2.43 10.11 1.03 13.5 2 0 0 1.65.54 3 2-.68.97-1.65.99-3 .5-3.39-.97-10.11.46-13.5-1-3.39 1.46-10.11.03-13.5 1-1.35.49-2.32.47-3-.5 1.35-1.94 3-2 3-2z"/><path d="M15 32c2.5 2.5 12.5 2.5 15 0 .5-1.5 0-2 0-2 0-2.5-2.5-4-2.5-4 5.5-1.5 6-11.5-5-15.5-11 4-10.5 14-5 15.5 0 0-2.5 1.5-2.5 4 0 0-.5.5 0 2z"/><path d="M25 8a2.5 2.5 0 1 1-5 0 2.5 2.5 0 1 1 5 0z"/></g><path stroke-linejoin="miter" d="M17.5 26h10M15 30h15m-7.5-14.5v5M20 18h5"/></g>',
    ),
    king: makeImage(
      '<g fill="none" fill-rule="evenodd" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path stroke-linejoin="miter" d="M22.5 11.63V6M20 8h5"/><path fill="#fff" stroke-linecap="butt" stroke-linejoin="miter" d="M22.5 25s4.5-7.5 3-10.5c0 0-1-2.5-3-2.5s-3 2.5-3 2.5c-1.5 3 3 10.5 3 10.5"/><path fill="#fff" d="M11.5 37c5.5 3.5 15.5 3.5 21 0v-7s9-4.5 6-10.5c-4-6.5-13.5-3.5-16 4V27v-3.5c-3.5-7.5-13-10.5-16-4-3 6 5 10 5 10V37z"/><path d="M11.5 30c5.5-3 15.5-3 21 0m-21 3.5c5.5-3 15.5-3 21 0m-21 3.5c5.5-3 15.5-3 21 0"/></g>',
    ),
    knight: makeImage(
      '<g fill="none" fill-rule="evenodd" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path fill="#fff" d="M22 10c10.5 1 16.5 8 16 29H15c0-9 10-6.5 8-21"/><path fill="#fff" d="M24 18c.38 2.91-5.55 7.37-8 9-3 2-2.82 4.34-5 4-1.042-.94 1.41-3.04 0-3-1 0 .19 1.23-1 2-1 0-4.003 1-4-4 0-2 6-12 6-12s1.89-1.9 2-3.5c-.73-.994-.5-2-.5-3 1-1 3 2.5 3 2.5h2s.78-1.992 2.5-3c1 0 1 3 1 3"/><path fill="#000" d="M9.5 25.5a.5.5 0 1 1-1 0 .5.5 0 1 1 1 0zm5.433-9.75a.5 1.5 30 1 1-.866-.5.5 1.5 30 1 1 .866.5z"/></g>',
    ),
    pawn: makeImage(
      '<path fill="#fff" stroke="#000" stroke-linecap="round" stroke-width="1.5" d="M22.5 9c-2.21 0-4 1.79-4 4 0 .89.29 1.71.78 2.38C17.33 16.5 16 18.59 16 21c0 2.03.94 3.84 2.41 5.03-3 1.06-7.41 5.55-7.41 13.47h23c0-7.92-4.41-12.41-7.41-13.47 1.47-1.19 2.41-3 2.41-5.03 0-2.41-1.33-4.5-3.28-5.62.49-.67.78-1.49.78-2.38 0-2.21-1.79-4-4-4z"/>',
    ),
    queen: makeImage(
      '<g fill="#fff" fill-rule="evenodd" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path d="M8 12a2 2 0 1 1-4 0 2 2 0 1 1 4 0zm16.5-4.5a2 2 0 1 1-4 0 2 2 0 1 1 4 0zM41 12a2 2 0 1 1-4 0 2 2 0 1 1 4 0zM16 8.5a2 2 0 1 1-4 0 2 2 0 1 1 4 0zM33 9a2 2 0 1 1-4 0 2 2 0 1 1 4 0z"/><path stroke-linecap="butt" d="M9 26c8.5-1.5 21-1.5 27 0l2-12-7 11V11l-5.5 13.5-3-15-3 15-5.5-14V25L7 14l2 12z"/><path stroke-linecap="butt" d="M9 26c0 2 1.5 2 2.5 4 1 1.5 1 1 .5 3.5-1.5 1-1.5 2.5-1.5 2.5-1.5 1.5.5 2.5.5 2.5 6.5 1 16.5 1 23 0 0 0 1.5-1 0-2.5 0 0 .5-1.5-1-2.5-.5-2.5-.5-2 .5-3.5 1-2 2.5-2 2.5-4-8.5-1.5-18.5-1.5-27 0z"/><path fill="none" d="M11.5 30c3.5-1 18.5-1 22 0M12 33.5c6-1 15-1 21 0"/></g>',
    ),
    rook: makeImage(
      '<g fill="#fff" fill-rule="evenodd" stroke="#000" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path stroke-linecap="butt" d="M9 39h27v-3H9v3zm3-3v-4h21v4H12zm-1-22V9h4v2h5V9h5v2h5V9h4v5"/><path d="m34 14-3 3H14l-3-3"/><path stroke-linecap="butt" stroke-linejoin="miter" d="M31 17v12.5H14V17"/><path d="m31 29.5 1.5 2.5h-20l1.5-2.5"/><path fill="none" stroke-linejoin="miter" d="M11 14h23"/></g>',
    ),
  },
};
