import { LocalPlayOpts } from './interfaces';
import { PromotionCtrl } from 'chess/promotion';
import { makeFen /*, parseFen*/ } from 'chessops/fen';
import { Chess, makeSquare, parseSquare, opposite, charToRole, squareRank, Role } from 'chessops';
import makeZerofish, { Zerofish, PV } from 'zerofish';
import { Api as CgApi } from 'chessground/api';
import { Config as CgConfig } from 'chessground/config';
import { Key } from 'chessground/types';

type Player = 'human' | 'zero' | 'fish';

export class Ctrl {
  promotion: PromotionCtrl;
  cg: CgApi;
  flipped = false;
  chess = Chess.default();
  fiftyMovePly = 0;
  fen = '';
  threefold: Map<string, number> = new Map();
  botFight?: { gamesLeft: number; white: number; black: number; draw: number };
  zf: { white: Zerofish; black: Zerofish };
  players: { white: Player; black: Player } = { white: 'human', black: 'fish' };

  constructor(readonly opts: LocalPlayOpts, readonly redraw: () => void) {
    this.promotion = new PromotionCtrl(
      f => f(this.cg),
      () => this.cg.set(this.makeCgOpts()),
      this.redraw
    );
    Promise.all([makeZerofish(), makeZerofish()]).then(([wz, bz]) => {
      this.zf ??= { white: wz, black: bz };
    });
  }

  dropHandler(color: 'white' | 'black', el: HTMLElement) {
    const $el = $(el);
    $el.on('dragenter dragover dragleave drop', e => {
      e.preventDefault();
      e.stopPropagation();
    });
    $el.on('dragenter dragover', _ => $el.addClass('hilite'));
    $el.on('dragleave drop', _ => $el.removeClass('hilite'));
    $el.on('drop', e => {
      const reader = new FileReader();
      const weights = e.dataTransfer.files.item(0) as File;
      reader.onload = e => {
        this.zf[color].setZeroWeights(new Uint8Array(e.target!.result as ArrayBuffer));
        $(`#${color} p`).first().text(weights.name);
        this.players[color] = 'zero';
        if (this.players[opposite(color)] !== 'human') $('#go').removeClass('disabled');
      };
      reader.readAsArrayBuffer(weights);
    });
  }

  go(numGames?: number) {
    if (numGames) {
      this.botFight = { gamesLeft: numGames, white: 0, black: 0, draw: 0 };
      $('#go').addClass('disabled');
    }
    this.fiftyMovePly = 0;
    this.threefold.clear();
    this.chess.reset();
    this.fen = makeFen(this.chess.toSetup());
    this.cg.set({ fen: this.fen });
    console.log(makeFen(this.chess.toSetup()));
    this.zf.white.reset();
    this.zf.black.reset();
    this.getBotMove();
  }

  gameOver(reason: 'threefold' | 'fifty' | 'white' | 'black' | 'draw') {
    console.log(`game over ${reason}`);
    // blah blah do outcome stuff
    if (!this.botFight) return;
    const bf = this.botFight;
    const result = this.chess.outcome();
    if (!result || !result.winner) bf.draw++;
    else if (result.winner === 'white') bf.white++;
    else bf.black++;
    $('#white-totals').text(`${bf.white} / ${bf.draw} / ${bf.black}`);
    $('#black-totals').text(`${bf.black} / ${bf.draw} / ${bf.white}`);
    if (--bf.gamesLeft < 1) {
      this.botFight = undefined;
      $('#go').removeClass('disabled');
      return;
    }
    setTimeout(() => this.go());
  }

  move(uci: Uci, user = false) {
    const promotion = charToRole(uci.slice(4)),
      from = parseSquare(uci.slice(0, 2))!,
      to = parseSquare(uci.slice(2, 4))!,
      piece = this.chess.board.getRole(from),
      move = { from, to, promotion };
    let finished = false;

    if (!this.chess.isLegal(move)) throw new Error(`illegal move ${uci}, ${makeFen(this.chess.toSetup())}}`);

    if (piece === 'pawn' || this.chess.board.get(to)) this.fiftyMovePly = 0;
    else this.fiftyMovePly++;

    this.chess.play(move);
    this.fen = makeFen(this.chess.toSetup());
    const fenCount = (this.threefold.get(this.fen) ?? 0) + 1;
    this.threefold.set(this.fen, fenCount);

    if (this.chess.isEnd() || fenCount >= 3 || this.fiftyMovePly >= 100) {
      this.gameOver(
        this.chess.outcome()?.winner ??
          (this.fiftyMovePly >= 100 ? 'fifty' : fenCount >= 3 ? 'threefold' : 'draw')
      );
      finished = true;
    }
    if (user && (squareRank(to) === 0 || squareRank(to) === 7) && piece === 'pawn') {
      return;
      // oh noes PromotionCtrl! put it back!
    } else this.cgMove(uci);
    if (!finished) this.getBotMove();
  }

  userMove = (orig: Key, dest: Key) => {
    this.move(orig + dest, true);
  };

  async getBotMove() {
    const moveType = this.players[this.chess.turn];
    if (moveType === 'human') return;
    const zf = this.zf[this.chess.turn];
    let move;
    if (moveType === 'zero') {
      console.log(this.chess.turn, this.fen);
      const [zeroMove, lines] = await Promise.all([
        zf.goZero(this.fen),
        zf.goFish(this.fen, { pvs: 8, depth: 6 }),
      ]);
      move = this.chess.turn === 'black' ? testAdjust(zeroMove, lines) : zeroMove;
      console.log(`${this.chess.turn} ${zeroMove === move ? 'zero' : 'ZEROFISH'} ${move}`);
    } else {
      move = (await zf.goFish(this.fen, { depth: 4 }))[0].moves[0];
      console.log(`${this.chess.turn} fish ${move}`);
    }
    this.move(move);
  }

  cgMove(uci: Uci) {
    const from = uci.slice(0, 2) as Key,
      to = uci.slice(2, 4) as Key,
      role = charToRole(uci.slice(4)) as Role,
      turn = this.chess.turn;
    this.cg.move(from, to);
    if (role) this.cg.setPieces(new Map([[to, { color: this.chess.turn, role, promoted: true }]]));
    const dests =
      this.players[turn] !== 'human'
        ? new Map()
        : new Map(
            [...this.chess.allDests()].map(
              ([s, ds]) => [makeSquare(s), [...ds].map(d => makeSquare(d))] as [Key, Key[]]
            )
          );
    this.cg.set({
      turnColor: turn,
      movable: { dests },
      check: this.chess.isCheck() ? turn : false,
    });
  }

  makeCgOpts = (): CgConfig => {
    const cgDests = new Map(
      [...this.chess.allDests()].map(
        ([s, ds]) => [makeSquare(s), [...ds].map(d => makeSquare(d))] as [Key, Key[]]
      )
    );
    return {
      fen: makeFen(this.chess.toSetup()),
      orientation: this.flipped ? 'black' : 'white',
      turnColor: this.chess.turn,

      movable: {
        color: this.chess.turn,
        dests: cgDests,
      },
    };
  };

  flip = () => {
    this.flipped = !this.flipped;
    this.cg.toggleOrientation();
    this.redraw();
  };
}

function linesWithin(move: string, lines: PV[], bias = 0, threshold = 50) {
  const zeroScore = lines.find(line => line.moves[0] === move)?.score ?? Number.NaN;
  return lines.filter(fish => Math.abs(fish.score - bias - zeroScore) < threshold && fish.moves.length);
}

function testAdjust(move: string, lines: PV[]) {
  //if (!occurs(0.5)) return move;
  lines = linesWithin(move, lines, 40, 40);
  if (!lines.length) return move;
  return lines[Math.floor(Math.random() * lines.length)].moves[0] ?? move;
}

/*function occurs(chance: number) {
  return Math.random() < chance;
}*/
