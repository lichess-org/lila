import * as co from 'chessops';
import { showSetupDialog } from './setupDialog';
import { LocalGame } from './localGame';
import { type Player, clockToSpeed } from 'lib/game/game';
import type { RoundProxy as RoundProxyType, RoundData, RoundOpts } from 'round';
import { analyse } from './analyse';
import { env } from './devEnv';
import { myUserId } from 'lib';

export class RoundProxy implements RoundProxyType {
  readonly data: RoundData;
  readonly handlers: SocketHandlers = {
    move: (d: any) => env.game.move(d.u),
    resign: () => env.game.resign(),
    'blindfold-no': () => {},
    'blindfold-yes': () => {},
    'rematch-yes': () => env.game.load(undefined),
    'draw-yes': () => env.game.draw(),
  };

  constructor(prefs: any) {
    this.data = {
      game: {
        id: 'synthetic',
        variant: { key: 'standard', name: 'Standard', short: 'Std' },
        speed: 'classical',
        perf: 'unlimited',
        fen: co.fen.INITIAL_FEN,
        turns: 0,
        source: 'local',
        status: { id: 20, name: 'started' },
        player: 'white',
      },
      local: this,
      player: {} as Player,
      opponent: {} as Player,
      pref: { ...prefs, submitMove: 0 },
      steps: [],
      takebackable: false,
      moretimeable: false,
    };
  }

  newOpponent = (): void => showSetupDialog(env.game.live.setup);
  analyse = (): void => analyse(env.game);
  moreTime = (): void => {};
  outoftime = (): void => env.game.flag();
  berserk = (): void => {};
  reload: () => void = site.reload;
  sendLoading = (typ: string, data?: any): void => this.send(typ, data);

  send(t: string, d?: any): void {
    if (this.handlers[t]) this.handlers[t]?.(d);
    else console.log('send: no handler for', t, d);
  }

  receive = (typ: string, data: any): boolean => {
    if (this.handlers[typ]) this.handlers[typ]?.(data);
    else console.log('recv: no handler for', typ, data);
    return true;
  };

  updateBoard(game: LocalGame | undefined, opts?: CgConfig): void {
    const updates: CgConfig = {};
    if (game) {
      updates.fen = game.fen;
      updates.check = game.chess.isCheck();
      updates.turnColor = game.turn;
      if (env.game.rewind || !env.bot[env.game.live.turn])
        updates.movable = {
          color: game.turn,
          dests: game.cgDests,
        };
    }
    env.round.chessground?.set({ ...updates, ...opts });
  }

  reset(): void {
    const bottom = env.game.orientation;
    const top = co.opposite(bottom);
    this.data.game.fen = env.game.live.initialFen;
    this.data.game.turns = env.game.live.ply;
    this.data.game.status = env.game.live.status.status;
    this.data.game.speed = this.data.game.perf = clockToSpeed(env.game.initial, env.game.increment);
    this.data.game.player = (env.game.rewind ?? env.game.live).turn;
    this.data.steps = env.game.live.roundSteps;
    this.data.possibleMoves = env.game.live.dests;
    this.data.player = player(bottom);
    this.data.opponent = player(top);
    this.data.clock = env.game.clock;
    if (!env.round) return;
    //env.round.chessground?.set({ movable: { dests: env.game.live.cgDests } });
    env.round.ply = env.game.live.ply;
    env.round.reload(this.data);
  }

  get roundOpts(): RoundOpts {
    return {
      data: this.data,
      userId: myUserId(),
      noab: false,
      onChange: () => {
        if (env.round.ply === 0)
          this.updateBoard(undefined, { lastMove: undefined, orientation: env.game.orientation });
      },
    };
  }
}

function player(color: Color): RoundData['player'] {
  const bot = env.bot.info(env.game.idOf(color));
  return {
    color,
    user: {
      id: env.game.idOf(color),
      username: env.game.nameOf(color),
      online: true,
      perfs: {},
    },
    id: env.game.idOf(color),
    isGone: false,
    name: env.game.nameOf(color),
    rating: bot?.ratings[env.game.speed],
    image: bot ? env.bot.imageUrl(bot) : undefined,
    onGame: true,
    version: 0,
  };
}
