import { Chess, opposite } from 'chessops';
import { Game, Pref } from '../interfaces';

export default class PlayCtrl {
  chess: Chess;
  moves: San[];
  onPly: number;
  ground?: CgApi;
  pov: Color;
  constructor(
    readonly pref: Pref,
    readonly game: Game,
    readonly redraw: () => void,
  ) {
    this.chess = Chess.default();
    this.moves = [];
    this.onPly = this.moves.length;
    this.pov = 'white';
  }

  setGround = (cg: CgApi) => (this.ground = cg);

  isPlaying = () => true;

  onMove = (_orig: Key, _dest: Key) => {};

  onUserMove = (_orig: Key, _dest: Key) => {
    this.ground?.set({ turnColor: opposite(this.pov) });
  };
}
