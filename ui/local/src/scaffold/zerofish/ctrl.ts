import { BvbOpts, CgHost } from '../interfaces';
import { PromotionCtrl } from 'chess/promotion';
import { makeFen /*, parseFen*/ } from 'chessops/fen';
import { Chess, Role } from 'chessops';
import * as Chops from 'chessops';
//import makeZerofish, { Zerofish, PV } from 'zerofish';
import { Api as CgApi } from 'chessground/api';
import { Config as CgConfig } from 'chessground/config';
import { Key } from 'chessground/types';
import { Engines } from 'ceval';

type Player = 'human' | 'zero' | 'fish';

interface Zerofish {
  goZero(fen: string): Promise<string>;
  goFish(fen: string, opts: { pvs?: number; depth?: number }): Promise<PV[]>;
  reset(): void;
  setNet(name: string, weights: Uint8Array): void;
}

interface PV {
  moves: string[];
  score: number;
}

function makeZerofish() {
  return new Promise<Zerofish>((resolve, reject) => {
    resolve({} as Zerofish);
  });
}

export class ZerofishCtrl implements CgHost {
  cg: CgApi;
  path = '';
  chess = Chess.default();
  promotion: PromotionCtrl;
  zf: { white?: Zerofish; black?: Zerofish };
  totals: { gamesLeft: number; white: number; black: number; draw: number };
  players: { white: Player; black: Player } = { white: 'human', black: 'fish' };

  fen = '';
  flipped = false;
  fiftyMovePly = 0;
  threefoldFens: Map<string, number> = new Map();

  engines = new Engines();
  constructor(
    readonly opts: BvbOpts,
    readonly redraw: () => void,
  ) {
    this.promotion = new PromotionCtrl(
      f => f(this.cg),
      () => this.cg.set(this.cgOpts()),
      this.redraw,
    );
    Promise.all([makeZerofish(), makeZerofish()]).then(([wz, bz]) => {
      this.zf ??= { white: wz, black: bz };
    });
  }

  go(numGames?: number) {
    this.totals ??= { gamesLeft: 1, white: 0, black: 0, draw: 0 };
    if (numGames) this.totals.gamesLeft = numGames;
    this.fiftyMovePly = 0;
    this.threefoldFens.clear();
    this.chess.reset();
    this.fen = makeFen(this.chess.toSetup());
    this.cg.set({ fen: this.fen });
    this.zf.white?.reset();
    this.zf.black?.reset();
    this.getBotMove();
    $('#go').addClass('disabled');
  }

  checkGameOver(userEnd?: 'whiteResign' | 'blackResign' | 'mutualDraw'): {
    end: boolean;
    result?: string;
    reason?: string;
  } {
    let result = 'draw',
      reason = userEnd ?? 'checkmate';
    if (this.chess.isCheckmate()) result = Chops.opposite(this.chess.turn);
    else if (this.chess.isInsufficientMaterial()) reason = 'insufficient';
    else if (this.chess.isStalemate()) reason = 'stalemate';
    else if (this.fifty()) reason = 'fifty';
    else if (this.threefold()) reason = 'threefold';
    else if (userEnd) {
      if (userEnd !== 'mutualDraw') reason = 'resign';
      if (userEnd === 'whiteResign') result = 'black';
      else if (userEnd === 'blackResign') result = 'white';
    } else return { end: false };
    return { end: true, result, reason };
  }

  doGameOver(result: string, reason: string) {
    console.log(`game over ${result} ${reason}`);

    // blah blah do outcome stuff
    if (result === 'white') this.totals.white++;
    else if (result === 'black') this.totals.black++;
    else this.totals.draw++;
    $('#white-totals').text(`${this.totals.white} / ${this.totals.draw} / ${this.totals.black}`);
    $('#black-totals').text(`${this.totals.black} / ${this.totals.draw} / ${this.totals.white}`);
    if (--this.totals.gamesLeft < 1) $('#go').removeClass('disabled');
    else setTimeout(() => this.go());
  }

  jump(path: string) {
    path;
    /*this.path = path;
    this.chess = Chess.fromSetup(Chops.parseFen(path));
    this.fen = makeFen(this.chess.toSetup());
    this.cg.set(this.cgOpts());
    this.fiftyMovePly = 0;
    this.threefoldFens.clear();
    this.zf.white?.reset();
    this.zf.black?.reset();*/
  }

  move(uci: Uci, user = false) {
    const move = Chops.parseUci(uci);
    if (!move || !this.chess.isLegal(move))
      throw new Error(`illegal move ${uci}, ${makeFen(this.chess.toSetup())}}`);

    this.chess.play(move);
    this.fen = makeFen(this.chess.toSetup());
    this.fifty(move);
    this.threefold('update');
    if (user && this.isPromotion(move)) {
      return; // oh noes PromotionCtrl! put it back! put it back!
    } else this.updateCgBoard(uci);
    const { end, result, reason } = this.checkGameOver();
    if (end) this.doGameOver(result!, reason!);
    else this.getBotMove();
  }

  cgUserMove = (orig: Key, dest: Key) => {
    this.move(orig + dest, true);
  };

  async getBotMove() {
    const moveType = this.players[this.chess.turn];
    if (moveType === 'human') return;
    const zf = this.zf[this.chess.turn];
    let move;
    if (moveType === 'zero') {
      const [zeroMove, lines] = await Promise.all([
        zf!.goZero(this.fen),
        zf!.goFish(this.fen, { pvs: 8, depth: 6 }),
      ]);
      // without randomSprinkle, lc0 will always play the same game
      move = Math.random() < 0.5 ? randomSprinkle(zeroMove, lines) : zeroMove;
      console.log(`${this.chess.turn} ${zeroMove === move ? 'zero' : 'ZEROFISH'} ${move}`);
    } else {
      move = (await zf!.goFish(this.fen, { depth: 3 }))[0].moves[0];
      console.log(`${this.chess.turn} fish ${move}`);
    }
    this.move(move);
  }

  updateCgBoard(uci: Uci) {
    const { from, to, role } = splitUci(uci);
    this.cg.move(from, to);
    if (role) this.cg.setPieces(new Map([[to, { color: this.chess.turn, role, promoted: true }]]));
    this.cg.set(this.cgOpts(true));
  }

  cgOpts(withFen = true): CgConfig {
    return {
      fen: withFen ? this.fen : undefined,
      orientation: this.flipped ? 'black' : 'white',
      turnColor: this.chess.turn,
      check: this.chess.isCheck() ? this.chess.turn : false,
      movable: {
        color: this.chess.turn,
        dests:
          this.players[this.chess.turn] !== 'human'
            ? new Map()
            : new Map([...this.chess.allDests()].map(([s, ds]) => [sq2key(s), [...ds].map(sq2key)])),
      },
    };
  }

  fifty(move?: Chops.Move) {
    if (move)
      if (
        !('from' in move) ||
        this.chess.board.getRole(move.from) === 'pawn' ||
        this.chess.board.get(move.to)
      )
        this.fiftyMovePly = 0;
      else this.fiftyMovePly++;
    return this.fiftyMovePly >= 100;
  }

  threefold(update: 'update' | false = false) {
    const boardFen = this.fen.split('-')[0];
    const fenCount = (this.threefoldFens.get(boardFen) ?? 0) + 1;
    if (update) this.threefoldFens.set(boardFen, fenCount);
    return fenCount >= 3;
  }

  isPromotion(move: Chops.Move) {
    return (
      'from' in move &&
      Chops.squareRank(move.to) === (this.chess.turn === 'white' ? 7 : 0) &&
      this.chess.board.getRole(move.from) === 'pawn'
    );
  }

  flip = () => {
    this.flipped = !this.flipped;
    this.cg.toggleOrientation();
    this.redraw();
  };

  dropHandler(color: 'white' | 'black', el: HTMLElement) {
    const $el = $(el);
    $el.on('dragenter dragover dragleave drop', e => {
      e.preventDefault();
      e.stopPropagation();
    });
    $el.on('dragenter dragover', () => this.zf[color] && $el.addClass('hilite'));
    $el.on('dragleave drop', () => this.zf[color] && $el.removeClass('hilite'));
    $el.on('drop', e => {
      if (!this.zf[color]) return;
      const reader = new FileReader();
      const weights = e.dataTransfer.files.item(0) as File;
      reader.onload = e => {
        this.zf[color]!.setNet(weights.name, new Uint8Array(e.target!.result as ArrayBuffer));
        $(`#${color} p`).first().text(weights.name);
        this.players[color] = 'zero';
        if (this.players[Chops.opposite(color)] !== 'human') $('#go').removeClass('disabled');
      };
      reader.readAsArrayBuffer(weights);
    });
  }
}

function sq2key(sq: number): Key {
  return Chops.makeSquare(sq);
}

function splitUci(uci: Uci): { from: Key; to: Key; role?: Role } {
  return { from: uci.slice(0, 2) as Key, to: uci.slice(2, 4) as Key, role: Chops.charToRole(uci.slice(4)) };
}

function linesWithin(move: string, lines: PV[], bias = 0, threshold = 50) {
  const zeroScore = lines.find(line => line.moves[0] === move)?.score ?? Number.NaN;
  return lines.filter(fish => Math.abs(fish.score - bias - zeroScore) < threshold && fish.moves.length);
}

function randomSprinkle(move: string, lines: PV[]) {
  lines = linesWithin(move, lines, 0, 20);
  if (!lines.length) return move;
  return lines[Math.floor(Math.random() * lines.length)].moves[0] ?? move;
}

/*
function occurs(chance: number) {
  return Math.random() < chance;
}*/
