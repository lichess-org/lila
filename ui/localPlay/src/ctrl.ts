import { LocalPlayOpts } from './interfaces';
import { PromotionCtrl } from 'chess/promotion';
import { prop, Prop } from 'common';
//import { makeFen } from 'chessops/fen';
import { Api as CgApi } from 'chessground/api';
import { Config as CgConfig } from 'chessground/config';

export class Ctrl {
  promotion: PromotionCtrl;
  ground = prop<CgApi | false>(false) as Prop<CgApi | false>;
  flipped = false;
  redraw: () => void = () => {};

  constructor(readonly opts: LocalPlayOpts) {
    this.promotion = new PromotionCtrl(this.withGround, this.setGround, this.redraw);
  }

  getCgOpts = (): CgConfig => {
    return {
      movable: {
        color: 'both',
      },
    };
  };

  withGround = <A>(f: (cg: CgApi) => A): A | false => {
    const g = this.ground();
    return g && f(g);
  };

  userMove = (orig: Key, dest: Key) => {
    orig;
    dest;
  };

  flip = () => {
    this.flipped = !this.flipped;
    this.withGround(g => g.toggleOrientation());
    this.redraw();
  };

  private setGround = () => this.withGround(g => g.set(this.getCgOpts()));
}
