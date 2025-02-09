import { type ObjectStorage, objectStorage, range } from 'common/objectStorage';
import { LocalGame } from './localGame';
import { status } from 'game';
import { type StatusId, clockToSpeed } from 'game';
import { myUserId } from 'common';

export class LocalDb {
  store: ObjectStorage<LocalGame> | undefined;
  lite: ObjectStorage<LiteGame> | undefined;

  constructor() {}

  async init(): Promise<this> {
    [this.store, this.lite] = await Promise.all([
      objectStorage<LocalGame>({
        store: 'local.games',
      }),
      objectStorage<LiteGame>({
        store: 'local.games.lite',
        version: 3,
        indices: [
          { name: 'createdAt', keyPath: 'createdAt' },
          { name: 'white', keyPath: 'white' },
          { name: 'black', keyPath: 'black' },
          { name: 'speed', keyPath: 'speed' },
          { name: 'status', keyPath: 'status' },
          { name: 'winner', keyPath: 'winner' },
        ],
      }),
    ]).catch(() => []);
    return this;
  }

  get lastId(): string | undefined {
    return localStorage.getItem(`local.${myUserId() ?? 'anonymous'}.gameId`) || undefined;
  }

  set lastId(id: string | undefined) {
    if (id) localStorage.setItem(`local.${myUserId() ?? 'anonymous'}.gameId`, id);
    else localStorage.removeItem(`local.${myUserId() ?? 'anonymous'}.gameId`);
  }

  async getOne(id: string | undefined = this.lastId): Promise<LocalGame | undefined> {
    // optional chaining in query methods as idb can be undefined in legacy incognito
    return id ? this.store?.get(id) : Promise.resolve(undefined);
  }

  async byDate(after: number | undefined, until: number | undefined): Promise<LiteGame[]> {
    const games: LiteGame[] = [];
    await this.lite?.readCursor({ index: 'createdAt', query: range({ above: after, max: until }) }, info =>
      games.push(info),
    );
    return games;
  }

  async ongoing(): Promise<LiteGame[]> {
    const games: LiteGame[] = [];
    await this.lite?.readCursor({ index: 'status', query: status.started }, info => games.push(info));
    return games;
  }

  // delete(ids?: string[] | string): Promise<any> {
  //   if (!ids) return Promise.all([this.store?.clear(), this.lite?.clear()]);
  //   else
  //     return Promise.all(
  //       [ids].flat().map(id => {
  //         this.store?.remove(id);
  //         this.lite?.remove(id);
  //       }),
  //     );
  // }

  async put(game: LocalGame): Promise<void> {
    const lite = makeLite(game);
    game = structuredClone(game);
    await Promise.all([this.store?.put(game.id, game), this.lite?.put(lite.id, lite)]);
    this.lastId = game.id;
  }
}

export interface LiteGame {
  id: string;
  white?: string;
  black?: string;
  createdAt: Millis;
  initial: Seconds;
  increment: Seconds;
  speed: Speed;
  initialFen: FEN;
  fen: FEN;
  turn: Color;
  lastMove: Uci;
  status: StatusId;
  winner?: string;
}

function makeLite(game: LocalGame): LiteGame {
  return {
    ...game,
    speed: game.initial === Infinity ? 'correspondence' : clockToSpeed(game.initial, game.increment),

    fen: game.fen,
    turn: game.turn,
    lastMove: !game.moves.length ? '' : game.moves[game.moves.length - 1].uci,
    status: game.finished?.status?.id ?? status['started'],
    winner: game.finished?.winner,
  };
}
