import { LocalPlayOpts } from './interfaces';
import { PromotionCtrl } from 'chess/promotion';
import { prop, Prop } from 'common';
import { makeBoardFen } from 'chessops/fen';
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
  zf: Zerofish;

  constructor(readonly opts: LocalPlayOpts, readonly redraw: () => void) {
    this.promotion = new PromotionCtrl(this.withGround, this.setGround, this.redraw);
    makeZerofish().then(zf => (this.zf = zf));
  }

  getCgOpts = (): CgConfig => {
    const cgDests = new Map(
      [...this.chess.allDests()].map(
        ([s, ds]) => [makeSquare(s), [...ds].map(d => makeSquare(d))] as [Key, Key[]]
      )
    );
    console.log(this.chess);
    return {
      fen: makeBoardFen(this.chess.board),
      orientation: this.flipped ? 'black' : 'white',
      turnColor: this.chess.turn,

      movable: {
        color: 'white', //this.chess.turn,
        dests: cgDests,
      },
    };
  };

  withGround = <A>(f: (cg: CgApi) => A): A | false => {
    const g = this.ground();
    return g && f(g);
  };

  apiMove = (uci: Uci) => {
    this.chess.play({ from: parseSquare(uci.slice(0, 2))!, to: parseSquare(uci.slice(2))! });
    this.withGround(g => {
      g.move(uci.slice(0, 2) as Key, uci.slice(2) as Key);
      g.set({
        turnColor: 'white', //this.chess.turn,
        movable: {
          dests: new Map(
            [...this.chess.allDests()].map(
              ([s, ds]) => [makeSquare(s), [...ds].map(d => makeSquare(d))] as [Key, Key[]]
            )
          ),
        },
      });
    });
    this.redraw();
  };

  userMove = (orig: Key, dest: Key) => {
    this.chess.play({ from: parseSquare(orig)!, to: parseSquare(dest)! });
    console.log('userMove', this.chess);
    //this.redraw();
    this.zf.goFish(makeBoardFen(this.chess.board), { depth: 10 }).then(m => {
      console.log('zf', m);
      this.apiMove(m[0].moves[0]);
    });
  };

  flip = () => {
    this.flipped = !this.flipped;
    this.withGround(g => g.toggleOrientation());
    this.redraw();
  };

  private setGround = () => this.withGround(g => g.set(this.getCgOpts()));
}
