import * as co from 'chessops';
import { showSetupDialog } from './setupDialog';
import { LocalGame } from './localGame';
import { clockToSpeed } from 'game';
import type { RoundProxy as IRoundProxy, RoundData, RoundOpts } from 'round';
import { analyse } from './analyse';
import { env } from './localEnv';

export class RoundProxy implements IRoundProxy {
  data: RoundData;
  handlers: SocketHandlers = {
    move: (d: any) => env.game.move(d.u),
    resign: () => env.game.resign(),
    'blindfold-no': () => {},
    'blindfold-yes': () => {},
    'rematch-yes': () => env.game.reset(),
    'draw-yes': () => env.game.draw(),
  };
  constructor() {
    this.data = Object.defineProperties(
      {
        game: {
          id: 'synthetic',
          variant: { key: 'standard', name: 'Standard', short: 'Std' },
          speed: 'classical',
          perf: 'unlimited',
          rated: false,
          fen: env.game.fen,
          turns: env.game.ply,
          source: 'local',
          status: { id: 20, name: 'started' },
          player: env.game.ply % 2 ? 'black' : 'white',
        },
        player: this.player('white', ''),
        opponent: this.player('black', ''),
        pref: env.game.opts.pref,
        steps: [{ ply: 0, san: '', uci: '', fen: env.game.fen }],
        takebackable: false,
        moretimeable: false,
        possibleMoves: env.game.live.dests,
      },
      {
        clock: { get: () => env.game.clock },
      },
    );
    this.reset();
  }
  send(t: string, d?: any): void {
    if (this.handlers[t]) this.handlers[t]?.(d);
    else console.log('send: no handler for', t, d);
  }
  newOpponent = (): void => showSetupDialog(env.bot, env.game.setup);
  analyse = (): void => analyse(env.game);
  moreTime = (): void => {};
  outoftime = (): void => env.game.flag();
  berserk = (): void => {};
  sendLoading = (typ: string, data?: any): void => this.send(typ, data);
  receive = (typ: string, data: any): boolean => {
    if (this.handlers[typ]) this.handlers[typ]?.(data);
    else console.log('recv: no handler for', typ, data);
    return true;
  };
  reload = (): void => site.reload();

  updateCg(game: LocalGame | undefined, cgOpts?: CgConfig): void {
    const gameUpdates: CgConfig = {};
    if (game) {
      gameUpdates.fen = game.fen;
      gameUpdates.check = game.chess.isCheck();
      gameUpdates.turnColor = game.turn;
      if (env.game.history || env.game.isUserTurn)
        gameUpdates.movable = {
          color: game.turn,
          dests: game.cgDests,
        };
    }
    env.round.chessground?.set({ ...gameUpdates, ...cgOpts });
  }

  reset(): void {
    const bottom = env.game.originalOrientation;
    const top = co.opposite(bottom);
    const twoPlayers = !env.game.setup.white && !env.game.setup.black;
    const playerName = {
      white: twoPlayers ? 'White player' : 'Player',
      black: twoPlayers ? 'Black player' : 'Player',
    };
    this.data.game.fen = env.game.fen;
    this.data.game.turns = 0;
    this.data.game.status = { id: 20, name: 'started' };
    this.data.steps = [{ ply: 0, san: '', uci: '', fen: env.game.fen }];
    this.data.possibleMoves = env.game.live.dests;
    this.data.player = this.player(bottom, env.bot[bottom]?.name ?? playerName[bottom]);
    this.data.opponent = this.player(top, env.bot[top]?.name ?? playerName[top]);
    if (env.round) env.round.ply = 0;
    this.data.game.speed = this.data.game.perf = clockToSpeed(
      env.game.setup.initial ?? Infinity,
      env.game.setup.increment ?? 0,
    );
  }

  get roundOpts(): RoundOpts {
    return {
      local: this,
      data: this.data,
      userId: '',
      noab: false,
      i18n: env.game.opts.i18n,
      //socketSend: this.send,
      onChange: () => {
        if (env.round.ply === 0)
          this.updateCg(undefined, {
            lastMove: undefined,
            orientation: env.game.originalOrientation,
            events: { move: undefined },
          });
      },
    };
  }

  private player(color: Color, name: string): RoundData['player'] {
    return {
      color,
      user: {
        id: '',
        username: name,
        online: true,
        perfs: {},
      },
      id: '',
      isGone: false,
      name,
      onGame: true,
      version: 0,
    };
  }
}
