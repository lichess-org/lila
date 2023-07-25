import { LocalPlayOpts } from './interfaces';
import { PromotionCtrl } from 'chess/promotion';
import { makeFen /*, parseFen*/ } from 'chessops/fen';
import { Chess, makeSquare, parseSquare, opposite, charToRole /*, Role*/ } from 'chessops';
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
  fifty = 0;
  threefold: Map<string, number> = new Map();
  botFight?: { gamesLeft: number; white: number; black: number; draw: number };
  zf: { white: Zerofish; black: Zerofish };
  players: { white: Player; black: Player } = { white: 'human', black: 'fish' };

  constructor(readonly opts: LocalPlayOpts, readonly redraw: () => void) {
    this.promotion = new PromotionCtrl(f => f(this.cg), this.setGround, this.redraw);
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
      reader.onload = e => this.setZero(color, weights, new Uint8Array(e.target!.result as ArrayBuffer));
      reader.readAsArrayBuffer(weights);
    });
  }

  setZero(color: 'white' | 'black', f: File, data: Uint8Array) {
    this.zf[color].setZeroWeights(data);
    $(`#${color} p`).first().text(f.name);
    this.players[color] = 'zero';
    if (this.players[opposite(color)] !== 'human') $('#go').removeClass('disabled');
  }

  go(numGames?: number) {
    if (numGames) {
      this.botFight = { gamesLeft: numGames, white: 0, black: 0, draw: 0 };
      $('#go').addClass('disabled');
    }
    this.chess.reset();
    this.cg.set({ fen: makeFen(this.chess.toSetup()) });
    console.log(makeFen(this.chess.toSetup()));
    this.zf.white.reset();
    this.zf.black.reset();
    this.doBots();
  }

  getCgOpts = (): CgConfig => {
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

  apiMove = (uci: Uci) => {
    const promotion = charToRole(uci.slice(4));
    const move = { from: parseSquare(uci.slice(0, 2))!, to: parseSquare(uci.slice(2, 4))!, promotion };
    if (!this.chess.isLegal(move)) throw new Error(`illegal move ${uci}, ${makeFen(this.chess.toSetup())}}`);
    this.cg.move(uci.slice(0, 2) as Key, uci.slice(2, 4) as Key);
    if (promotion) {
      this.cg.setPieces(
        new Map([
          [
            uci.slice(2, 4) as Key,
            {
              color: this.chess.turn,
              role: promotion,
              promoted: true,
            },
          ],
        ])
      );
    }
    this.chess.play(move);
    this.cg.set({
      turnColor: this.chess.turn,
      movable: {
        dests: new Map(
          [...this.chess.allDests()].map(
            ([s, ds]) => [makeSquare(s), [...ds].map(d => makeSquare(d))] as [Key, Key[]]
          )
        ),
      },
      check: this.chess.isCheck(),
    });
    this.redraw();
    this.doBots();
  };

  userMove = (orig: Key, dest: Key) => {
    this.chess.play({ from: parseSquare(orig)!, to: parseSquare(dest)! });
    if (this.chess.isEnd()) {
      console.log('game over');
      return;
    }
    this.doBots();
  };

  async doBots() {
    const moveType = this.players[this.chess.turn];
    if (moveType === 'human') return;
    if (this.botFight && this.chess.isEnd()) {
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
      return;
    }
    const zf = this.zf[this.chess.turn],
      fen = makeFen(this.chess.toSetup());
    let move;
    if (moveType === 'zero') {
      const [zeroMove, lines] = await Promise.all([zf.goZero(fen), zf.goFish(fen, { pvs: 8, depth: 8 })]);
      move = this.chess.turn === 'black' ? testAdjust(zeroMove, lines) : zeroMove;
      console.log(`${this.chess.turn} ${zeroMove === move ? 'zero' : 'ZEROFISH'} ${move}`);
    } else {
      move = (await zf.goFish(fen, { depth: 1 }))[0].moves[0];
      console.log(`${this.chess.turn} fish ${move}`);
    }
    this.apiMove(move);
  }

  flip = () => {
    this.flipped = !this.flipped;
    this.cg.toggleOrientation();
    this.redraw();
  };

  private setGround = () => this.cg.set(this.getCgOpts());
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
