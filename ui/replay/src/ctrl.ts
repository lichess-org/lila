import { Api as CgApi } from 'chessground/api';
import { makeFen } from 'chessops/fen';
import { makeUci, opposite, Position } from 'chessops';
import { parsePgn, startingPosition } from 'chessops/pgn';
import { parseSan } from 'chessops/san';
import { Prop, prop } from 'common';
import { ReplayOpts, Node } from './interfaces';
import { uciToMove } from 'chessground/util';

export default class ReplayCtrl {
  orientation: Color;
  nodes: Node[] = [];
  index = 0;
  trans: Trans;
  ground = prop<CgApi | false>(false) as Prop<CgApi | false>;
  menu = prop<boolean>(false);

  constructor(opts: ReplayOpts, readonly redraw: () => void) {
    this.trans = lichess.trans(opts.i18n);
    this.orientation = opts.orientation || 'white';
    const game = parsePgn(opts.pgn)[0];
    const pos = startingPosition(game.headers).unwrap();
    const toNode = (pos: Position) => ({ fen: makeFen(pos.toSetup()), check: pos.isCheck() });
    this.nodes.push(toNode(pos));
    for (const n of game.moves.mainline()) {
      const move = parseSan(pos, n.san);
      if (!move) {
        console.error(n, game.headers, makeFen(pos.toSetup()));
        throw `Can't parse ${n}`;
      } else pos.play(move);
      this.nodes.push({ ...toNode(pos), san: n.san, uci: makeUci(move) });
    }
    this.index = this.nodes.length - 1;
  }

  node = () => this.nodes[this.index];

  backward = () => {
    this.index = Math.max(0, this.index - 1);
    this.setGround();
    this.redraw();
  };
  forward = () => {
    this.index = Math.min(this.nodes.length - 1, this.index + 1);
    this.setGround();
    this.redraw();
  };

  toggleMenu = () => {
    this.menu(!this.menu());
    this.redraw();
  };

  flip = () => {
    this.orientation = opposite(this.orientation);
    this.menu(false);
    this.redraw();
  };

  cgOpts = () => ({
    fen: this.node().fen,
    orientation: this.orientation,
    check: this.node().check,
    lastMove: uciToMove(this.node().uci),
  });

  analysisUrl = () => `/analysis/${this.node().fen.replace(' ', '_')}?color=${this.orientation}`;
  practiceUrl = () => `${this.analysisUrl()}#practice`;

  private setGround = () => this.withGround(g => g.set(this.cgOpts()));

  withGround = (f: (cg: CgApi) => void) => {
    const g = this.ground();
    return g && f(g);
  };
}
