import { Api as CgApi } from 'chessground/api';
import { Chess } from 'chessops/chess';
import { chessgroundDests } from 'chessops/compat';
import { Config as CgConfig } from 'chessground/config';
import { parseFen, makeFen } from 'chessops/fen';
import { parseUci, opposite } from 'chessops/util';
import { prop, Prop } from 'common';
import { StormOpts, StormData, StormPuzzle, StormVm, Promotion } from './interfaces';
import makePromotion from './promotion';
import {Role} from 'chessground/types';

export default class StormCtrl {

  data: StormData;
  vm: StormVm;
  trans: Trans;
  promotion: Promotion;
  ground = prop<CgApi | undefined>(undefined) as Prop<CgApi>;

  constructor(readonly opts: StormOpts, readonly redraw: () => void) {
    this.data = opts.data;
    this.trans = lichess.trans(opts.i18n);
    this.vm = {
      mode: 'init',
      puzzleIndex: 0,
      moveIndex: 0,
      clockMillis: 60 * 3
    };
    this.promotion = makePromotion(this.ground, this.makeCgOpts, redraw);
  }

  userMove = (orig: Key, dest: Key): void => {
    if (!this.promotion.start(orig, dest, this.playUserMove)) this.playUserMove(orig, dest);
  }

  playUserMove = (orig: Key, dest: Key, promotion?: Role): void => {
    this.vm.moveIndex++;
    const expected = this.line()[this.vm.moveIndex];
    const uci = `${orig}${dest}${promotion ? (promotion == 'knight' ? 'n' : promotion[0]) : ''}`;
    if (uci == expected) {
      lichess.sound.play('move');
      if (this.vm.moveIndex == this.line().length - 1) {
        this.vm.puzzleIndex++;
        this.vm.moveIndex = 0;
      } else {
        this.vm.moveIndex++;
      }
    } else {
      lichess.sound.play('error');
      this.vm.puzzleIndex++;
      this.vm.moveIndex = 0;
    }
    this.withGround(this.showGround);
  };

  private withGround = <A>(f: (cg: CgApi) => A): A | undefined => {
    const g = this.ground();
    return g && f(g);
  }

  private showGround = (g: CgApi): void => g.set(this.makeCgOpts());

  puzzle = (): StormPuzzle => this.data.puzzles[this.vm.puzzleIndex];

  line = (): Uci[] => this.puzzle().line.split(' ');

  position = (): Chess => {
    const pos = Chess.fromSetup(parseFen(this.puzzle().fen).unwrap()).unwrap();
    this.line().slice(0, this.vm.moveIndex + 1).forEach(uci =>
      pos.play(parseUci(uci)!)
    );
    return pos;
  }

  makeCgOpts = (): CgConfig => {
    const puzzle = this.puzzle();
    const pos = this.position();
    const pov = opposite(parseFen(puzzle.fen).unwrap().turn);
    const canMove = this.vm.mode == 'init' || this.vm.mode == 'play';
    return {
      fen: makeFen(pos.toSetup()),
      orientation: pov,
      turnColor: pos.turn,
      movable: canMove ? {
        color: pov,
        dests: chessgroundDests(pos)
      } : undefined,
      premovable: {
        enabled: false
      },
      check: !!pos.isCheck(),
      lastMove: this.uciToLastMove(this.line()[this.vm.moveIndex])
    };
  }

  private uciToLastMove = (uci: string): [Key, Key] => [uci.substr(0, 2) as Key, uci.substr(2, 2) as Key];
}
