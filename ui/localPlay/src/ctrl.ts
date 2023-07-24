import { LocalPlayOpts } from './interfaces';
import { PromotionCtrl } from 'chess/promotion';
import { prop, Prop } from 'common';
import { makeFen } from 'chessops/fen';
import { Chess, makeSquare, parseSquare } from 'chessops';
import makeZerofish, { Zerofish } from 'zerofish';
import { Api as CgApi } from 'chessground/api';
import { Config as CgConfig } from 'chessground/config';
import { Key } from 'chessground/types';

export class Ctrl {
  promotion: PromotionCtrl;
  ground = prop<CgApi | false>(false) as Prop<CgApi | false>;
  flipped = false;
  chess = Chess.default();
  zf: { white: Zerofish; black: Zerofish };
  whiteEl: Cash;
  blackEl: Cash;

  constructor(readonly opts: LocalPlayOpts, readonly redraw: () => void) {
    this.promotion = new PromotionCtrl(this.withGround, this.setGround, this.redraw);
    Promise.all([makeZerofish(), makeZerofish()]).then(([wz, bz]) => {
      this.zf ??= { white: wz, black: bz };
    });
  }

  dropHandler(color: 'white' | 'black', el: HTMLElement) {
    const $el = $(el);
    $el.on('dragenter dragover dragleave drop', e => {
      console.log('gibbins');
      e.preventDefault();
      e.stopPropagation();
    });
    $el.on('dragenter dragover', e => $(e.eventTarget).addClass('hilite'));
    $el.on('dragleave drop', e => $(e.eventTarget).removeClass('hilite'));
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
  }
  go() {
    //const numTimes = parseInt($('#num-games').val() as string) || 1;
    this.chess.reset();
    this.zf.white.goZero(makeFen(this.chess.toSetup())).then(m => {
      this.apiMove(m);
    });
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

  withGround = <A>(f: (cg: CgApi) => A): A | false => {
    const g = this.ground();
    return g && f(g);
  };

  apiMove = (uci: Uci) => {
    console.log('apiMove', uci);
    console.log(`apiMove ${uci} by ${this.chess.turn}`);
    this.chess.play({ from: parseSquare(uci.slice(0, 2))!, to: parseSquare(uci.slice(2))! });
    console.log('apiMove after', this.chess.turn);
    this.withGround(g => {
      g.move(uci.slice(0, 2) as Key, uci.slice(2) as Key);
      g.set({
        turnColor: this.chess.turn,
        /*movable: {
          dests: new Map(
            [...this.chess.allDests()].map(
              ([s, ds]) => [makeSquare(s), [...ds].map(d => makeSquare(d))] as [Key, Key[]]
            )
          ),
        },*/
      });
    });
    this.redraw();
    setTimeout(() => {
      console.log('calling makeZero');
      this.zf[this.chess.turn]
        .goZero(makeFen(this.chess.toSetup()))
        .then(m => {
          console.log('makeZero returned', m);
          this.apiMove(m);
        })
        .catch(e => console.log('makeZero error', e));
    });
  };

  userMove = (orig: Key, dest: Key) => {
    this.chess.play({ from: parseSquare(orig)!, to: parseSquare(dest)! });

    /*this.zf.goFish(makeBoardFen(this.chess.board), { depth: 10 }).then(m => {
      this.apiMove(m[0].moves[0]);
    });*/
  };

  flip = () => {
    this.flipped = !this.flipped;
    this.withGround(g => g.toggleOrientation());
    this.redraw();
  };

  private setGround = () => this.withGround(g => g.set(this.getCgOpts()));
}
