import * as co from 'chessops';
import { looseH as h, VNode } from 'common/snabbdom';
import { showSetupDialog } from './setupDialog';
import { LocalGame } from './localGame';
import { clockToSpeed, type Player } from 'game';
import type { RoundProxy as IRoundProxy, RoundData, RoundOpts, Position } from 'round';
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
    this.data = {
      game: {
        id: 'synthetic',
        variant: { key: 'standard', name: 'Standard', short: 'Std' },
        speed: 'classical',
        perf: 'unlimited',
        rated: false,
        fen: env.game.initialFen,
        turns: env.game.live.ply,
        source: 'local',
        status: { id: 20, name: 'started' },
        player: env.game.live.turn,
      },
      player: player('white'),
      opponent: player('black'),
      pref: { ...env.game.opts.pref, submitMove: 0 },
      steps: [{ ply: 0, san: '', uci: '', fen: env.game.initialFen }],
      takebackable: false,
      moretimeable: false,
      possibleMoves: env.game.live.dests,
      clock: env.game.clock,
    };
    this.reset();
  }

  newOpponent = (): void => showSetupDialog(env.bot, env.game);
  userVNode = (player: Player, position: Position): VNode | undefined => botVNode(player, position);
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
    const bottom = env.game.orientation;
    const top = co.opposite(bottom);
    this.data.game.fen = env.game.initialFen;
    this.data.game.turns = 0;
    this.data.game.status = { id: 20, name: 'started' };
    this.data.steps = [{ ply: 0, san: '', uci: '', fen: env.game.initialFen }];
    this.data.possibleMoves = env.game.live.dests;
    this.data.player = player(bottom);
    this.data.opponent = player(top);
    this.data.game.speed = this.data.game.perf = clockToSpeed(env.game.initial, env.game.increment);
    this.data.clock = env.game.clock;
    if (!env.round) return;
    env.round.ply = 0;
    env.round.reload(this.data);
  }

  get roundOpts(): RoundOpts {
    return {
      local: this,
      data: this.data,
      userId: env.user,
      noab: false,
      onChange: () => {
        if (env.round.ply === 0)
          this.updateCg(undefined, { lastMove: undefined, orientation: env.game.orientation });
      },
    };
  }
}

function player(color: Color): RoundData['player'] {
  const bot = env.bot.get(env.game.idOf(color));
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

function botVNode(player: Player, position: Position): VNode | undefined {
  return player.id.startsWith('#') || player.name === 'White' || player.name === 'Black'
    ? h(`div.ruser-${position}.ruser.user-link.fancy-bot.online`, [
        h('span', [h('i.line.patron', {}), h('name', player.name)]),
        env.dev
          ? h('rating', player.rating ? `${player.rating}` : '')
          : !!player.image && h('img', { attrs: { src: player.image } }),
      ])
    : undefined;
}
