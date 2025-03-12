import { Chess, opposite } from 'chessops';
import { Game } from '../interfaces';
import { looseH as h } from 'common/snabbdom';
import { Chessground } from 'chessground';
import { makeFen } from 'chessops/fen';
import { chessgroundDests } from 'chessops/compat';

export class PlayCtrl {
  chess: Chess;
  ground?: CgApi;
  pov: Color;
  constructor(
    readonly game: Game,
    readonly redraw: () => void,
  ) {
    this.pov = 'white';
  }

  view = () => h('main.bot-app.bot-game', [this.viewBoard()]);

  private setGround = (cg: CgApi) => (this.ground = cg);

  private isPlaying = () => true;

  private onUserMove = (_orig: Key, _dest: Key) => {
    this.ground?.set({ turnColor: opposite(this.pov) });
  };

  private chessgroundConfig = () => ({
    orientation: this.pov,
    fen: makeFen(this.chess.toSetup()),
    // lastMove: this.lastMove,
    turnColor: this.chess.turn,
    check: !!this.chess.isCheck(),
    movable: {
      free: false,
      color: this.isPlaying() ? this.pov : undefined,
      dests: chessgroundDests(this.chess),
    },
    events: {
      move: this.onUserMove,
    },
  });

  private viewBoard = () =>
    h('div.bot-game__board', [
      h(
        'div.cg-wrap',
        {
          hook: {
            insert: vnode => {
              this.setGround(Chessground(vnode.elm as HTMLElement, this.chessgroundConfig()));
            },
          },
        },
        'loading...',
      ),
    ]);
}
