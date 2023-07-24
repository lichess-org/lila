import { LocalPlayOpts } from './interfaces';
import { PromotionCtrl } from 'chess/promotion';
import { makeFen } from 'chessops/fen';
import { Chess, makeSquare, parseSquare, opposite } from 'chessops';
import makeZerofish, { Zerofish } from 'zerofish';
import { Api as CgApi } from 'chessground/api';
import { Config as CgConfig } from 'chessground/config';
import { Key } from 'chessground/types';

type Player = 'human' | 'zero' | 'fish';
export class Ctrl {
  promotion: PromotionCtrl;
  cg: CgApi;
  flipped = false;
  chess = Chess.default();
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
    $(`#${color}`).text(f.name);
    this.players[color] = 'zero';
    if (this.players[opposite(color)] !== 'human') $('#go').removeClass('disabled');
  }
  go() {
    //const numTimes = parseInt($('#num-games').val() as string) || 1;
    this.chess.reset();
    this.cg.set({ fen: makeFen(this.chess.toSetup()) });
    console.log(makeFen(this.chess.toSetup()));
    this.zf.white.reset();
    this.zf.black.reset();
    this.botMove();
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
    this.chess.play({ from: parseSquare(uci.slice(0, 2))!, to: parseSquare(uci.slice(2))! });
    this.cg.move(uci.slice(0, 2) as Key, uci.slice(2) as Key);
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
    if (this.chess.isEnd()) {
      console.log('game over');
      return;
    }
    if (this.players[this.chess.turn] === 'human') return;
    setTimeout(() => this.botMove());
  };

  userMove = (orig: Key, dest: Key) => {
    this.chess.play({ from: parseSquare(orig)!, to: parseSquare(dest)! });
    if (this.chess.isEnd()) {
      console.log('game over');
      return;
    }
    this.botMove();
  };

  botMove() {
    if (this.players[this.chess.turn] === 'human') return;
    if (this.players[this.chess.turn] === 'zero') {
      this.zf[this.chess.turn].goZero(makeFen(this.chess.toSetup())).then(m => {
        this.apiMove(m);
      });
    } else {
      this.zf[this.chess.turn].goFish(makeFen(this.chess.toSetup()), { depth: 10 }).then(pvs => {
        this.apiMove(pvs[0].moves[0]);
      });
    }
  }
  flip = () => {
    this.flipped = !this.flipped;
    this.cg.toggleOrientation();
    this.redraw();
  };

  private setGround = () => this.cg.set(this.getCgOpts());
}
